package ru.vtb.api.tests.batm_dp;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.qameta.allure.TmsLink;
import org.awaitility.Awaitility;
import org.awaitility.core.ConditionTimeoutException;
import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;
import ru.vtb.api.state.BaseAPIState;
import ru.vtb.core.common.Log;
import ru.vtb.core.data.static_data.KafkaTopic;
import ru.vtb.core.data.static_data.dto.TransactionDto;
import ru.vtb.core.models.kafka.batm_trans_out_batm_dp_inst.BatmTransOutBatmDpInstRootMessage;
import ru.vtb.core.models.kafka.batm_trans_out_batm_dp_trans.BatmTransOutBatmDptransRootMessage;
import ru.vtb.core.utils.B2LHelper;
import ru.vtb.core.utils.CommonHelper;
import ru.vtb.core.utils.DBHelper;
import ru.vtb.core.utils.KafkaHelper;
import ru.vtb.core.utils.kafka.Described;

import java.io.File;
import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import static org.apache.commons.lang3.StringUtils.isNotBlank;
import static ru.vtb.core.data.static_data.B2LMask._298cto;
import static ru.vtb.core.data.static_data.KafkaTopic.*;
import static ru.vtb.core.data.static_data.MapsKeysConstants.TERMINAL_ID;
import static ru.vtb.core.data.static_data.S3Constants.BATM_OUTPUT;
import static ru.vtb.core.data.static_data.S3Constants.IF_BATM_WAY_IN;
import static ru.vtb.core.utils.kafka.JsonPredicates.fields;
import static ru.vtb.core.utils.kafka.JsonPredicates.jsonFieldsEqual;

/**
 * BATM-DP Стримминг в платформу данных / Операции клиринга.
 * <p>
 * Каждый тест независим: генерирует свою транзакцию Cash-in по карте ВТБ на случайном терминале,
 * маршрутизируемом в ppks+gp, загружает файл клиринга в S3 и проверяет свой статус ФИ/ФТ.
 * <p>
 * Шаги UASP и ODS работают с отдельными Kafka-кластерами, которые пока не настроены (заглушки):
 * kafka.uasp.bootstrap.servers, kafka.ods.bootstrap.servers, имена топиков UASP в {@link KafkaTopic},
 * критерий поиска сообщения ODS в {@link #odsOutboxMessageCondition(String)}.
 */
public class BatmDpStreamingTests extends BaseAPIState {

    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final long DB_POLL_INTERVAL_MILLIS = 200;

    private static final String SERVICE = "batm_transactions_agr";

    /* статусы в сообщениях kafka */
    private static final String NEW = "New";
    private static final String ACCEPTED = "Accepted";
    private static final String PROCESSING = "Processing";
    private static final String EXECUTED = "Executed";

    /* статусы в batm_outbox */
    private static final String DB_ACCEPTED = "ACCEPTED";
    private static final String DB_PROCESSING = "PROCESSING";
    private static final String DB_EXECUTED = "EXECUTED";

    private static final String INSTRUCTION_TABLE = "batm_outbox.instruction";
    private static final String TRANSACTION_TABLE = "batm_outbox.transaction";

    @BeforeMethod
    public void before() {
        setAllureInfo("BATM DP", "Операции клиринга");
    }

    @Test(description = "Проверка формирования инструкции status=New")
    @TmsLink("BATM-658")
    public void testBATM658() {
        String rrn = uploadClearingCashInTransaction();

        // шаг 1
        var instruction = awaitInstructionNew(rrn);
        String instructionId = instruction.getInstructionId();

        // шаг 2
        Map<String, Object> row = awaitOutboxRecord(INSTRUCTION_TABLE,
                () -> DBHelper.BatmDP.getInstructionRows(instructionId), null).get(0);
        softAssertEquals(row.get("service"), SERVICE, "Поле 'service' в %s".formatted(INSTRUCTION_TABLE));
        softAssertNull(row.get("sent_to_dp"), "Поле 'sent_to_dp' в %s должно быть NULL".formatted(INSTRUCTION_TABLE));
        softAssertNull(row.get("execution_dt"), "Поле 'execution_dt' в %s должно быть NULL".formatted(INSTRUCTION_TABLE));
        softAssertAll();

        // шаг 3
        checkUaspInstructionMessage(instruction.getAtmId(), NEW);
    }

