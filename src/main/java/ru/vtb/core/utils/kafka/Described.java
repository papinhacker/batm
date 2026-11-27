package ru.vtb.core.utils.kafka;

import java.util.Objects;
import java.util.function.Predicate;

/**
 * Условие поиска с человекочитаемым описанием — для логов, шагов Allure и текста ошибки при таймауте.
 * <pre>
 * KafkaHelper.await(TOPIC, Msg.class, Described.of("rrn=" + rrn, m -&gt; rrn.equals(m.getRrn())), timeout);
 * </pre>
 */
public final class Described<T> implements Predicate<T> {

    private final String description;
    private final Predicate<T> predicate;

    private Described(String description, Predicate<T> predicate) {
        this.description = Objects.requireNonNull(description);
        this.predicate = Objects.requireNonNull(predicate);
    }

    public static <T> Described<T> of(String description, Predicate<T> predicate) {
        return new Described<>(description, predicate);
    }

    /**
     * @return описание условия: у {@link Described} — заданное, у остальных — toString()
     */
    public static String describe(Predicate<?> predicate) {
        return predicate instanceof Described<?> described ? described.description : String.valueOf(predicate);
    }

    @Override
    public boolean test(T value) {
        return predicate.test(value);
    }

    @Override
    public String toString() {
        return description;
    }
}
