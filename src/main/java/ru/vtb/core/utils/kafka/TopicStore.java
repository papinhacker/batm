package ru.vtb.core.utils.kafka;

import ru.vtb.core.utils.kafka.KafkaMessage.MessageId;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Predicate;

/**
 * Сообщения одного топика (docs/kafka-design.md, раздел 3).
 * <ul>
 *     <li>ключ — топик + партиция + offset: сообщения разных партиций с одинаковым offset хранятся оба,
 *     повторное получение того же сообщения дубля не создаёт;</li>
 *     <li>порядок — по timestamp, затем partition, offset (FIRST/LAST — по timestamp);</li>
 *     <li>ограничения: не больше maxMessages сообщений и не старше ttl (от момента получения) — старейшие вытесняются;</li>
 *     <li>сообщения не удаляются при поиске. Для старых методов KafkaHelper есть отметки «уже возвращено»:
 *     {@link #takeUnmarked} возвращает каждое сообщение не больше одного раза.</li>
 * </ul>
 * Потокобезопасен: пишет consumer-поток, читают тесты.
 */
public final class TopicStore {

    public enum Order {FIRST, LAST, ANY}

    private record OrderKey(long timestamp, int partition, long offset) implements Comparable<OrderKey> {
        private static final Comparator<OrderKey> COMPARATOR = Comparator.comparingLong(OrderKey::timestamp)
                .thenComparingInt(OrderKey::partition)
                .thenComparingLong(OrderKey::offset);

        @Override
        public int compareTo(OrderKey other) {
            return COMPARATOR.compare(this, other);
        }
    }

    private final String topic;
    private final int maxMessages;
    private final Duration ttl;
    private final Clock clock;
    private final ConcurrentSkipListMap<OrderKey, KafkaMessage> messages = new ConcurrentSkipListMap<>();
    private final Map<MessageId, OrderKey> index = new ConcurrentHashMap<>();
    private final Set<MessageId> marked = ConcurrentHashMap.newKeySet();
    private final AtomicLong evicted = new AtomicLong();
    /** Полный проход по времени жизни — не чаще раза в секунду (порядок хранения — по timestamp, а не по receivedAt). */
    private volatile Instant nextTtlCheck = Instant.MIN;

    public TopicStore(String topic, int maxMessages, Duration ttl, Clock clock) {
        if (maxMessages <= 0) {
            throw new IllegalArgumentException("maxMessages должен быть больше 0: " + maxMessages);
        }
        this.topic = topic;
        this.maxMessages = maxMessages;
        this.ttl = Objects.requireNonNull(ttl);
        this.clock = Objects.requireNonNull(clock);
    }

    public String getTopic() {
        return topic;
    }

    /**
     * @return true, если сообщение добавлено; false — такое (топик+партиция+offset) уже есть
     */
    public boolean add(KafkaMessage message) {
        if (!topic.equals(message.topic())) {
            throw new IllegalArgumentException("Сообщение топика '%s' не может храниться в хранилище топика '%s'"
                    .formatted(message.topic(), topic));
        }
        OrderKey key = new OrderKey(message.timestamp(), message.partition(), message.offset());
        if (index.putIfAbsent(message.id(), key) != null) {
            return false;
        }
        messages.put(key, message);
        evict();
        return true;
    }

    /**
     * Вытесняет самые старые сообщения сверх лимита количества и (не чаще раза в секунду) сообщения старше ttl.
     */
    public void evict() {
        Instant now = clock.instant();
        if (!now.isBefore(nextTtlCheck)) {
            nextTtlCheck = now.plusSeconds(1);
            Instant expiry = now.minus(ttl);
            for (Iterator<KafkaMessage> it = messages.values().iterator(); it.hasNext(); ) {
                KafkaMessage message = it.next();
                if (message.receivedAt().isBefore(expiry)) {
                    it.remove();
                    forget(message);
                }
            }
        }
        while (messages.size() > maxMessages) {
            Map.Entry<OrderKey, KafkaMessage> oldest = messages.pollFirstEntry();
            if (oldest == null) {
                break;
            }
            forget(oldest.getValue());
        }
    }

    private void forget(KafkaMessage message) {
        index.remove(message.id());
        marked.remove(message.id());
        evicted.incrementAndGet();
    }

    /** @return все сообщения в порядке timestamp, partition, offset */
    public List<KafkaMessage> messages() {
        return List.copyOf(messages.values());
    }

    public int size() {
        return messages.size();
    }

    /** @return сколько сообщений вытеснено лимитами за всё время */
    public long evictedCount() {
        return evicted.get();
    }

    /**
     * @return первое/последнее (по timestamp) или любое сообщение, удовлетворяющее условию; отметки не учитываются
     */
    public Optional<KafkaMessage> find(Predicate<KafkaMessage> condition, Order order) {
        return candidates(order).stream().filter(condition).findFirst();
    }

    public boolean anyMatch(Predicate<KafkaMessage> condition, boolean skipMarked) {
        return messages.values().stream()
                .filter(m -> !skipMarked || !marked.contains(m.id()))
                .anyMatch(condition);
    }

    /**
     * Для старых методов KafkaHelper: находит неотмеченное сообщение по условию и порядку и отмечает его,
     * чтобы следующие такие вызовы его не вернули. При параллельных вызовах каждое сообщение достаётся одному.
     */
    public Optional<KafkaMessage> takeUnmarked(Predicate<KafkaMessage> condition, Order order) {
        for (KafkaMessage message : candidates(order)) {
            if (!marked.contains(message.id()) && condition.test(message) && marked.add(message.id())) {
                return Optional.of(message);
            }
        }
        return Optional.empty();
    }

    public boolean isMarked(MessageId id) {
        return marked.contains(id);
    }

    private Collection<KafkaMessage> candidates(Order order) {
        return order == Order.LAST ? messages.descendingMap().values() : messages.values();
    }
}