    @Test(description = "Проверка формирования инструкции status=Accepted")
    @TmsLink("BATM-659")
    public void testBATM659() {
        String rrn = uploadClearingCashInTransaction();
        var instruction = awaitInstructionNew(rrn);
        String instructionId = instruction.getInstructionId();

        // запись в БД отслеживается сразу после получения id, чтобы зафиксировать её состояние до обновления
        List<Map<String, Object>> history = awaitOutboxRecord(INSTRUCTION_TABLE,
                () -> DBHelper.BatmDP.getInstructionRows(instructionId), DB_ACCEPTED);

        // шаг 1
        awaitInstructionEvent(instructionId, ACCEPTED, long_timeout);

        // шаг 2
        Map<String, Object> row = last(history);
        softAssertNull(row.get("sent_to_dp"), "Поле 'sent_to_dp' в %s должно быть NULL".formatted(INSTRUCTION_TABLE));
        softAssertNull(row.get("execution_dt"), "Поле 'execution_dt' в %s должно быть NULL".formatted(INSTRUCTION_TABLE));
        softAssertUpdated(history, DB_ACCEPTED, INSTRUCTION_TABLE);
        softAssertAll();

        // шаг 3
        checkUaspInstructionMessage(instruction.getAtmId(), ACCEPTED);
    }

    @Test(description = "Проверка формирования транзакции status=New")
    @TmsLink("BATM-660")
    public void testBATM660() {
        String rrn = uploadClearingCashInTransaction();
        var instruction = awaitInstructionNew(rrn);
        String instructionId = instruction.getInstructionId();

        // шаг 1
        var transaction = awaitTransactionNew(instructionId);
        String transactionId = transaction.getTransactionId();

        // шаг 2
        Map<String, Object> row = awaitOutboxRecord(TRANSACTION_TABLE,
                () -> DBHelper.BatmDP.getTransactionRows(transactionId), null).get(0);
        softAssertEquals(row.get("service"), SERVICE, "Поле 'service' в %s".formatted(TRANSACTION_TABLE));
        softAssertNull(row.get("sent_to_dp"), "Поле 'sent_to_dp' в %s должно быть NULL".formatted(TRANSACTION_TABLE));
        softAssertAll();

        // шаг 3
        checkUaspTransactionMessage(instruction.getAtmId(), NEW);
    }

    @Test(description = "Проверка формирования инструкции status=Processing")
    @TmsLink("BATM-661")
    public void testBATM661() {
        String rrn = uploadClearingCashInTransaction();
        var instruction = awaitInstructionNew(rrn);
        String instructionId = instruction.getInstructionId();

        List<Map<String, Object>> history = awaitOutboxRecord(INSTRUCTION_TABLE,
                () -> DBHelper.BatmDP.getInstructionRows(instructionId), DB_PROCESSING);

        // шаг 1
        awaitInstructionEvent(instructionId, PROCESSING, long_timeout);

        // шаг 2
        Map<String, Object> row = last(history);
        softAssertNull(row.get("sent_to_dp"), "Поле 'sent_to_dp' в %s должно быть NULL".formatted(INSTRUCTION_TABLE));
        softAssertNull(row.get("execution_dt"), "Поле 'execution_dt' в %s должно быть NULL".formatted(INSTRUCTION_TABLE));
        softAssertUpdated(history, DB_PROCESSING, INSTRUCTION_TABLE);
        softAssertAll();

        // шаг 3
        checkUaspInstructionMessage(instruction.getAtmId(), PROCESSING);
    }

    @Test(description = "Проверка формирования транзакции status=Accepted")
    @TmsLink("BATM-662")
    public void testBATM662() {
        String rrn = uploadClearingCashInTransaction();
        var instruction = awaitInstructionNew(rrn);
        String transactionId = awaitTransactionNew(instruction.getInstructionId())
                .getTransactionId();

        List<Map<String, Object>> history = awaitOutboxRecord(TRANSACTION_TABLE,
                () -> DBHelper.BatmDP.getTransactionRows(transactionId), DB_ACCEPTED);

        // шаг 1
        awaitTransactionEvent(transactionId, ACCEPTED, long_timeout);

        // шаг 2
        Map<String, Object> row = last(history);
        softAssertNull(row.get("sent_to_dp"), "Поле 'sent_to_dp' в %s должно быть NULL".formatted(TRANSACTION_TABLE));
        softAssertUpdated(history, DB_ACCEPTED, TRANSACTION_TABLE);
        softAssertAll();

        // шаг 3
        checkUaspTransactionMessage(instruction.getAtmId(), ACCEPTED);
    }

