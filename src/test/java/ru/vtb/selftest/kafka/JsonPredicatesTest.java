package ru.vtb.selftest.kafka;

import org.testng.annotations.Test;
import ru.vtb.core.utils.kafka.Described;

import java.util.Map;
import java.util.function.Predicate;

import static org.testng.Assert.*;
import static ru.vtb.core.utils.kafka.JsonPredicates.fields;
import static ru.vtb.core.utils.kafka.JsonPredicates.jsonFieldsEqual;

/**
 * Условия по полям JSON и описания условий.
 */
public class JsonPredicatesTest {

    private static final String UASP = """
            {"Object": {"Id": {"Id": "instr-1"}, "CashMachine": {"Id": {"Id": "ATM42"}}, "Status": "New", "Amount": 100}}""";

    @Test(groups = {"selftest"}, description = "Совпадают все поля по JSON Pointer, сравнение по asText()")
    public void matchesAllFields() {
        assertTrue(jsonFieldsEqual(fields("/Object/CashMachine/Id/Id", "ATM42", "/Object/Status", "New")).test(UASP));
        assertTrue(jsonFieldsEqual(fields("/Object/Amount", "100")).test(UASP), "число сравнивается как текст");
    }

    @Test(groups = {"selftest"}, description = "Другое значение, отсутствующее поле, null — несовпадение")
    public void mismatches() {
        assertFalse(jsonFieldsEqual(fields("/Object/Status", "Accepted")).test(UASP));
        assertFalse(jsonFieldsEqual(fields("/Object/Missing", "x")).test(UASP));
        assertFalse(jsonFieldsEqual(fields("/Object/Status", "null")).test("{\"Object\": {\"Status\": null}}"));
    }

    @Test(groups = {"selftest"}, description = "Сообщение, которое не JSON, — несовпадение, а не исключение")
    public void notJsonIsMismatch() {
        assertFalse(jsonFieldsEqual(fields("/a", "b")).test("plain text"));
        assertFalse(jsonFieldsEqual(fields("/a", "b")).test(""));
    }

    @Test(groups = {"selftest"}, description = "fields(...) — пары; нечётное число аргументов — ошибка")
    public void fieldsPairs() {
        assertEquals(fields("/a", "1", "/b", "2"), Map.of("/a", "1", "/b", "2"));
        expectThrows(IllegalArgumentException.class, () -> fields("/a"));
    }

    @Test(groups = {"selftest"}, description = "Описание: jsonFieldsEqual и Described — заданное, остальные — toString")
    public void descriptions() {
        assertEquals(Described.describe(jsonFieldsEqual(fields("/a", "1"))), "JSON-поля {/a=1}");
        assertEquals(Described.describe(Described.of("rrn=1", v -> true)), "rrn=1");
        Predicate<String> plain = new Predicate<>() {
            @Override
            public boolean test(String s) {
                return true;
            }

            @Override
            public String toString() {
                return "plain";
            }
        };
        assertEquals(Described.describe(plain), "plain");
        assertTrue(Described.of("x", (String v) -> v.isEmpty()).test(""));
    }
}
