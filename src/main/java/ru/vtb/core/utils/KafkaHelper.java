package ru.vtb.core.utils;

import org.apache.commons.lang3.RandomStringUtils;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.clients.producer.RecordMetadata;
import org.awaitility.Awaitility;
import org.awaitility.core.ConditionTimeoutException;
import org.testng.Assert;
import ru.vtb.core.common.Log;
import ru.vtb.core.data.static_data.KafkaTopic;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.function.Predicate;

public class KafkaHelper {

    protected static final KafkaClient BATM_KAFKA = new KafkaClient().addConfig(Map.of(ConsumerConfig.GROUP_ID_CONFIG,
            "Test Automation Consumer - " + RandomStringUtils.insecure().nextAlphabetic(10)));

    private KafkaHelper() {
    }

    private enum SearchStrategy {
        NONE,
        FIRST,
        LAST;
    }

    public static List<Object> getAllRecords(KafkaTopic topic, Predicate<String> condition, long timeout) {
        List<Object> result = new ArrayList<>();
        long start = System.nanoTime();
        long duration = timeout * 1_000_000_000;
        long endTime = start + duration;
        while (System.nanoTime() < endTime) {
            var msg = getRecord(topic, condition, timeout, SearchStrategy.NONE);
            result.add(msg);
        }
        return result;
    }

    public static List<Object> getAllRecords(KafkaTopic topic, List<String> keywords, long timeout) {
        return getAllRecords(topic, compilePredicate(keywords), timeout);
    }

    public static List<Object> getAllRecords(KafkaTopic topic, String keyword, long timeout) {
        return getAllRecords(topic, List.of(keyword), timeout);
    }

    public static Object getFirstRecord(KafkaTopic topic, List<String> keywords, long timeout, boolean needFail) {
        return getRecord(topic, keywords, timeout, needFail, SearchStrategy.FIRST);
    }

    public static Object getFirstRecord(KafkaTopic topic, List<String> keywords, long timeout) {
        return getFirstRecord(topic, keywords, timeout, false);
    }

    public static Object getFirstRecord(KafkaTopic topic, String keyword, long timeout) {
        return getFirstRecord(topic, List.of(keyword), timeout);
    }

    public static Object getLastRecord(KafkaTopic topic, List<String> keywords, long timeout, boolean needFail) {
        return getRecord(topic, keywords, timeout, needFail, SearchStrategy.LAST);
    }

    public static Object getLastRecord(KafkaTopic topic, List<String> keywords, long timeout) {
        return getLastRecord(topic, keywords, timeout, false);
    }

    public static Object getLastRecord(KafkaTopic topic, String keyword, long timeout) {
        return getLastRecord(topic, List.of(keyword), timeout);
    }

    /**
     * Возвращает последнее (по offset) сообщение топика в виде строки, удовлетворяющее условию.
     * Если сообщение не найдено за timeout, тест падает.
     *
     * @param topic                топик
     * @param condition            условие поиска по тексту сообщения (не должно бросать исключений)
     * @param conditionDescription описание условия для логов и текста ошибки
     * @param timeout              таймаут ожидания в секундах
     */
    public static String getLastRawRecord(KafkaTopic topic, Predicate<String> condition, String conditionDescription, long timeout) {
        Log.step("Receiving LAST message from topic '%s', which matches: %s".formatted(topic.getTopicName(), conditionDescription));
        String result = getRecord(topic, condition, timeout, SearchStrategy.LAST);
        if (result == null) {
            Assert.fail("Unable to find any message from topic '%s', which matches: %s".formatted(topic.getTopicName(), conditionDescription));
        }
        return result;
    }

    public static Object getRecord(KafkaTopic topic, List<String> keywords, long timeout, boolean needFail, SearchStrategy searchStrategy) {
        Log.step("Receiving %s message from topic '%s', which contains '%s'".formatted(searchStrategy, topic.getTopicName(), String.join("; ", keywords)));
        String result = getRecord(topic, compilePredicate(keywords), timeout, searchStrategy);
        if (result != null) {
            if (topic.getDefClass().equals(String.class)) {
                return result;
            } else return CommonHelper.deserializeJsonToObject(result, topic.getDefClass());
        } else if (needFail) {
            return null;
        } else {
            Assert.fail(String.format("Unable to find any message from topic '%s', which contains '%s'", topic.getTopicName(), String.join("; ", keywords)));
            return null;
        }
    }

    private static String getRecord(KafkaTopic topic, Predicate<String> condition, long timeout, SearchStrategy searchStrategy) {
        if (!KafkaClient.isReadingStarted(topic.getCluster())) {
            Assert.fail("Топик '%s' не читается: не запущено чтение Kafka-кластера '%s' (проверьте свойство '%s')".formatted(
                    topic.getTopicName(), topic.getCluster(), topic.getCluster().getBootstrapServersPropertyKey()));
        }
        Map<Long, String> current = topic.getTopicMap();
        waitForMessage(current, condition, timeout);
        var stream = current.entrySet().stream().filter(e -> condition.test(e.getValue()));
        Optional<Map.Entry<Long, String>> entry;
        switch (searchStrategy) {
            case LAST -> entry = stream.max(Map.Entry.comparingByKey());
            case FIRST -> entry = stream.min(Map.Entry.comparingByKey());
            case NONE -> entry = stream.findAny();
            default -> throw new IllegalStateException("Unexpected value: " + searchStrategy);
        }
        if (entry.isPresent()) {
            var buff = entry.get();
            current.remove(buff.getKey(), buff.getValue());
            return buff.getValue();
        }
        return null;
    }

    private static void waitForMessage(Map<Long, String> map, Predicate<String> condition, long timeout) {
        try {
            Awaitility.await()
                    .pollInSameThread()
                    .timeout(Duration.ofSeconds(timeout))
                    .pollInterval(1, TimeUnit.SECONDS)
                    .until(() -> map.values().stream().anyMatch(condition));
        } catch (ConditionTimeoutException ignored) {

        }
    }

    public static Future<RecordMetadata> sendMessage(KafkaTopic topic, Object object) {
        return sendMessage(topic, object, true);
    }

    public static Future<RecordMetadata> sendMessage(KafkaTopic topic, Object object, boolean serializeNulls) {
        String message;
        if (object.getClass().equals(String.class)) {
            message = object.toString();
        } else if (serializeNulls) {
            message = CommonHelper.serializeToJsonString(object);
        } else {
            message = CommonHelper.serializeToJsonStringIgnoringNullableFields(object);
        }
        Log.step("Отправляем сообщение '%s' в топик '%s'".formatted(message, topic.getTopicName()));
        Future<RecordMetadata> future = null;
        try (var producer = BATM_KAFKA.createProducer()) {
            future = producer.send(new ProducerRecord<>(topic.getTopicName(), message));
            future.get();
        } catch (ExecutionException | InterruptedException e) {
            Assert.fail(String.format("Не удалось отправить сообщение в топик '%s'", topic.getTopicName()), e);
        }
        return future;
    }

    private static Predicate<String> compilePredicate(List<String> keywords) {
        Predicate<String> condition = !keywords.isEmpty() ? value -> value.contains(keywords.get(0)) : s -> true;
        if (keywords.size() > 1) {
            for (int i = 1; i < keywords.size(); i++) {
                int finalI = i;
                condition = condition.and(value -> value.contains(keywords.get(finalI)));
            }
        }
        return condition;
    }
}