    @Test(description = "Проверка формирования транзакции status=Processing")
    @TmsLink("BATM-663")
    public void testBATM663() {
        String rrn = uploadClearingCashInTransaction();
        var instruction = awaitInstructionNew(rrn);
        String transactionId = awaitTransactionNew(instruction.getInstructionId())
                .getTransactionId();

        List<Map<String, Object>> history = awaitOutboxRecord(TRANSACTION_TABLE,
                () -> DBHelper.BatmDP.getTransactionRows(transactionId), DB_PROCESSING);

        // шаг 1
        var event = awaitTransactionEvent(transactionId, PROCESSING, long_timeout);
        softAssertTrue(isNotBlank(event.getDebit().getProductOperationId()) || isNotBlank(event.getCredit().getProductOperationId()),
                "В сообщении transactionEvent со status=Processing должен быть debit.productOperationId или credit.productOperationId");

        // шаг 2
        Map<String, Object> row = last(history);
        softAssertNull(row.get("sent_to_dp"), "Поле 'sent_to_dp' в %s должно быть NULL".formatted(TRANSACTION_TABLE));
        softAssertTrue(row.get("debit_prod_operation_id") != null || row.get("credit_prod_operation_id") != null,
                "В %s должно быть заполнено поле 'debit_prod_operation_id' или 'credit_prod_operation_id'".formatted(TRANSACTION_TABLE));
        softAssertUpdated(history, DB_PROCESSING, TRANSACTION_TABLE);
        softAssertAll();

        // шаг 3
        checkUaspTransactionMessage(instruction.getAtmId(), PROCESSING);
    }

    @Test(description = "Проверка формирования инструкции status=Executed")
    @TmsLink("BATM-664")
    public void testBATM664() {
        String rrn = uploadClearingCashInTransaction();
        var instruction = awaitInstructionNew(rrn);
        String instructionId = instruction.getInstructionId();

        List<Map<String, Object>> history = awaitOutboxRecord(INSTRUCTION_TABLE,
                () -> DBHelper.BatmDP.getInstructionRows(instructionId), DB_EXECUTED);

        // шаг 1
        awaitInstructionEvent(instructionId, EXECUTED, long_timeout);

        // шаг 2
        Map<String, Object> row = last(history);
        softAssertEquals(row.get("sent_to_dp"), true, "Поле 'sent_to_dp' в %s".formatted(INSTRUCTION_TABLE));
        softAssertNotNull(row.get("execution_dt"), "Поле 'execution_dt' в %s должно быть заполнено".formatted(INSTRUCTION_TABLE));
        softAssertUpdated(history, DB_EXECUTED, INSTRUCTION_TABLE);

        // шаги 3, 4
        checkOutboxMessagesSent(instructionId);
        softAssertAll();

        // шаг 5
        checkOdsOutboxMessage(instructionId);

        // шаг 6
        checkUaspInstructionMessage(instruction.getAtmId(), EXECUTED);
    }

    @Test(description = "Проверка формирования транзакции status=Executed")
    @TmsLink("BATM-665")
    public void testBATM665() {
        String rrn = uploadClearingCashInTransaction();
        var instruction = awaitInstructionNew(rrn);
        String transactionId = awaitTransactionNew(instruction.getInstructionId())
                .getTransactionId();

        List<Map<String, Object>> history = awaitOutboxRecord(TRANSACTION_TABLE,
                () -> DBHelper.BatmDP.getTransactionRows(transactionId), DB_EXECUTED);

        // шаг 1
        awaitTransactionEvent(transactionId, EXECUTED, long_timeout);

        // шаг 2
        Map<String, Object> row = last(history);
        softAssertEquals(row.get("sent_to_dp"), true, "Поле 'sent_to_dp' в %s".formatted(TRANSACTION_TABLE));
        softAssertUpdated(history, DB_EXECUTED, TRANSACTION_TABLE);

        // шаги 3, 4
        checkOutboxMessagesSent(transactionId);
        softAssertAll();

        // шаг 5
        checkOdsOutboxMessage(transactionId);

        // шаг 6
        checkUaspTransactionMessage(instruction.getAtmId(), EXECUTED);
    }

    /* ******************************
     ********** Предусловие *********
     ********************************/

    /**
     * Генерирует транзакцию Cash-in по карте ВТБ на случайном терминале, маршрутизируемом в ppks+gp,
     * и загружает файл клиринга 298cto в S3.
     *
     * @return rrn сгенерированной транзакции
     */
    private String uploadClearingCashInTransaction() {
        String terminalId = CommonHelper.getRandomFromList(DBHelper.TransAgr.getTerminalIdsToGenerator());
        String b2lFilename = B2LHelper.generateBase2LikeFileName(_298cto);
        TransactionDto transaction = B2LHelper.getNewCashInTransactionReference(Map.of(TERMINAL_ID, terminalId));
        File b2lFile = B2LHelper.generateBase2LikeFile(b2lFilename, transaction);
        s3Service.putObject(IF_BATM_WAY_IN, BATM_OUTPUT, b2lFile);
        String rrn = transaction.getRetrievalReferenceNumber();
        Log.info("Терминал '%s', файл '%s', rrn '%s'".formatted(terminalId, b2lFilename, rrn));
        return rrn;
    }

