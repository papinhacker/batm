package ru.vtb.core.utils.kafka;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.qameta.allure.Allure;
import org.awaitility.Awaitility;
import org.awaitility.core.ConditionTimeoutException;
import ru.vtb.core.common.Log;
import ru.vtb.core.utils.kafka.KafkaMessage.MessageId;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.function.Predicate;
import java.util.function.Supplier;

/**
 * Поиск сообщений в хранилище топика (docs/kafka-design.md, раздел 6).
 * <ul>
 *     <li>ищется среди всех сообщений хранилища (с начала прогона), отметки старых методов KafkaHelper не учитываются;</li>
 *     <li>из нескольких подходящих возвращается последнее по timestamp;</li>
 *     <li>сообщение, которое не десериализуется в нужный тип, пропускается, но попадает в текст ошибки при таймауте;</li>
 *     <li>исключение в самом условии роняет поиск сразу;</li>
 *     <li>при таймауте — AssertionError с диагностикой (топик, кластер, условие, сколько пришло за ожидание,
 *     не десериализованные, последние N сообщений) и вложение Allure «Kafka: диагностика»;</li>
 *     <li>найденное сообщение прикладывается к Allure.</li>
 * </ul>
 */
public final class KafkaSearch {

    /** Куда прикладывать сообщения и диагностику (по умолчанию — Allure). */
    public interface Reporter {
        void attach(String name, String mimeType, String content);

        Reporter ALLURE = (name, mimeType, content) -> Allure.addAttachment(name, mimeType, content);
    }

    /**
     * Где искать.
     *
     * @param clusterError ошибка чтения кластера (null — чтение идёт); проверяется на каждом шаге ожидания
     */
    public record Target(String topic, String cluster, TopicStore store, Supplier<Throwable> clusterError) {
    }

    private static final int VALUE_PREVIEW = 500;
    private static final ObjectMapper PRETTY = new ObjectMapper();

    private final Duration pollInterval;
    private final int lastN;
    private final Reporter reporter;
    private final ObjectMapper mapper;
    private final Clock clock;

    public KafkaSearch(Duration pollInterval, int lastN, Reporter reporter, ObjectMapper mapper, Clock clock) {
        this.pollInterval = pollInterval;
        this.lastN = lastN;
        this.reporter = reporter;
        this.mapper = mapper;
        this.clock = clock;
    }

    /**
     * @return последнее (по timestamp) сообщение типа T, удовлетворяющее условию; по таймауту — AssertionError
     */
    public <T> T await(Target target, Class<T> type, Predicate<T> condition, Duration timeout) {
        requirePositive(timeout, "timeout");
        Log.step("Ждём в топике '%s' сообщение %s: %s".formatted(target.topic(), type.getSimpleName(), Described.describe(condition)));
        Scan<T> scan = new Scan<>(target, type, condition);
        List<Match<T>> found = waitFor(scan, timeout, matches -> !matches.isEmpty());
        if (found.isEmpty()) {
            throw timeout(scan, "не найдено сообщение " + type.getSimpleName(), timeout);
        }
        Match<T> last = found.get(found.size() - 1);
        attachFound(target, last.message());
        return last.value();
    }

    /**
     * @return ровно expectedCount сообщений типа T по условию (в порядке timestamp); меньше за таймаут или больше — AssertionError
     */
    public <T> List<T> awaitAll(Target target, Class<T> type, Predicate<T> condition, int expectedCount, Duration timeout) {
        requirePositive(timeout, "timeout");
        if (expectedCount <= 0) {
            throw new IllegalArgumentException("expectedCount должен быть больше 0: " + expectedCount);
        }
        Log.step("Ждём в топике '%s' ровно %d сообщений %s: %s"
                .formatted(target.topic(), expectedCount, type.getSimpleName(), Described.describe(condition)));
        Scan<T> scan = new Scan<>(target, type, condition);
        List<Match<T>> found = waitFor(scan, timeout, matches -> matches.size() >= expectedCount);
        if (found.size() < expectedCount) {
            throw timeout(scan, "найдено %d из %d сообщений %s".formatted(found.size(), expectedCount, type.getSimpleName()), timeout);
        }
        if (found.size() > expectedCount) {
            StringBuilder text = new StringBuilder("Kafka: ожидалось ровно %d сообщений %s в топике '%s' (кластер '%s'), найдено %d. Условие: %s%nНайденные:%n"
                    .formatted(expectedCount, type.getSimpleName(), target.topic(), target.cluster(), found.size(),
                            Described.describe(condition)));
            found.forEach(m -> text.append("  - ").append(line(m.message())).append(System.lineSeparator()));
            reporter.attach("Kafka: диагностика", "text/plain", text.toString());
            throw new AssertionError(text.toString());
        }
        found.forEach(m -> attachFound(target, m.message()));
        return found.stream().map(Match::value).toList();
    }

