package ru.vtb.selftest.kafka;

import org.apache.kafka.common.record.TimestampType;
import org.testng.annotations.Test;
import ru.vtb.core.utils.kafka.KafkaMessage;
import ru.vtb.core.utils.kafka.TopicStore;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.*;
import java.util.concurrent.*;

import static org.testng.Assert.*;

/**
 * Хранилище сообщений топика (docs/kafka-design.md, раздел 3) — без брокера.
 */
public class TopicStoreTest {

    private static final String TOPIC = "selftest-topic";

    /** Часы, которые двигает тест. */
    private static final class MutableClock extends Clock {
        private Instant now = Instant.parse("2026-11-27T10:00:00Z");

        @Override
        public ZoneId getZone() {
            return ZoneOffset.UTC;
        }

        @Override
        public Clock withZone(ZoneId zone) {
            return this;
        }

        @Override
        public Instant instant() {
            return now;
        }

        void advance(Duration duration) {
            now = now.plus(duration);
        }
    }

    private static KafkaMessage message(int partition, long offset, long timestamp, String value, Instant receivedAt) {
        return new KafkaMessage(TOPIC, partition, offset, "key-" + value, Map.of("h", "v"), timestamp,
                TimestampType.CREATE_TIME, value, receivedAt);
    }

    private static TopicStore store(Clock clock) {
        return new TopicStore(TOPIC, 100, Duration.ofMinutes(60), clock);
    }

    @Test(groups = {"selftest"}, description = "Две партиции с одинаковым offset — оба сообщения доступны")
    public void sameOffsetInDifferentPartitionsKeepsBoth() {
        Clock clock = Clock.systemUTC();
        TopicStore store = store(clock);
        assertTrue(store.add(message(0, 5, 1_000, "p0", clock.instant())));
        assertTrue(store.add(message(1, 5, 2_000, "p1", clock.instant())));

        assertEquals(store.size(), 2);
        assertEquals(store.messages().stream().map(KafkaMessage::value).toList(), List.of("p0", "p1"));
        assertTrue(store.find(m -> m.value().equals("p0"), TopicStore.Order.ANY).isPresent());
        assertTrue(store.find(m -> m.value().equals("p1"), TopicStore.Order.ANY).isPresent());
    }

    @Test(groups = {"selftest"}, description = "Повторное получение того же topic+partition+offset не создаёт дубля")
    public void duplicateIsIgnored() {
        Clock clock = Clock.systemUTC();
        TopicStore store = store(clock);
        assertTrue(store.add(message(0, 1, 1_000, "a", clock.instant())));
        assertFalse(store.add(message(0, 1, 1_000, "a", clock.instant())));
        assertEquals(store.size(), 1);
    }

    @Test(groups = {"selftest"}, description = "Сохраняются key, headers, timestamp")
    public void keyHeadersTimestampAreKept() {
        Clock clock = Clock.systemUTC();
        TopicStore store = store(clock);
        store.add(message(2, 7, 12_345, "x", clock.instant()));
        KafkaMessage stored = store.messages().get(0);
        assertEquals(stored.key(), "key-x");
        assertEquals(stored.headers(), Map.of("h", "v"));
        assertEquals(stored.timestamp(), 12_345L);
        assertEquals(stored.id(), new KafkaMessage.MessageId(TOPIC, 2, 7));
    }

    @Test(groups = {"selftest"}, description = "FIRST/LAST — по timestamp, а не по offset; при равном timestamp — partition, offset")
    public void orderIsByTimestamp() {
        Clock clock = Clock.systemUTC();
        TopicStore store = store(clock);
        // offset больше, но timestamp раньше
        store.add(message(0, 100, 1_000, "early-big-offset", clock.instant()));
        store.add(message(0, 1, 3_000, "late-small-offset", clock.instant()));
        store.add(message(1, 50, 2_000, "middle", clock.instant()));
        store.add(message(0, 7, 2_000, "middle-p0", clock.instant()));

        assertEquals(store.find(m -> true, TopicStore.Order.FIRST).orElseThrow().value(), "early-big-offset");
        assertEquals(store.find(m -> true, TopicStore.Order.LAST).orElseThrow().value(), "late-small-offset");
        assertEquals(store.messages().stream().map(KafkaMessage::value).toList(),
                List.of("early-big-offset", "middle-p0", "middle", "late-small-offset"));
    }

