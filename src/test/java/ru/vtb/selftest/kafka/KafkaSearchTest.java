package ru.vtb.selftest.kafka;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.kafka.common.record.TimestampType;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;
import ru.vtb.core.utils.kafka.Described;
import ru.vtb.core.utils.kafka.KafkaMessage;
import ru.vtb.core.utils.kafka.KafkaSearch;
import ru.vtb.core.utils.kafka.TopicStore;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static org.testng.Assert.*;

/**
 * Новый API поиска (docs/kafka-design.md, раздел 6) на хранилище без брокера: await, awaitAll, awaitRaw,
 * assertNoMessage, таймауты и текст ошибки.
 */
public class KafkaSearchTest {

    private static final String TOPIC = "selftest-search";
    private static final Duration SHORT = Duration.ofMillis(300);

    /** Модель сообщения для десериализации. */
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Msg {
        public String id;
        public String status;

        @Override
        public String toString() {
            return id + "/" + status;
        }
    }

    private record Attachment(String name, String mimeType, String content) {
    }

    private TopicStore store;
    private List<Attachment> attachments;
    private AtomicReference<Throwable> clusterError;
    private KafkaSearch search;
    private KafkaSearch.Target target;
    private ScheduledExecutorService scheduler;
    private long offset;

    @BeforeMethod
    public void setUp() {
        store = new TopicStore(TOPIC, 1000, Duration.ofMinutes(60), Clock.systemUTC());
        attachments = new ArrayList<>();
        clusterError = new AtomicReference<>();
        search = new KafkaSearch(Duration.ofMillis(20), 3,
                (name, mimeType, content) -> attachments.add(new Attachment(name, mimeType, content)),
                new ObjectMapper(), Clock.systemUTC());
        target = new KafkaSearch.Target(TOPIC, "selftest-cluster", store, clusterError::get);
        scheduler = Executors.newSingleThreadScheduledExecutor();
        offset = 0;
    }

    @AfterMethod(alwaysRun = true)
    public void tearDown() {
        scheduler.shutdownNow();
    }

    private void put(long timestamp, String value) {
        store.add(new KafkaMessage(TOPIC, 0, offset++, "k" + offset, Map.of(), timestamp, TimestampType.CREATE_TIME, value, Instant.now()));
    }

    private static String json(String id, String status) {
        return "{\"id\":\"%s\",\"status\":\"%s\"}".formatted(id, status);
    }

    private Attachment attachment(String name) {
        return attachments.stream().filter(a -> a.name().equals(name)).findFirst()
                .orElseThrow(() -> new AssertionError("нет вложения '" + name + "', есть: " + attachments.stream().map(Attachment::name).toList()));
    }

    /* ------------------------------------------- await ------------------------------------------- */

    @Test(groups = {"selftest"}, description = "await: последнее по timestamp подходящее сообщение, вложение в Allure")
    public void awaitReturnsLastByTimestamp() {
        put(3_000, json("a", "Accepted"));
        put(1_000, json("a", "New"));
        put(2_000, json("a", "Accepted-old"));
        put(4_000, json("b", "Accepted"));

        Msg found = search.await(target, Msg.class, Described.of("id=a", m -> "a".equals(m.id)), SHORT);
        assertEquals(found.status, "Accepted", "timestamp 3000 — последнее для id=a");
        Attachment attachment = attachment("Kafka ← " + TOPIC);
        assertTrue(attachment.content().contains("\"status\" : \"Accepted\""), attachment.content());
        assertTrue(attachment.content().contains("offset=0"), attachment.content());
    }

    @Test(groups = {"selftest"}, description = "await находит сообщение, пришедшее во время ожидания")
    public void awaitWaitsForArrival() {
        scheduler.schedule(() -> put(1_000, json("late", "New")), 150, TimeUnit.MILLISECONDS);
        Msg found = search.await(target, Msg.class, m -> "late".equals(m.id), Duration.ofSeconds(5));
        assertEquals(found.status, "New");
    }

    @Test(groups = {"selftest"}, description = "await видит сообщения, уже отмеченные старыми методами KafkaHelper")
    public void awaitIgnoresLegacyMarks() {
        put(1_000, json("m", "New"));
        assertTrue(store.takeUnmarked(m -> true, TopicStore.Order.LAST).isPresent());
        assertEquals(search.await(target, Msg.class, m -> "m".equals(m.id), SHORT).status, "New");
    }

    @Test(groups = {"selftest"}, description = "Не десериализуемые сообщения пропускаются при поиске")
    public void notDeserializableIsSkipped() {
        put(1_000, json("x", "Ok"));
        put(2_000, "not a json");
        put(3_000, "{\"id\": [1, 2]}");   // id — массив, в String не десериализуется
        Msg found = search.await(target, Msg.class, m -> true, SHORT);
        assertEquals(found.id, "x");
    }