    /**
     * @return текст последнего (по timestamp) сообщения, удовлетворяющего условию; по таймауту — AssertionError
     */
    public String awaitRaw(Target target, Predicate<String> condition, Duration timeout) {
        return await(target, String.class, condition, timeout);
    }

    /**
     * Ждёт всё окно и падает, если за это время в топике есть (или уже было с начала прогона) подходящее сообщение.
     *
     * @param window явное окно ожидания (обязательно, больше нуля)
     */
    public void assertNoMessage(Target target, Predicate<String> condition, Duration window) {
        if (window == null || window.isZero() || window.isNegative()) {
            throw new IllegalArgumentException("assertNoMessage требует явное положительное окно ожидания, передано: " + window);
        }
        Log.step("Проверяем, что в топике '%s' в течение %s нет сообщения: %s"
                .formatted(target.topic(), window, Described.describe(condition)));
        Scan<String> scan = new Scan<>(target, String.class, condition);
        List<Match<String>> found = waitFor(scan, window, matches -> !matches.isEmpty());
        if (!found.isEmpty()) {
            KafkaMessage unexpected = found.get(found.size() - 1).message();
            attachFound(target, unexpected);
            String text = "Kafka: в топике '%s' (кластер '%s') есть сообщение, которого не должно быть. Условие: %s%n  - %s"
                    .formatted(target.topic(), target.cluster(), Described.describe(condition), line(unexpected));
            throw new AssertionError(text);
        }
    }

    /* ---------------------------------------------------------------------------------------------------------- */

    private record Match<T>(KafkaMessage message, T value) {
    }

    /** Один поиск: кэш десериализации, не десериализованные сообщения, момент начала ожидания. */
    private final class Scan<T> {
        final Target target;
        final Class<T> type;
        final Predicate<T> condition;
        final Instant started = clock.instant();
        final Map<MessageId, Optional<T>> parsed = new HashMap<>();
        final Map<MessageId, KafkaMessage> notDeserialized = new LinkedHashMap<>();

        Scan(Target target, Class<T> type, Predicate<T> condition) {
            this.target = target;
            this.type = type;
            this.condition = condition;
        }

        List<Match<T>> matches() {
            List<Match<T>> result = new ArrayList<>();
            for (KafkaMessage message : target.store().messages()) {
                Optional<T> value = parsed.computeIfAbsent(message.id(), id -> deserialize(message));
                if (value.isEmpty()) {
                    continue;
                }
                boolean matches;
                try {
                    matches = condition.test(value.get());
                } catch (RuntimeException e) {
                    throw new AssertionError("Kafka: условие поиска «%s» упало на сообщении %s: %s"
                            .formatted(Described.describe(condition), line(message), e), e);
                }
                if (matches) {
                    result.add(new Match<>(message, value.get()));
                }
            }
            return result;
        }

        private Optional<T> deserialize(KafkaMessage message) {
            if (message.value() == null) {
                notDeserialized.put(message.id(), message);
                return Optional.empty();
            }
            if (type == String.class) {
                return Optional.of(type.cast(message.value()));
            }
            try {
                return Optional.of(mapper.readValue(message.value(), type));
            } catch (Exception e) {
                notDeserialized.put(message.id(), message);
                return Optional.empty();
            }
        }
    }