    /* ******************************
     ********* Kafka BATM ***********
     ********************************/

    private BatmTransOutBatmDpInstRootMessage awaitInstructionNew(String rrn) {
        return KafkaHelper.await(BATM_TRANS_OUT_BATM_DP_INST, BatmTransOutBatmDpInstRootMessage.class,
                Described.of("rrn=%s, status=%s".formatted(rrn, NEW),
                        m -> rrn.equals(m.getRrn()) && NEW.equals(m.getStatus())),
                Duration.ofSeconds(long_timeout * 3));
    }

    private BatmTransOutBatmDpInstRootMessage awaitInstructionEvent(String instructionId, String status, long timeoutInSecs) {
        return KafkaHelper.await(BATM_TRANS_OUT_BATM_DP_INST, BatmTransOutBatmDpInstRootMessage.class,
                Described.of("instructionId=%s, status=%s".formatted(instructionId, status),
                        m -> instructionId.equals(m.getInstructionId()) && status.equals(m.getStatus())),
                Duration.ofSeconds(timeoutInSecs));
    }

    private BatmTransOutBatmDptransRootMessage awaitTransactionNew(String instructionId) {
        return KafkaHelper.await(BATM_TRANS_OUT_BATM_DP_TRANS, BatmTransOutBatmDptransRootMessage.class,
                Described.of("instructionId=%s, status=%s".formatted(instructionId, NEW),
                        m -> instructionId.equals(m.getInstructionId()) && NEW.equals(m.getStatus())),
                Duration.ofSeconds(long_timeout));
    }

    private BatmTransOutBatmDptransRootMessage awaitTransactionEvent(String transactionId, String status, long timeoutInSecs) {
        return KafkaHelper.await(BATM_TRANS_OUT_BATM_DP_TRANS, BatmTransOutBatmDptransRootMessage.class,
                Described.of("transactionId=%s, status=%s".formatted(transactionId, status),
                        m -> transactionId.equals(m.getTransactionId()) && status.equals(m.getStatus())),
                Duration.ofSeconds(timeoutInSecs));
    }

    /* ******************************
     ********* Kafka UASP/ODS *******
     ********************************/

    /**
     * Сообщение ФИ для транзакционного мониторинга ищется по atmId и статусу.
     */
    private void checkUaspInstructionMessage(String atmId, String status) {
        awaitUaspMessage(UASP_RETAIL_ADM_ATM_CASH_MONEY_TRANSFER_INSTRUCTION,
                fields("/Object/CashMachine/Id/Id", atmId, "/Object/Status", status));
    }

    /**
     * В сообщении ФТ для транзакционного мониторинга нет atmId, поэтому сначала по atmId находится сообщение ФИ,
     * а сообщение ФТ ищется по его идентификатору (Object.Instruction.Id.Id ФТ = Object.Id.Id ФИ) и статусу.
     */
    private void checkUaspTransactionMessage(String atmId, String status) {
        JsonNode uaspInstruction = awaitUaspMessage(UASP_RETAIL_ADM_ATM_CASH_MONEY_TRANSFER_INSTRUCTION,
                fields("/Object/CashMachine/Id/Id", atmId));
        String uaspInstructionId = uaspInstruction.at("/Object/Id/Id").asText();
        awaitUaspMessage(UASP_RETAIL_ADM_ATM_CASH_MONEY_TRANSFER_TRANSACTION,
                fields("/Object/Instruction/Id/Id", uaspInstructionId, "/Object/Status", status));
    }

    private JsonNode awaitUaspMessage(KafkaTopic topic, Map<String, String> expectedFields) {
        String message = KafkaHelper.awaitRaw(topic, jsonFieldsEqual(expectedFields), Duration.ofSeconds(long_timeout));
        try {
            return MAPPER.readTree(message);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Не удалось разобрать сообщение из топика '%s': %s".formatted(topic.getTopicName(), message), e);
        }
    }

    /**
     * Пока критерий поиска в ODS не определён, шаг падает с явной ошибкой (см. {@link #odsOutboxMessageCondition}).
     */
    private void checkOdsOutboxMessage(String entityId) {
        KafkaHelper.awaitRaw(ODS_BATMADMIN_OUTBOX, odsOutboxMessageCondition(entityId), Duration.ofSeconds(long_timeout));
    }

