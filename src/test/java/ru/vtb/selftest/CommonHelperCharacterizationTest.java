package ru.vtb.selftest;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import org.testng.annotations.Test;
import ru.vtb.core.models.kafka.batm_clearing_out_batm_trans_data.BatmClearingOutBatmTransAgrDataRootMessage;
import ru.vtb.core.models.kafka.batm_clearing_out_batm_trans_data.TransactionsItem;
import ru.vtb.core.utils.CommonHelper;

import java.util.List;

import static org.testng.Assert.*;

/**
 * Характеризационные тесты: фиксируют ТЕКУЩЕЕ поведение CommonHelper
 * (сериализация/десериализация JSON, parseDateFromPatternToAnotherPattern).
 */
public class CommonHelperCharacterizationTest {

    @JsonPropertyOrder({"name", "count", "nullable"})
    public static class Sample {
        public String name;
        public Integer count;
        public String nullable;

        public Sample() {
        }

        Sample(String name, Integer count, String nullable) {
            this.name = name;
            this.count = count;
            this.nullable = nullable;
        }
    }

    /** Jackson DefaultPrettyPrinter использует системный перевод строки — сравниваем без учёта \r. */
    private static String lf(String s) {
        return s.replace("\r\n", "\n");
    }

    private static TransactionsItem sampleItem() {
        return TransactionsItem.builder()
                .amount(1500.5)
                .authCode("123456")
                .nameFile("D_038cto05.123")
                .mcc("6011")
                .rrn("000000000001")
                .binRange("42926700")
                .transType("Cash-In")
                .transDate("2024-10-18T14:30:43")
                .atmId("LA151190")
                .currency("643")
                .bankProductType("ExVTB24")
                .arn("12345678901234567890123")
                .build();
    }

    // ---------- сериализация ----------

    @Test(groups = {"selftest"}, description = "serializeToJsonString: pretty print, null-поля включаются")
    public void serializeIncludesNulls() {
        assertEquals(lf(CommonHelper.serializeToJsonString(new Sample("a", 1, null))),
                "{\n  \"name\" : \"a\",\n  \"count\" : 1,\n  \"nullable\" : null\n}");
    }

    @Test(groups = {"selftest"}, description = "serializeToJsonStringIgnoringNullableFields: null-поля пропускаются")
    public void serializeIgnoringNullsSkipsNulls() {
        assertEquals(lf(CommonHelper.serializeToJsonStringIgnoringNullableFields(new Sample("a", 1, null))),
                "{\n  \"name\" : \"a\",\n  \"count\" : 1\n}");
        assertEquals(lf(CommonHelper.serializeToJsonStringIgnoringNullableFields(new Sample(null, null, null))), "{ }");
    }

    @Test(groups = {"selftest"}, description = "Модель Kafka: порядок полей — порядок объявления, Double как 1500.5, "
            + "toJson() из Jsonable не попадает в JSON")
    public void serializeKafkaModel() {
        String expected = """
                {
                  "messageId" : "m-1",
                  "transactions" : [ {
                    "amount" : 1500.5,
                    "authCode" : "123456",
                    "nameFile" : "D_038cto05.123",
                    "mcc" : "6011",
                    "rrn" : "000000000001",
                    "binRange" : "42926700",
                    "transType" : "Cash-In",
                    "transDate" : "2024-10-18T14:30:43",
                    "atmId" : "LA151190",
                    "currency" : "643",
                    "bankProductType" : "ExVTB24",
                    "arn" : "12345678901234567890123"
                  } ]
                }""";
        var msg = BatmClearingOutBatmTransAgrDataRootMessage.builder()
                .messageId("m-1").transactions(List.of(sampleItem())).build();
        assertEquals(lf(CommonHelper.serializeToJsonStringIgnoringNullableFields(msg)), expected);
    }

    @Test(groups = {"selftest"}, description = "serializeToJsonString модели: все 15 полей, отсутствующие — null")
    public void serializeKafkaModelWithNulls() {
        String json = lf(CommonHelper.serializeToJsonString(sampleItem()));
        assertTrue(json.contains("\"amountFee\" : null"), json);
        assertTrue(json.contains("\"id\" : null"), json);
        assertTrue(json.contains("\"atmMigration\" : null"), json);
        assertFalse(json.contains("toJson"), json);
        assertEquals(json.lines().filter(l -> l.contains(" : ")).count(), 15, json);
    }

    @Test(groups = {"selftest"}, description = "Сериализация -> десериализация возвращает равный объект")
    public void jsonRoundTrip() {
        TransactionsItem item = sampleItem();
        item.setAmountFee(1.0);
        item.setAtmMigration(true);
        assertEquals(CommonHelper.deserializeJsonToObject(CommonHelper.serializeToJsonString(item), TransactionsItem.class), item);
        assertEquals(CommonHelper.deserializeJsonToObject(
                CommonHelper.serializeToJsonStringIgnoringNullableFields(item), TransactionsItem.class), item);
    }

    // ---------- десериализация ----------

