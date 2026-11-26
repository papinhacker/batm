package ru.vtb.core.utils;

import org.apache.kafka.clients.CommonClientConfigs;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.config.SslConfigs;
import org.apache.kafka.common.security.auth.SecurityProtocol;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;
import ru.vtb.core.common.EnvConstants;
import ru.vtb.core.common.Log;
import ru.vtb.core.data.static_data.KafkaCluster;
import ru.vtb.core.data.static_data.KafkaTopic;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class KafkaClient {

    private static final Set<KafkaCluster> readingClusters = ConcurrentHashMap.newKeySet();
    private static final Map<KafkaCluster, Throwable> readingErrors = new ConcurrentHashMap<>();
    private final Properties clientConfig;

    public KafkaClient() {
        this(KafkaCluster.BATM);
    }

    /**
     * @param cluster кластер, к которому подключается клиент. SSL-настройки общие для всех кластеров
     */
    public KafkaClient(KafkaCluster cluster) {
        Properties properties = new Properties();
        properties.put(CommonClientConfigs.BOOTSTRAP_SERVERS_CONFIG, cluster.getBootstrapServers());
        properties.put(CommonClientConfigs.SECURITY_PROTOCOL_CONFIG, SecurityProtocol.SSL.name);
        properties.put(SslConfigs.SSL_TRUSTSTORE_LOCATION_CONFIG, EnvConstants.SSL_TRUSTSTORE_LOCATION);
        properties.put(SslConfigs.SSL_TRUSTSTORE_PASSWORD_CONFIG, EnvConstants.SSL_TRUSTSTORE_PASSWORD);
        properties.put(SslConfigs.SSL_TRUSTSTORE_TYPE_CONFIG, EnvConstants.SSL_TRUSTSTORE_TYPE);
        properties.put(SslConfigs.SSL_KEYSTORE_LOCATION_CONFIG, EnvConstants.SSL_KEYSTORE_LOCATION);
        properties.put(SslConfigs.SSL_KEYSTORE_PASSWORD_CONFIG, EnvConstants.SSL_KEYSTORE_PASSWORD);
        properties.put(SslConfigs.SSL_KEYSTORE_TYPE_CONFIG, EnvConstants.SSL_KEYSTORE_TYPE);
        properties.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        properties.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        properties.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        properties.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        properties.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        properties.put("max.request.size", 40000000);
        this.clientConfig = properties;
    }

    public static void startReadingTopics(KafkaCluster cluster, List<String> topics) {
        if (readingClusters.add(cluster)) {
            Log.trace("About to starting consumer for '%s' kafka cluster".formatted(cluster));
            ExecutorService executorService = Executors.newSingleThreadExecutor();
            executorService.submit(() -> {
                try (var consumer = new KafkaClient(cluster)
                        .addConfig(Map.of(
                                ConsumerConfig.GROUP_ID_CONFIG,
                                "Test Automation Consumer " + UUID.randomUUID()))
                        .createConsumer()) {
                    consumer.subscribe(topics);
                    while (true) {
                        ConsumerRecords<String, String> records = consumer.poll(Duration.ofMillis(200));
                        for (ConsumerRecord<String, String> rec : records) {
                            Log.trace("Catching message with offset '%s' from topic '%s'".formatted(rec.offset(), rec.topic()));
                            KafkaTopic.getTopicStorageByName(cluster, rec.topic()).put(rec.offset(), rec.value());
                        }
                    }
                } catch (Throwable e) {
                    // Прогон не обрывается: ошибка сохраняется по кластеру, и KafkaHelper роняет с ней тесты,
                    // которые читают топики этого кластера. Тесты других кластеров продолжают работать.
                    readingErrors.put(cluster, e);
                    StringWriter stackTrace = new StringWriter();
                    e.printStackTrace(new PrintWriter(stackTrace));
                    Log.error("Чтение топиков Kafka-кластера '%s' остановлено из-за ошибки: %s%n%s"
                            .formatted(cluster, e, stackTrace));
                }
            });
        } else {
            throw new RuntimeException("Поток с чтением топиков кафки может быть запущен только ОДИН раз для кластера " + cluster);
        }
    }

    /**
     * @return true, если для кластера запущен поток чтения топиков
     */
    public static boolean isReadingStarted(KafkaCluster cluster) {
        return readingClusters.contains(cluster);
    }

    /**
     * @return исключение, из-за которого остановилось чтение топиков кластера, или null, если чтение не падало
     */
    public static Throwable getReadingError(KafkaCluster cluster) {
        return readingErrors.get(cluster);
    }

    public KafkaClient addConfig(Map<Object, Object> configMap) {
        this.clientConfig.putAll(configMap);
        return this;
    }

    /**
     * @return {@link KafkaConsumer} с текущими конфигурационными параметрами клиента
     */
    public KafkaConsumer<String, String> createConsumer() {
        return new KafkaConsumer<>(this.clientConfig);
    }

    /**
     * @return {@link KafkaProducer} с текущими конфигурационными параметрами клиента
     */
    public <K, V> KafkaProducer<K, V> createProducer() {
        return new KafkaProducer<>(this.clientConfig);
    }
}