    @Test(groups = {"selftest"}, description = "Таймаут await: текст ошибки — топик, кластер, условие, сколько пришло, "
            + "не десериализованные, последние N; диагностика во вложении")
    public void awaitTimeoutDescribesTopic() {
        put(1_000, json("other", "New"));
        put(2_000, "garbage-1");
        put(3_000, json("other2", "New"));
        put(4_000, json("other3", "New"));
        scheduler.schedule(() -> put(5_000, "garbage-2"), 50, TimeUnit.MILLISECONDS);

        AssertionError e = expectThrows(AssertionError.class,
                () -> search.await(target, Msg.class, Described.of("id=wanted, status=New", m -> "wanted".equals(m.id)), SHORT));
        String text = e.getMessage();
        assertTrue(text.contains("не найдено сообщение Msg"), text);
        assertTrue(text.contains("топик '" + TOPIC + "', кластер 'selftest-cluster'"), text);
        assertTrue(text.contains("Условие: id=wanted, status=New"), text);
        assertTrue(text.contains("Сообщений пришло в топик за ожидание: 1 (в хранилище всего: 5, вытеснено лимитами: 0)"), text);
        assertTrue(text.contains("Не десериализовались в Msg и пропущены: 2"), text);
        assertTrue(text.contains("garbage-1") && text.contains("garbage-2"), "не десериализованные — в тексте ошибки: " + text);
        assertTrue(text.contains("Последние 3 сообщений топика:"), text);
        assertFalse(text.contains("offset=0 "), "последние N=3 — самого старого нет: " + text);
        assertTrue(text.contains("offset=4 "), text);
        assertEquals(attachment("Kafka: диагностика").content(), text);
    }

    @Test(groups = {"selftest"}, description = "Исключение в условии роняет поиск сразу, не дожидаясь таймаута")
    public void predicateExceptionFailsImmediately() {
        put(1_000, "{\"id\":\"a\"}");   // status отсутствует → null → NPE в условии
        long start = System.nanoTime();
        AssertionError e = expectThrows(AssertionError.class,
                () -> search.await(target, Msg.class, Described.of("status starts with N", m -> m.status.startsWith("N")),
                        Duration.ofSeconds(10)));
        assertTrue(Duration.ofNanos(System.nanoTime() - start).compareTo(Duration.ofSeconds(5)) < 0, "должно упасть сразу");
        assertTrue(e.getMessage().contains("условие поиска «status starts with N» упало"), e.getMessage());
        assertTrue(e.getCause() instanceof NullPointerException, "cause: " + e.getCause());
    }

    @Test(groups = {"selftest"}, description = "Ошибка чтения кластера роняет ожидание с этой ошибкой")
    public void clusterErrorFailsWait() {
        RuntimeException broken = new RuntimeException("consumer упал");
        scheduler.schedule(() -> clusterError.set(broken), 50, TimeUnit.MILLISECONDS);
        AssertionError e = expectThrows(AssertionError.class,
                () -> search.await(target, Msg.class, m -> true, Duration.ofSeconds(10)));
        assertSame(e.getCause(), broken);
        assertTrue(e.getMessage().contains("кластера 'selftest-cluster' остановлено"), e.getMessage());
    }

    @Test(groups = {"selftest"}, description = "Таймаут должен быть положительным")
    public void awaitRequiresPositiveTimeout() {
        expectThrows(IllegalArgumentException.class, () -> search.await(target, Msg.class, m -> true, Duration.ZERO));
        expectThrows(IllegalArgumentException.class, () -> search.await(target, Msg.class, m -> true, null));
    }

    /* ------------------------------------------ awaitRaw ----------------------------------------- */

    @Test(groups = {"selftest"}, description = "awaitRaw: текст последнего подходящего сообщения, не-JSON тоже ищется")
    public void awaitRawReturnsText() {
        put(1_000, "plain text id=7");
        put(2_000, json("7", "New"));
        assertEquals(search.awaitRaw(target, v -> v.contains("7"), SHORT), json("7", "New"));
        assertEquals(search.awaitRaw(target, v -> v.startsWith("plain"), SHORT), "plain text id=7");
    }

    @Test(groups = {"selftest"}, description = "Таймаут awaitRaw: в тексте нет раздела о десериализации")
    public void awaitRawTimeout() {
        put(1_000, "x");
        AssertionError e = expectThrows(AssertionError.class,
                () -> search.awaitRaw(target, Described.of("contains y", v -> v.contains("y")), SHORT));
        assertTrue(e.getMessage().contains("не найдено сообщение String"), e.getMessage());
        assertTrue(e.getMessage().contains("Условие: contains y"), e.getMessage());
        assertFalse(e.getMessage().contains("Не десериализовались"), e.getMessage());
    }

