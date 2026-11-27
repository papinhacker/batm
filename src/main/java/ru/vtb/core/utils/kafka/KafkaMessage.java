package ru.vtb.core.utils.kafka;

import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.common.header.Header;
import org.apache.kafka.common.record.TimestampType;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Сообщение, прочитанное из Kafka. Неизменяемое.
 *
 * @param headers    заголовки (значения как UTF-8 строки; при повторе ключа — последнее значение)
 * @param receivedAt момент получения тестовым consumer-ом (для времени жизни в хранилище)
 */
public record KafkaMessage(String topic, int partition, long offset, String key, Map<String, String> headers,
                           long timestamp, TimestampType timestampType, String value, Instant receivedAt) {

    public KafkaMessage {
        headers = headers == null ? Map.of() : Collections.unmodifiableMap(new LinkedHashMap<>(headers));
    }

    public MessageId id() {
        return new MessageId(topic, partition, offset);
    }

    public static KafkaMessage of(ConsumerRecord<String, String> record, Instant receivedAt) {
        Map<String, String> headers = new LinkedHashMap<>();
        for (Header header : record.headers()) {
            headers.put(header.key(), header.value() == null ? null : new String(header.value(), StandardCharsets.UTF_8));
        }
        return new KafkaMessage(record.topic(), record.partition(), record.offset(), record.key(), headers,
                record.timestamp(), record.timestampType(), record.value(), receivedAt);
    }

    /** Идентификатор сообщения: топик + партиция + offset. */
    public record MessageId(String topic, int partition, long offset) {
        @Override
        public String toString() {
            return "%s-%d@%d".formatted(topic, partition, offset);
        }
    }
}
