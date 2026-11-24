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
import ru.vtb.core.data.static_data.KafkaTopic;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

public class KafkaClient {

    private static final AtomicBoolean isReadingThreadRunning = new AtomicBoolean(false);
    private final Properties clientConfig;

    public KafkaClient() {
        Properties properties = new Properties();
        properties.put(CommonClientConfigs.BOOTSTRAP_SERVERS_CONFIG, EnvConstants.BOOTSTRAP_SERVERS);
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

    public static void startReadingTopics(List<String> topics) {
        if (isReadingThreadRunning.compareAndSet(false, true)) {
            Log.trace("About to starting main consumer");
            ExecutorService executorService = Executors.newSingleThreadExecutor();
            executorService.submit(() -> {
                try (var consumer = new KafkaClient()
                        .addConfig(Map.of(
                                ConsumerConfig.GROUP_ID_CONFIG,
                                "Test Automation Consumer " + UUID.randomUUID()))
                        .createConsumer()) {
                    consumer.subscribe(topics);
                    while (true) {
                        ConsumerRecords<String, String> records = consumer.poll(Duration.ofMillis(200));
                        for (ConsumerRecord<String, String> rec : records) {
                            Log.trace("Catching message with offset '%s' from topic '%s'".formatted(rec.offset(), rec.topic()));
                            KafkaTopic.getTopicStorageByName(rec.topic()).put(rec.offset(), rec.value());
                        }
                    }
                } catch (Exception e) {
                    Log.error("While reading kafka topics unpredictable error was occurred. Reason is: " + e.getMessage());
                    System.exit(-1);
                }
            });
        } else {
            throw new RuntimeException("Поток с чтением топиков кафки может быть запущен только ОДИН раз");
        }
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
