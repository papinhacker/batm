package ru.vtb.core.utils;

import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.clients.producer.RecordMetadata;
import org.awaitility.Awaitility;
import org.awaitility.core.ConditionTimeoutException;
import org.testng.Assert;
import ru.vtb.core.common.Log;
import ru.vtb.core.data.static_data.KafkaTopic;
import ru.vtb.core.utils.kafka.KafkaClusterRuntime;
import ru.vtb.core.utils.kafka.KafkaMessage;
import ru.vtb.core.utils.kafka.KafkaRuntimeRegistry;
import ru.vtb.core.utils.kafka.KafkaSearch;
import ru.vtb.core.utils.kafka.TopicStore;
import ru.vtb.core.common.EnvConstants;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.function.Predicate;

public class KafkaHelper {

    private static volatile KafkaSearch search;

    private KafkaHelper() {
    }

    /* ******************************************************************************************
     *  Новый API поиска (docs/kafka-design.md, раздел 6). Ищет среди всех сообщений с начала прогона,
     *  возвращает последнее по timestamp; при таймауте — ошибка с диагностикой и вложением Allure.
     * ******************************************************************************************/

    /**
     * @return последнее (по timestamp) сообщение типа T, удовлетворяющее условию; сообщения, которые не
     * десериализуются в T, пропускаются (и показываются в тексте ошибки при таймауте)
     */
    public static <T> T await(KafkaTopic topic, Class<T> type, Predicate<T> condition, Duration timeout) {
        return search().await(target(topic), type, condition, timeout);
    }

    /**
     * @return ровно expectedCount сообщений типа T по условию (в порядке timestamp); меньше за таймаут или больше — падение
     */
    public static <T> List<T> awaitAll(KafkaTopic topic, Class<T> type, Predicate<T> condition, int expectedCount, Duration timeout) {
        return search().awaitAll(target(topic), type, condition, expectedCount, timeout);
    }

    /**
     * @return текст последнего (по timestamp) сообщения, удовлетворяющего условию
     */
    public static String awaitRaw(KafkaTopic topic, Predicate<String> condition, Duration timeout) {
        return search().awaitRaw(target(topic), condition, timeout);
    }

    /**
     * Падает, если за окно ожидания (или раньше, с начала прогона) в топике есть подходящее сообщение.
     *
     * @param window явное окно ожидания, обязательно
     */
    public static void assertNoMessage(KafkaTopic topic, Predicate<String> condition, Duration window) {
        search().assertNoMessage(target(topic), condition, window);
    }

    private static KafkaSearch.Target target(KafkaTopic topic) {
        TopicStore store = storeOf(topic);
        return new KafkaSearch.Target(topic.getTopicName(), topic.getCluster().getConfigName(), store, () -> readingError(topic));
    }

    private static KafkaSearch search() {
        KafkaSearch current = search;
        if (current == null) {
            synchronized (KafkaHelper.class) {
                if (search == null) {
                    String lastN = EnvConstants.getProperty("kafka.report.last.n");
                    int n;
                    try {
                        n = lastN == null || lastN.isBlank() ? 10 : Integer.parseInt(lastN.trim());
                    } catch (NumberFormatException e) {
                        throw new IllegalStateException("Некорректное значение 'kafka.report.last.n': '%s'".formatted(lastN), e);
                    }
                    search = new KafkaSearch(Duration.ofMillis(200), n, KafkaSearch.Reporter.ALLURE, new ObjectMapper(),
                            java.time.Clock.systemUTC());
                }
                current = search;
            }
        }
        return current;
    }

    /* ******************************************************************************************
     *  Старые методы (удалить в 2.5)
     * ******************************************************************************************/

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

    /**
     * Старое поведение поверх нового хранилища (временно, до перехода на await*): сообщение, уже возвращённое
     * одним из старых методов, им повторно не возвращается — оно отмечается, а не удаляется
     * (новые методы поиска видят все сообщения). FIRST/LAST — по timestamp сообщения.
     */
    private static String getRecord(KafkaTopic topic, Predicate<String> condition, long timeout, SearchStrategy searchStrategy) {
        TopicStore store = storeOf(topic);
        Predicate<KafkaMessage> byValue = message -> message.value() != null && condition.test(message.value());
        waitForMessage(topic, store, byValue, timeout);
        failIfReadingBroken(topic);
        TopicStore.Order order = switch (searchStrategy) {
            case LAST -> TopicStore.Order.LAST;
            case FIRST -> TopicStore.Order.FIRST;
            case NONE -> TopicStore.Order.ANY;
        };
        return store.takeUnmarked(byValue, order).map(KafkaMessage::value).orElse(null);
    }

    /**
     * @return хранилище топика; тест падает, если чтение кластера не запущено или остановилось с ошибкой
     */
    private static TopicStore storeOf(KafkaTopic topic) {
        failIfReadingBroken(topic);
        KafkaClusterRuntime runtime = KafkaRuntimeRegistry.runtime(topic.getCluster());
        TopicStore store = runtime == null ? null : runtime.store(topic.getTopicName());
        if (store == null) {
            Assert.fail("Топик '%s' не читается: не запущено чтение Kafka-кластера '%s' (проверьте свойство '%s')".formatted(
                    topic.getTopicName(), topic.getCluster(), topic.getCluster().getBootstrapServersPropertyKey()));
        }
        return store;
    }

    /**
     * Роняет тест, если чтение топиков кластера, к которому относится топик, остановилось с ошибкой.
     */
    private static void failIfReadingBroken(KafkaTopic topic) {
        Throwable error = readingError(topic);
        if (error != null) {
            Assert.fail("Топик '%s' не читается: чтение Kafka-кластера '%s' остановлено из-за ошибки: %s"
                    .formatted(topic.getTopicName(), topic.getCluster(), error), error);
        }
    }

    private static Throwable readingError(KafkaTopic topic) {
        KafkaClusterRuntime runtime = KafkaRuntimeRegistry.runtime(topic.getCluster());
        return runtime == null ? null : runtime.getError();
    }

    private static void waitForMessage(KafkaTopic topic, TopicStore store, Predicate<KafkaMessage> condition, long timeout) {
        try {
            // ожидание прерывается и при падении чтения кластера — тест не ждёт весь таймаут впустую
            Awaitility.await()
                    .pollInSameThread()
                    .timeout(Duration.ofSeconds(timeout))
                    .pollInterval(1, TimeUnit.SECONDS)
                    .until(() -> readingError(topic) != null || store.anyMatch(condition, true));
        } catch (ConditionTimeoutException ignored) {
            // не найдено за таймаут: вызывающий метод решает, падать (Assert.fail) или вернуть null (needFail)
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
        KafkaSearch.Reporter.ALLURE.attach("Kafka → " + topic.getTopicName(), "text/plain", message);
        Future<RecordMetadata> future = null;
        try {
            // один producer на кластер топика (раньше — новый producer кластера BATM на каждое сообщение)
            future = KafkaRuntimeRegistry.producer(topic.getCluster()).send(new ProducerRecord<>(topic.getTopicName(), message));
            future.get();
        } catch (ExecutionException e) {
            Assert.fail(String.format("Не удалось отправить сообщение в топик '%s'", topic.getTopicName()), e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
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
