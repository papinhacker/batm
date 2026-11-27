package ru.vtb.core.utils.kafka;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Predicate;

/**
 * Условия поиска по полям JSON-сообщения (перенесено из BatmDpStreamingTests, версия ffecc53).
 */
public final class JsonPredicates {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private JsonPredicates() {
    }

    /**
     * Все поля по JSON Pointer равны ожидаемым значениям (сравнение по {@code asText()}).
     * Отсутствующее поле или null — несовпадение; сообщение, которое не является JSON, — несовпадение.
     *
     * @param expectedFields JSON Pointer → ожидаемое значение (см. {@link #fields(String...)})
     */
    public static Predicate<String> jsonFieldsEqual(Map<String, String> expectedFields) {
        Map<String, String> expected = new LinkedHashMap<>(expectedFields);
        return Described.of("JSON-поля " + expected, message -> {
            JsonNode node;
            try {
                node = MAPPER.readTree(message);
            } catch (JsonProcessingException e) {
                return false; // не JSON — не то сообщение
            }
            if (node == null) {
                return false;
            }
            for (Map.Entry<String, String> field : expected.entrySet()) {
                JsonNode value = node.at(field.getKey());
                if (value.isMissingNode() || value.isNull() || !field.getValue().equals(value.asText())) {
                    return false;
                }
            }
            return true;
        });
    }

    /**
     * @param pointersAndValues пары «JSON Pointer, значение»: {@code fields("/Object/Id/Id", id, "/Object/Status", status)}
     */
    public static Map<String, String> fields(String... pointersAndValues) {
        if (pointersAndValues.length % 2 != 0) {
            throw new IllegalArgumentException("fields(...) ожидает пары «JSON Pointer, значение», передано %d аргументов"
                    .formatted(pointersAndValues.length));
        }
        Map<String, String> result = new LinkedHashMap<>();
        for (int i = 0; i < pointersAndValues.length; i += 2) {
            result.put(pointersAndValues[i], pointersAndValues[i + 1]);
        }
        return result;
    }
}