    @Test(groups = {"selftest"}, description = "Целое число в JSON десериализуется в Double")
    public void deserializeIntegerIntoDouble() {
        TransactionsItem item = CommonHelper.deserializeJsonToObject("{\"amount\": 100}", TransactionsItem.class);
        assertEquals(item.getAmount(), Double.valueOf(100.0));
        assertNull(item.getRrn());
    }

    @Test(groups = {"selftest"}, description = "Неизвестное поле -> RuntimeException (FAIL_ON_UNKNOWN_PROPERTIES включён)")
    public void deserializeUnknownPropertyThrows() {
        RuntimeException e = expectThrows(RuntimeException.class,
                () -> CommonHelper.deserializeJsonToObject("{\"unknown\": 1}", TransactionsItem.class));
        assertTrue(e.getMessage().startsWith("Unable to deserialize json to object '"
                + TransactionsItem.class.getName() + "'. Reason is: Unrecognized field \"unknown\""), e.getMessage());
    }

    // KNOWN-BUG: причина (IOException) не передаётся в RuntimeException как cause — остаётся только её текст.
    @Test(groups = {"selftest"}, description = "Битый JSON -> RuntimeException с текстом причины, без cause (KNOWN-BUG)")
    public void deserializeBrokenJsonThrowsWithoutCause() {
        RuntimeException e = expectThrows(RuntimeException.class,
                () -> CommonHelper.deserializeJsonToObject("{not json", TransactionsItem.class));
        assertTrue(e.getMessage().startsWith("Unable to deserialize json to object '"
                + TransactionsItem.class.getName() + "'. Reason is: "), e.getMessage());
        assertNull(e.getCause());
    }

    // ---------- parseDateFromPatternToAnotherPattern ----------

    @Test(groups = {"selftest"}, description = "Перевод даты между шаблонами (как в UfxHelper)")
    public void parseDateConvertsPatterns() {
        assertEquals(CommonHelper.parseDateFromPatternToAnotherPattern(
                "yyyyMMddHHmmss", "yyyy-MM-dd'T'HH:mm:ss", "20241018143043"), "2024-10-18T14:30:43");
        assertEquals(CommonHelper.parseDateFromPatternToAnotherPattern(
                "yyyy-MM-dd", "yyyyMMdd", "2026-11-26"), "20261126");
        assertEquals(CommonHelper.parseDateFromPatternToAnotherPattern(
                "yyyy-MM-dd HH:mm:ss.SSS", "yyyy-MM-dd", "2026-11-26 23:59:59.999"), "2026-11-26");
    }

    @Test(groups = {"selftest"}, description = "Непарсящаяся дата -> RuntimeException с понятным текстом")
    public void parseDateInvalidThrows() {
        RuntimeException e = expectThrows(RuntimeException.class,
                () -> CommonHelper.parseDateFromPatternToAnotherPattern("yyyy-MM-dd", "yyyyMMdd", "26.11.2026"));
        assertEquals(e.getMessage(),
                "Unable to parse date '26.11.2026' from pattern 'yyyy-MM-dd' to pattern 'yyyyMMdd'");
        // KNOWN-BUG: исходный ParseException не передаётся как cause.
        assertNull(e.getCause());
    }

    // KNOWN-BUG: SimpleDateFormat нестрогий (lenient) — несуществующие даты молча переносятся.
    @Test(groups = {"selftest"}, description = "Несуществующая дата не падает, а переносится (KNOWN-BUG)")
    public void parseDateIsLenient() {
        assertEquals(CommonHelper.parseDateFromPatternToAnotherPattern("yyyyMMdd", "yyyy-MM-dd", "20241332"), "2025-02-01");
        assertEquals(CommonHelper.parseDateFromPatternToAnotherPattern("yyyy-MM-dd", "yyyy-MM-dd", "2026-02-30"), "2026-03-02");
        assertEquals(CommonHelper.parseDateFromPatternToAnotherPattern("HHmmss", "HH:mm:ss", "256161"), "02:02:01");
    }

    // KNOWN-BUG: хвост строки после разобранной даты игнорируется (SimpleDateFormat.parse не требует разбора всей строки).
    @Test(groups = {"selftest"}, description = "Мусор после даты игнорируется (KNOWN-BUG)")
    public void parseDateIgnoresTrailingGarbage() {
        assertEquals(CommonHelper.parseDateFromPatternToAnotherPattern("yyyy-MM-dd", "yyyyMMdd", "2026-11-26garbage"), "20261126");
        // KNOWN-BUG: строка длиннее шаблона из одних цифр. Последнее поле (dd) забирает ВСЕ оставшиеся цифры:
        // "26143043" дней от 2026-11 -> год 73604. Ошибки нет, дата молча становится бессмысленной.
        assertEquals(CommonHelper.parseDateFromPatternToAnotherPattern("yyyyMMdd", "yyyy-MM-dd", "20261126143043"), "73604-01-20");
    }

    @Test(groups = {"selftest"}, description = "Разделители не обязаны совпадать по количеству цифр: '2026-1-5' принимается")
    public void parseDateAcceptsShortFields() {
        assertEquals(CommonHelper.parseDateFromPatternToAnotherPattern("yyyy-MM-dd", "yyyyMMdd", "2026-1-5"), "20260105");
    }
}