    /**
     * TODO заглушка: критерий поиска сообщения по инструкции/транзакции в топике ODS tsss.batmadmin.outbox не определён
     */
    private Predicate<String> odsOutboxMessageCondition(String entityId) {
        throw new UnsupportedOperationException(
                "Не определён критерий поиска сообщения по '%s' в топике ODS tsss.batmadmin.outbox (заглушка)".formatted(entityId));
    }

    /* ******************************
     ********** batm_outbox *********
     ********************************/

    /**
     * Опрашивает запись в batm_outbox, пока она не появится (expectedStatus == null) или не перейдёт в expectedStatus.
     *
     * @return все различные состояния записи, увиденные за время ожидания; последнее — искомое
     */
    private List<Map<String, Object>> awaitOutboxRecord(String table, Supplier<List<Map<String, Object>>> rowsSupplier, String expectedStatus) {
        Log.step("Ожидаем запись в %s%s".formatted(table, expectedStatus == null ? "" : " со status = " + expectedStatus));
        List<Map<String, Object>> history = new ArrayList<>();
        long timeoutInSecs = DB_EXECUTED.equals(expectedStatus) ? long_timeout * 3 : long_timeout;
        try {
            Awaitility.await().pollInSameThread()
                    .timeout(Duration.ofSeconds(timeoutInSecs))
                    .pollInterval(Duration.ofMillis(DB_POLL_INTERVAL_MILLIS))
                    .until(() -> {
                        List<Map<String, Object>> rows = rowsSupplier.get();
                        if (rows.isEmpty()) {
                            return false;
                        }
                        Map<String, Object> row = rows.get(0);
                        if (history.isEmpty() || !last(history).equals(row)) {
                            history.add(row);
                        }
                        return expectedStatus == null || expectedStatus.equals(String.valueOf(row.get("status")));
                    });
        } catch (ConditionTimeoutException e) {
            Assert.fail("Не дождались записи в %s%s за %s с. Наблюдаемые состояния записи: %s".formatted(
                    table, expectedStatus == null ? "" : " со status = " + expectedStatus, timeoutInSecs, history));
        }
        return history;
    }

    /**
     * Проверяет, что updated_at и updated_dt изменились относительно последнего увиденного состояния записи
     * со статусом, отличным от expectedStatus.
     */
    private void softAssertUpdated(List<Map<String, Object>> history, String expectedStatus, String table) {
        Map<String, Object> current = last(history);
        Map<String, Object> previous = null;
        for (Map<String, Object> row : history) {
            if (!expectedStatus.equals(String.valueOf(row.get("status")))) {
                previous = row;
            }
        }
        if (previous == null) {
            softAssertTrue(false, ("Не удалось зафиксировать запись в %s до перехода в status = %s: " +
                    "проверка обновления 'updated_at' и 'updated_dt' невозможна").formatted(table, expectedStatus));
            return;
        }
        for (String column : List.of("updated_at", "updated_dt")) {
            softAssertFalse(Objects.equals(current.get(column), previous.get(column)),
                    "Поле '%s' в %s не обновилось при переходе %s -> %s (значение: %s)".formatted(
                            column, table, previous.get("status"), expectedStatus, current.get(column)));
        }
    }

    /**
     * Шаги 3-4: запись в msg_content_0/msg_content_1 по ключу и запись в msg_sent с заполненным полем sent
     */
    private void checkOutboxMessagesSent(String key) {
        List<String> messageIds = DBHelper.BatmDP.getOutboxMessageIdsByKey(key);
        softAssertFalse(messageIds.isEmpty(),
                "В batm_outbox.msg_content_0/msg_content_1 не найдено записей с key = '%s'".formatted(key));
        if (messageIds.isEmpty()) {
            return;
        }
        List<Map<String, Object>> sentRows = DBHelper.BatmDP.getOutboxMessagesSent(messageIds);
        Set<String> sentIds = sentRows.stream()
                .filter(r -> r.get("sent") != null)
                .map(r -> String.valueOf(r.get("message_id")))
                .collect(Collectors.toSet());
        for (String messageId : messageIds) {
            softAssertTrue(sentIds.contains(messageId),
                    "В msg_sent нет записи с заполненным полем 'sent' для message_id = '%s'".formatted(messageId));
        }
    }

    /* ******************************
     ************ Утилиты ***********
     ********************************/

    private static <T> T last(List<T> list) {
        return list.get(list.size() - 1);
    }
}