    @Test(groups = {"selftest"}, description = "Отметки старых методов: найденное не возвращается повторно, но остаётся в хранилище")
    public void takeUnmarkedReturnsEachMessageOnce() {
        Clock clock = Clock.systemUTC();
        TopicStore store = store(clock);
        store.add(message(0, 0, 1_000, "pack-1", clock.instant()));
        store.add(message(1, 0, 2_000, "pack-2", clock.instant()));
        store.add(message(0, 1, 3_000, "pack-3", clock.instant()));

        List<String> taken = new ArrayList<>();
        for (int i = 0; i < 3; i++) {
            taken.add(store.takeUnmarked(m -> m.value().startsWith("pack"), TopicStore.Order.LAST).orElseThrow().value());
        }
        assertEquals(taken, List.of("pack-3", "pack-2", "pack-1"));
        assertTrue(store.takeUnmarked(m -> m.value().startsWith("pack"), TopicStore.Order.LAST).isEmpty());
        // сообщения не удалены: обычный поиск их видит
        assertEquals(store.size(), 3);
        assertTrue(store.find(m -> m.value().equals("pack-2"), TopicStore.Order.ANY).isPresent());
        assertFalse(store.anyMatch(m -> m.value().startsWith("pack"), true));
        assertTrue(store.anyMatch(m -> m.value().startsWith("pack"), false));
    }

    @Test(groups = {"selftest"}, description = "Параллельные вызовы старых методов получают разные сообщения")
    public void parallelTakeUnmarkedNeverReturnsSameMessageTwice() throws Exception {
        Clock clock = Clock.systemUTC();
        TopicStore store = store(clock);
        for (int i = 0; i < 50; i++) {
            store.add(message(i % 3, i, 1_000 + i, "m" + i, clock.instant()));
        }
        ExecutorService pool = Executors.newFixedThreadPool(8);
        try {
            List<Future<Optional<KafkaMessage>>> futures = new ArrayList<>();
            for (int i = 0; i < 50; i++) {
                futures.add(pool.submit(() -> store.takeUnmarked(m -> true, TopicStore.Order.ANY)));
            }
            Set<String> values = new HashSet<>();
            for (Future<Optional<KafkaMessage>> future : futures) {
                assertTrue(values.add(future.get(10, TimeUnit.SECONDS).orElseThrow().value()), "сообщение выдано дважды");
            }
            assertEquals(values.size(), 50);
        } finally {
            pool.shutdownNow();
        }
    }

    @Test(groups = {"selftest"}, description = "Лимит количества вытесняет самые старые по timestamp")
    public void maxMessagesEvictsOldest() {
        Clock clock = Clock.systemUTC();
        TopicStore store = new TopicStore(TOPIC, 3, Duration.ofMinutes(60), clock);
        for (int i = 0; i < 5; i++) {
            store.add(message(0, i, 1_000 + i, "m" + i, clock.instant()));
        }
        assertEquals(store.messages().stream().map(KafkaMessage::value).toList(), List.of("m2", "m3", "m4"));
        assertEquals(store.evictedCount(), 2);
    }

    @Test(groups = {"selftest"}, description = "Время жизни считается от момента получения")
    public void ttlEvictsExpired() {
        MutableClock clock = new MutableClock();
        TopicStore store = new TopicStore(TOPIC, 100, Duration.ofMinutes(10), clock);
        store.add(message(0, 0, 1_000, "old", clock.instant()));
        clock.advance(Duration.ofMinutes(9));
        store.add(message(0, 1, 2_000, "fresh", clock.instant()));
        clock.advance(Duration.ofMinutes(2));
        store.evict();
        assertEquals(store.messages().stream().map(KafkaMessage::value).toList(), List.of("fresh"));
        assertEquals(store.evictedCount(), 1);
    }

    @Test(groups = {"selftest"}, description = "Сообщение другого топика в хранилище не принимается")
    public void foreignTopicRejected() {
        TopicStore store = store(Clock.systemUTC());
        KafkaMessage foreign = new KafkaMessage("other", 0, 0, null, null, 0, TimestampType.CREATE_TIME, "x", Instant.now());
        expectThrows(IllegalArgumentException.class, () -> store.add(foreign));
    }
}