    /* ------------------------------------------ awaitAll ----------------------------------------- */

    @Test(groups = {"selftest"}, description = "awaitAll: ровно expectedCount, в порядке timestamp, дожидается недостающих")
    public void awaitAllExactCount() {
        put(2_000, json("pack", "2"));
        put(1_000, json("pack", "1"));
        scheduler.schedule(() -> put(3_000, json("pack", "3")), 100, TimeUnit.MILLISECONDS);
        List<Msg> packs = search.awaitAll(target, Msg.class, m -> "pack".equals(m.id), 3, Duration.ofSeconds(5));
        assertEquals(packs.stream().map(m -> m.status).toList(), List.of("1", "2", "3"));
        assertEquals(attachments.stream().filter(a -> a.name().startsWith("Kafka ←")).count(), 3);
    }

    @Test(groups = {"selftest"}, description = "awaitAll: больше ожидаемого — падение со списком найденных")
    public void awaitAllTooManyFails() {
        for (int i = 1; i <= 4; i++) {
            put(i * 1_000L, json("pack", String.valueOf(i)));
        }
        AssertionError e = expectThrows(AssertionError.class,
                () -> search.awaitAll(target, Msg.class, Described.of("id=pack", m -> "pack".equals(m.id)), 3, SHORT));
        assertTrue(e.getMessage().contains("ожидалось ровно 3 сообщений Msg"), e.getMessage());
        assertTrue(e.getMessage().contains("найдено 4"), e.getMessage());
        assertTrue(e.getMessage().contains("\"status\":\"4\""), e.getMessage());
    }

    @Test(groups = {"selftest"}, description = "awaitAll: меньше ожидаемого за таймаут — ошибка «найдено N из M» с диагностикой")
    public void awaitAllTooFewTimesOut() {
        put(1_000, json("pack", "1"));
        AssertionError e = expectThrows(AssertionError.class,
                () -> search.awaitAll(target, Msg.class, m -> "pack".equals(m.id), 3, SHORT));
        assertTrue(e.getMessage().contains("найдено 1 из 3 сообщений Msg"), e.getMessage());
        assertTrue(e.getMessage().contains("Последние 1 сообщений топика:"), e.getMessage());
    }

    @Test(groups = {"selftest"}, description = "awaitAll: expectedCount должен быть больше 0")
    public void awaitAllRequiresPositiveCount() {
        expectThrows(IllegalArgumentException.class, () -> search.awaitAll(target, Msg.class, m -> true, 0, SHORT));
    }

    /* --------------------------------------- assertNoMessage ------------------------------------- */

    @Test(groups = {"selftest"}, description = "assertNoMessage: нет подходящего — проходит, ожидая всё окно")
    public void assertNoMessagePassesAfterWindow() {
        put(1_000, json("other", "New"));
        long start = System.nanoTime();
        search.assertNoMessage(target, v -> v.contains("forbidden"), SHORT);
        assertTrue(Duration.ofNanos(System.nanoTime() - start).compareTo(SHORT.minusMillis(20)) >= 0, "должно ждать всё окно");
    }

    @Test(groups = {"selftest"}, description = "assertNoMessage: падает, если сообщение уже было с начала прогона")
    public void assertNoMessageFailsOnExisting() {
        put(1_000, "forbidden message");
        AssertionError e = expectThrows(AssertionError.class,
                () -> search.assertNoMessage(target, Described.of("contains forbidden", v -> v.contains("forbidden")), SHORT));
        assertTrue(e.getMessage().contains("есть сообщение, которого не должно быть"), e.getMessage());
        assertTrue(e.getMessage().contains("Условие: contains forbidden"), e.getMessage());
        assertTrue(e.getMessage().contains("forbidden message"), e.getMessage());
        attachment("Kafka ← " + TOPIC);
    }

    @Test(groups = {"selftest"}, description = "assertNoMessage: падает, если сообщение пришло во время окна")
    public void assertNoMessageFailsOnArrival() {
        scheduler.schedule(() -> put(1_000, "forbidden later"), 100, TimeUnit.MILLISECONDS);
        AssertionError e = expectThrows(AssertionError.class,
                () -> search.assertNoMessage(target, v -> v.contains("forbidden"), Duration.ofSeconds(5)));
        assertTrue(e.getMessage().contains("forbidden later"), e.getMessage());
    }

    @Test(groups = {"selftest"}, description = "assertNoMessage требует явное положительное окно")
    public void assertNoMessageRequiresWindow() {
        for (Duration window : new Duration[]{null, Duration.ZERO, Duration.ofSeconds(-1)}) {
            IllegalArgumentException e = expectThrows(IllegalArgumentException.class,
                    () -> search.assertNoMessage(target, v -> true, window));
            assertTrue(e.getMessage().contains("явное положительное окно"), e.getMessage());
        }
    }
}
