package ru.vtb.core.utils.kafka;

import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.PartitionInfo;
import org.apache.kafka.common.TopicPartition;
import org.apache.kafka.common.errors.WakeupException;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;
import ru.vtb.core.common.Log;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/**
 * Чтение и отправка для одного Kafka-кластера (docs/kafka-design.md, разделы 2 и 4).
 * <ul>
 *     <li>один consumer-поток (daemon) без consumer group: assign всех партиций топиков, seekToEnd —
 *     сообщения, записанные до старта, не читаются;</li>
 *     <li>{@link #awaitReady} ждёт, пока позиции всех партиций зафиксированы; иначе — понятная ошибка;</li>
 *     <li>новые партиции, появившиеся во время прогона, раз в partitionsRefresh добавляются и читаются с начала;</li>
 *     <li>один KafkaProducer на кластер, создаётся при первой отправке;</li>
 *     <li>{@link #close} останавливает consumer и закрывает producer.</li>
 * </ul>
 */
public final class KafkaClusterRuntime implements AutoCloseable {

    /** Параметры хранилища и потока чтения. */
    public record Settings(int maxMessagesPerTopic, Duration storeTtl, Duration partitionsRefresh, Duration pollTimeout) {
        public static Settings defaults() {
            return new Settings(50_000, Duration.ofMinutes(60), Duration.ofSeconds(30), Duration.ofMillis(200));
        }
    }

    private final String cluster;
    private final Properties clientProperties;
    private final List<String> topics;
    private final Settings settings;
    private final Clock clock;
    private final Map<String, TopicStore> stores = new LinkedHashMap<>();
    private final CountDownLatch ready = new CountDownLatch(1);
    private final Thread consumerThread;

    private volatile KafkaConsumer<String, String> consumer;
    private volatile KafkaProducer<String, String> producer;
    private volatile Throwable error;
    private volatile boolean closed;
    /** Что поток делает сейчас — для текста ошибки при таймауте готовности. */
    private volatile String stage = "запуск потока";

    /**
     * @param cluster          имя кластера (для логов и ошибок)
     * @param clientProperties свойства клиента: bootstrap.servers и, при необходимости, SSL
     * @param topics           топики, которые читает consumer
     */
    public KafkaClusterRuntime(String cluster, Properties clientProperties, List<String> topics, Settings settings, Clock clock) {
        this.cluster = cluster;
        this.clientProperties = clientProperties;
        this.topics = List.copyOf(topics);
        this.settings = settings;
        this.clock = clock;
        for (String topic : this.topics) {
            stores.put(topic, new TopicStore(topic, settings.maxMessagesPerTopic(), settings.storeTtl(), clock));
        }
        this.consumerThread = new Thread(this::consume, "kafka-consumer-" + cluster);
        this.consumerThread.setDaemon(true);
    }

    public String getCluster() {
        return cluster;
    }

    public void start() {
        if (!topics.isEmpty()) {
            consumerThread.start();
        } else {
            ready.countDown();
        }
    }