    private <T> List<Match<T>> waitFor(Scan<T> scan, Duration timeout, Predicate<List<Match<T>>> done) {
        List<List<Match<T>>> last = new ArrayList<>(List.of(List.of()));
        List<AssertionError> failure = new ArrayList<>();
        try {
            Awaitility.await()
                    .pollInSameThread()
                    .pollDelay(Duration.ZERO)
                    .pollInterval(pollInterval)
                    .atMost(timeout)
                    .until(() -> {
                        Throwable clusterError = scan.target.clusterError().get();
                        if (clusterError != null) {
                            failure.add(new AssertionError("Kafka: топик '%s' не читается — чтение кластера '%s' остановлено: %s"
                                    .formatted(scan.target.topic(), scan.target.cluster(), clusterError), clusterError));
                            return true;
                        }
                        try {
                            List<Match<T>> matches = scan.matches();
                            last.set(0, matches);
                            return done.test(matches);
                        } catch (AssertionError e) {
                            failure.add(e);
                            return true;
                        }
                    });
        } catch (ConditionTimeoutException e) {
            // таймаут: вызывающий метод проверяет результат и строит диагностику
        }
        if (!failure.isEmpty()) {
            throw failure.get(0);
        }
        return last.get(0);
    }

    private AssertionError timeout(Scan<?> scan, String what, Duration timeout) {
        Target target = scan.target;
        List<KafkaMessage> all = target.store().messages();
        long arrived = all.stream().filter(m -> !m.receivedAt().isBefore(scan.started)).count();
        StringBuilder text = new StringBuilder();
        String nl = System.lineSeparator();
        text.append("Kafka: %s за %s — топик '%s', кластер '%s'".formatted(what, timeout, target.topic(), target.cluster())).append(nl);
        text.append("Условие: ").append(Described.describe(scan.condition)).append(nl);
        text.append("Сообщений пришло в топик за ожидание: %d (в хранилище всего: %d, вытеснено лимитами: %d)"
                .formatted(arrived, all.size(), target.store().evictedCount())).append(nl);
        if (scan.type != String.class) {
            text.append("Не десериализовались в %s и пропущены: %d".formatted(scan.type.getSimpleName(), scan.notDeserialized.size())).append(nl);
            scan.notDeserialized.values().stream().limit(lastN)
                    .forEach(m -> text.append("  - ").append(line(m)).append(nl));
        }
        List<KafkaMessage> tail = all.subList(Math.max(0, all.size() - lastN), all.size());
        text.append("Последние %d сообщений топика:".formatted(tail.size())).append(nl);
        tail.forEach(m -> text.append("  - ").append(line(m)).append(nl));
        reporter.attach("Kafka: диагностика", "text/plain", text.toString());
        return new AssertionError(text.toString());
    }

    private void attachFound(Target target, KafkaMessage message) {
        reporter.attach("Kafka ← " + target.topic(), "text/plain",
                "partition=%d offset=%d timestamp=%s key=%s headers=%s%n%n%s".formatted(message.partition(), message.offset(),
                        Instant.ofEpochMilli(message.timestamp()), message.key(), message.headers(), pretty(message.value())));
    }

    static String pretty(String value) {
        if (value == null) {
            return "null";
        }
        try {
            JsonNode node = PRETTY.readTree(value);
            return node == null ? value : PRETTY.writerWithDefaultPrettyPrinter().writeValueAsString(node);
        } catch (Exception e) {
            return value; // не JSON — как есть
        }
    }

    private static String line(KafkaMessage message) {
        String value = String.valueOf(message.value()).replaceAll("\\s+", " ");
        if (value.length() > VALUE_PREVIEW) {
            value = value.substring(0, VALUE_PREVIEW) + "…";
        }
        return "partition=%d offset=%d timestamp=%s key=%s: %s".formatted(message.partition(), message.offset(),
                Instant.ofEpochMilli(message.timestamp()), message.key(), value);
    }

    private static void requirePositive(Duration duration, String name) {
        if (duration == null || duration.isZero() || duration.isNegative()) {
            throw new IllegalArgumentException("%s должен быть больше нуля, передано: %s".formatted(name, duration));
        }
    }
}
