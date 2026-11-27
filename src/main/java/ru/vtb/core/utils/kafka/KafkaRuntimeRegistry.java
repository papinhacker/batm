package ru.vtb.core.utils.kafka;

import org.apache.kafka.clients.producer.KafkaProducer;
import ru.vtb.core.common.EnvConstants;
import ru.vtb.core.common.Log;
import ru.vtb.core.data.static_data.KafkaCluster;
import ru.vtb.core.data.static_data.KafkaTopic;

import java.time.Clock;
import java.time.Duration;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * Runtime Kafka-кластеров на прогон: запуск в ISuiteListener.onStart, закрытие в onFinish (docs/kafka-design.md, раздел 2).
 * <p>
 * Ошибка старта любого настроенного кластера роняет весь прогон (исключение из {@link #start()}).
 * Если в одной JVM несколько сьютов — runtime общий, закрывается при выходе последнего.
 * <p>
 * Необязательные ключи (значения по умолчанию в скобках): kafka.startup.timeout (PT60S),
 * kafka.store.max.messages.per.topic (50000), kafka.store.ttl (PT60M), kafka.partitions.refresh (PT30S).
 */
public final class KafkaRuntimeRegistry {

    private static final Map<KafkaCluster, KafkaClusterRuntime> RUNTIMES = new EnumMap<>(KafkaCluster.class);
    /** Producer-ы для кластеров без читаемых топиков (отправка без чтения). */
    private static final Map<KafkaCluster, KafkaProducer<String, String>> PRODUCERS = new EnumMap<>(KafkaCluster.class);
    private static int activeSuites;

    private KafkaRuntimeRegistry() {
    }

    /**
     * Запускает чтение всех настроенных кластеров, у которых есть топики, и ждёт фиксации позиций.
     *
     * @throws IllegalStateException если какой-то кластер не стартовал (весь прогон падает)
     */
    public static synchronized void start() {
        activeSuites++;
        if (activeSuites > 1) {
            return;
        }
        KafkaClusterRuntime.Settings settings = settings();
        Duration startupTimeout = duration("kafka.startup.timeout", Duration.ofSeconds(60));
        List<KafkaClusterRuntime> started = new ArrayList<>();
        try {
            for (KafkaCluster cluster : KafkaCluster.values()) {
                List<String> topics = KafkaTopic.getTopicsNames(cluster);
                if (topics.isEmpty()) {
                    continue;
                }
                KafkaClusterConfig config = KafkaClusterConfig.of(cluster);
                if (!config.isEnabled()) {
                    Log.warning("Kafka-кластер '%s' не настроен (пустое свойство '%s'), топики %s читаться не будут"
                            .formatted(cluster, cluster.getBootstrapServersPropertyKey(), topics));
                    continue;
                }
                KafkaClusterRuntime runtime = new KafkaClusterRuntime(cluster.getConfigName(), config.toClientProperties(),
                        topics, settings, Clock.systemUTC());
                runtime.start();
                started.add(runtime);
                RUNTIMES.put(cluster, runtime);
            }
            for (KafkaClusterRuntime runtime : started) {
                runtime.awaitReady(startupTimeout);
            }
        } catch (RuntimeException e) {
            closeAll();
            activeSuites = 0;
            throw e;
        }
    }

    /**
     * Закрывает consumer-ы и producer-ы, если завершился последний сьют.
     */
    public static synchronized void stop() {
        if (activeSuites == 0) {
            return;
        }
        activeSuites--;
        if (activeSuites == 0) {
            closeAll();
        }
    }

    /** @return runtime кластера или null, если чтение кластера не запущено */
    public static synchronized KafkaClusterRuntime runtime(KafkaCluster cluster) {
        return RUNTIMES.get(cluster);
    }

    /**
     * @return producer кластера: из его runtime или отдельный, если чтение кластера не запущено
     */
    public static synchronized KafkaProducer<String, String> producer(KafkaCluster cluster) {
        KafkaClusterRuntime runtime = RUNTIMES.get(cluster);
        if (runtime != null) {
            return runtime.producer();
        }
        return PRODUCERS.computeIfAbsent(cluster,
                c -> KafkaClusterRuntime.createProducer(KafkaClusterConfig.of(c).toClientProperties()));
    }

    private static void closeAll() {
        for (KafkaClusterRuntime runtime : RUNTIMES.values()) {
            runtime.close();
        }
        RUNTIMES.clear();
        for (KafkaProducer<String, String> producer : PRODUCERS.values()) {
            producer.close(Duration.ofSeconds(10));
        }
        PRODUCERS.clear();
    }

    private static KafkaClusterRuntime.Settings settings() {
        KafkaClusterRuntime.Settings defaults = KafkaClusterRuntime.Settings.defaults();
        String max = EnvConstants.getProperty("kafka.store.max.messages.per.topic");
        int maxMessages;
        try {
            maxMessages = max == null || max.isBlank() ? defaults.maxMessagesPerTopic() : Integer.parseInt(max.trim());
        } catch (NumberFormatException e) {
            throw new IllegalStateException("Некорректное значение 'kafka.store.max.messages.per.topic': '%s'".formatted(max), e);
        }
        return new KafkaClusterRuntime.Settings(
                maxMessages,
                duration("kafka.store.ttl", defaults.storeTtl()),
                duration("kafka.partitions.refresh", defaults.partitionsRefresh()),
                defaults.pollTimeout());
    }

    private static Duration duration(String key, Duration defaultValue) {
        String value = EnvConstants.getProperty(key);
        try {
            return value == null || value.isBlank() ? defaultValue : Duration.parse(value.trim());
        } catch (java.time.format.DateTimeParseException e) {
            throw new IllegalStateException("Некорректное значение '%s': '%s' (ожидается ISO-8601, например PT60S)"
                    .formatted(key, value), e);
        }
    }
}