    /**
     * Ждёт, пока зафиксированы позиции всех партиций.
     *
     * @throws IllegalStateException по таймауту или если consumer упал при старте — с указанием кластера и этапа
     */
    public void awaitReady(Duration timeout) {
        boolean done;
        try {
            done = ready.await(timeout.toMillis(), TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Ожидание готовности Kafka-кластера '%s' прервано".formatted(cluster), e);
        }
        if (error != null) {
            throw new IllegalStateException("Kafka-кластер '%s': чтение не запущено — %s".formatted(cluster, error.getMessage()), error);
        }
        if (!done) {
            throw new IllegalStateException("Kafka-кластер '%s': за %d с не зафиксированы позиции партиций (этап: %s)"
                    .formatted(cluster, timeout.toSeconds(), stage));
        }
    }

    /** @return хранилище топика или null, если топик этим runtime не читается */
    public TopicStore store(String topic) {
        return stores.get(topic);
    }

    /** @return ошибка, из-за которой остановилось чтение, или null */
    public Throwable getError() {
        return error;
    }

    public synchronized KafkaProducer<String, String> producer() {
        if (closed) {
            throw new IllegalStateException("Kafka-кластер '%s' уже закрыт".formatted(cluster));
        }
        if (producer == null) {
            producer = createProducer(clientProperties);
        }
        return producer;
    }

    static KafkaProducer<String, String> createProducer(Properties clientProperties) {
        Properties properties = new Properties();
        properties.putAll(clientProperties);
        properties.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        properties.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        properties.put(ProducerConfig.MAX_REQUEST_SIZE_CONFIG, 40_000_000);
        return new KafkaProducer<>(properties);
    }

    @Override
    public void close() {
        synchronized (this) {
            if (closed) {
                return;
            }
            closed = true;
        }
        KafkaConsumer<String, String> current = consumer;
        if (current != null) {
            current.wakeup();
        }
        if (consumerThread.isAlive()) {
            try {
                consumerThread.join(Duration.ofSeconds(10).toMillis());
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
        KafkaProducer<String, String> currentProducer = producer;
        if (currentProducer != null) {
            currentProducer.close(Duration.ofSeconds(10));
        }
    }

    private void consume() {
        Properties properties = new Properties();
        properties.putAll(clientProperties);
        properties.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        properties.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        properties.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, false);
        // тесты не должны создавать топики на стенде: несуществующий топик — ошибка старта
        properties.put(ConsumerConfig.ALLOW_AUTO_CREATE_TOPICS_CONFIG, false);
        // метаданные обновляются не реже, чем проверяются новые партиции
        properties.put(ConsumerConfig.METADATA_MAX_AGE_CONFIG, (int) settings.partitionsRefresh().toMillis());
        properties.remove(ConsumerConfig.GROUP_ID_CONFIG);
        try (KafkaConsumer<String, String> kafkaConsumer = new KafkaConsumer<>(properties)) {
            consumer = kafkaConsumer;
            if (closed) {
                return;
            }
            Set<TopicPartition> assigned = new LinkedHashSet<>(partitions(kafkaConsumer, true));
            kafkaConsumer.assign(assigned);
            kafkaConsumer.seekToEnd(assigned);
            for (TopicPartition partition : assigned) {
                stage = "фиксация позиции " + partition;
                kafkaConsumer.position(partition);
            }
            stage = "позиции зафиксированы: " + assigned.size() + " партиций";
            Log.info("Kafka-кластер '%s': читаем %d партиций топиков %s с конца".formatted(cluster, assigned.size(), topics));
            ready.countDown();

            Instant nextRefresh = clock.instant().plus(settings.partitionsRefresh());
            while (!closed) {
                ConsumerRecords<String, String> records = kafkaConsumer.poll(settings.pollTimeout());
                Instant now = clock.instant();
                for (ConsumerRecord<String, String> record : records) {
                    TopicStore store = stores.get(record.topic());
                    if (store != null) {
                        store.add(KafkaMessage.of(record, now));
                    }
                }
                if (!now.isBefore(nextRefresh)) {
                    addNewPartitions(kafkaConsumer, assigned);
                    stores.values().forEach(TopicStore::evict);
                    nextRefresh = now.plus(settings.partitionsRefresh());
                }
            }
        } catch (WakeupException e) {
            if (!closed) {
                fail(e);
            }
        } catch (Throwable e) {
            fail(e);
        } finally {
            ready.countDown();
        }
    }

    /**
     * @param failOnMissing true — при старте: несуществующий топик или топик без партиций — ошибка
     */
    private List<TopicPartition> partitions(KafkaConsumer<String, String> kafkaConsumer, boolean failOnMissing) {
        List<TopicPartition> result = new ArrayList<>();
        List<String> missing = new ArrayList<>();
        for (String topic : topics) {
            stage = "получение партиций топика " + topic;
            List<PartitionInfo> infos;
            try {
                infos = kafkaConsumer.partitionsFor(topic);
            } catch (WakeupException e) {
                throw e;
            } catch (RuntimeException e) {
                throw new IllegalStateException("не удалось получить партиции топика '%s': %s".formatted(topic, e.getMessage()), e);
            }
            if (infos == null || infos.isEmpty()) {
                missing.add(topic);
                continue;
            }
            infos.forEach(info -> result.add(new TopicPartition(info.topic(), info.partition())));
        }
        if (failOnMissing && !missing.isEmpty()) {
            throw new IllegalStateException("топики не существуют или не имеют партиций: " + missing);
        }
        return result;
    }

    private void addNewPartitions(KafkaConsumer<String, String> kafkaConsumer, Set<TopicPartition> assigned) {
        List<TopicPartition> added = partitions(kafkaConsumer, false).stream()
                .filter(partition -> !assigned.contains(partition))
                .toList();
        if (!added.isEmpty()) {
            assigned.addAll(added);
            kafkaConsumer.assign(assigned);
            kafkaConsumer.seekToBeginning(added);
            Log.info("Kafka-кластер '%s': новые партиции %s читаются с начала".formatted(cluster, added));
        }
    }

    private void fail(Throwable e) {
        error = e;
        StringWriter stackTrace = new StringWriter();
        e.printStackTrace(new PrintWriter(stackTrace));
        Log.error("Чтение топиков Kafka-кластера '%s' остановлено из-за ошибки: %s%n%s".formatted(cluster, e, stackTrace));
    }
}
