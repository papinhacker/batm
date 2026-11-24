package ru.vtb.api.tests.batm_acc_route_s3;

import io.qameta.allure.TmsLink;
import io.restassured.path.json.JsonPath;
import org.testng.Assert;
import org.testng.annotations.Ignore;
import org.testng.annotations.Test;
import ru.vtb.api.state.BaseAPIState;
import ru.vtb.core.common.Log;
import ru.vtb.core.models.common.CommonModelHelper;
import ru.vtb.core.models.kafka.batm_acc_route_s3_in_batm_trans_agr_data.TransactionsItem;
import ru.vtb.core.models.kafka.batm_acc_route_s3_in_batm_trans_agr_data.generator.BatmAccRouteS3InBatmTransAgrDataRootMessageGenerator;
import ru.vtb.core.models.kafka.batm_acc_route_s3_out_batm_trans_agr_result.BatmAccRouteS3OutBatmTransAgrResultRootMessage;
import ru.vtb.core.models.kafka.batm_zod_in_batm_acc_route_s3_load_result.BatmZodInBatmAccRouteS3LoadResultRootMessage;
import ru.vtb.core.models.kafka.batm_zod_out_batm_acc_route_s3_event.generators.BatmZodOutBatmAccRouteS3EventRootMessageGenerator;
import ru.vtb.core.utils.CSVUtils;
import ru.vtb.core.utils.CommonHelper;
import ru.vtb.core.utils.DBHelper;
import ru.vtb.core.utils.KafkaHelper;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

import static ru.vtb.core.data.static_data.KafkaTopic.*;
import static ru.vtb.core.data.static_data.MapsKeysConstants.*;
import static ru.vtb.core.data.static_data.S3Constants.AGGREGATION;
import static ru.vtb.core.data.static_data.S3Constants.IF_BATM_AGR_OUT;

public class BatmAccRouteS3Tests extends BaseAPIState {

    private static void validateAggregatorCSVFile(List<String[]> csvData) {
        List<String> errors = new ArrayList<>();
        String errRef = "Значение номер %d в строке %d неверно";
        String emptyErrRef = "Значение номер %d в строке %d должно быть пустым";

        String[] header = csvData.get(0);
        if (!header[0].equals("#HDR")) errors.add(errRef.formatted(1, 1));
        if (!header[5].matches("\\d+/\\d+/\\d+")) errors.add(errRef.formatted(6, 1));
        if (!header[6].matches("[1|2]")) errors.add(errRef.formatted(7, 1));
        if (!header[7].equals("Way4")) errors.add(errRef.formatted(8, 1));
        for (int i = 1; i < 5; i++) {
            if (!header[i].isEmpty()) errors.add(emptyErrRef.formatted(i + 1, 1));
        }

        int midLnNum = csvData.size() / 2;
        String[] body = csvData.get(midLnNum);
        if (!body[0].equals("#DTL")) errors.add(errRef.formatted(1, midLnNum + 1));
        if (!body[1].matches("^W4_VTB_[\\D]{3}$")) errors.add(errRef.formatted(2, midLnNum + 1));
        if (!body[2].matches("^WAY4_\\D_[\\d]{18}$")) errors.add(errRef.formatted(3, midLnNum + 1));
        if (!body[3].matches("^[\\d]{5}$")) errors.add(errRef.formatted(4, midLnNum + 1));
        if (!body[4].equals("N")) errors.add(errRef.formatted(5, midLnNum + 1));
        if (!body[5].matches("^[\\d]{20}$")) errors.add(errRef.formatted(6, midLnNum + 1));
        String currency = body[6];
        if (!currency.matches("^[A-Z]{3}$")) errors.add(errRef.formatted(7, midLnNum + 1));
        String amount = body[7];
        if (!amount.matches("^\\d+.[\\d]{2}$")) errors.add(errRef.formatted(8, midLnNum + 1));
        if (!body[11].equals(currency)) errors.add(errRef.formatted(12, midLnNum + 1));
        if (!body[12].equals(amount)) errors.add(errRef.formatted(13, midLnNum + 1));
        if (!body[14].equals("A")) errors.add(errRef.formatted(15, midLnNum + 1));
        if (!body[48].matches("^\\D1AT[a-z]{2}$")) errors.add(errRef.formatted(49, midLnNum + 1));
        for (int i = 8; i < 11; i++) {
            if (!body[i].isEmpty()) errors.add(emptyErrRef.formatted(i + 1, midLnNum + 1));
        }
        for (int i = 15; i < 48; i++) {
            if (!body[i].isEmpty()) errors.add(emptyErrRef.formatted(i + 1, midLnNum + 1));
        }

        String[] footer = csvData.get(csvData.size() - 1);
        int lastLnNum = csvData.size();
        if (!footer[0].equals("#FTR")) errors.add(errRef.formatted(1, lastLnNum));
        if (!footer[1].matches("\\d+")) errors.add(errRef.formatted(2, lastLnNum));

        if (errors.size() > 0) {
            errors.forEach(Log::error);
            Assert.fail("CSV файл для агрегатора не прошёл валидацию");
        }
    }

    @Test(description = "Отправка сообщения содержащую транзакцию")
    @TmsLink("BATM-303")
    public void testBATM303() {
        var msg = BatmAccRouteS3InBatmTransAgrDataRootMessageGenerator.generate();
        String incomingMessage = CommonHelper.serializeToJsonStringIgnoringNullableFields(msg);
        KafkaHelper.sendMessage(BATM_ACC_ROUTE_S3_IN_BATM_TRANS_AGR_DATA, incomingMessage);

        var data = DBHelper.BatmAccRouteS3.getAccRouteS3TransactionFileDataByMessageId(msg.getMessageId());
        softAssertEquals(data.get("status"), "new", "В бд неверно записалось поле 'status'");
        softAssertEquals(data.get("income_message"), incomingMessage, "Поле 'income_message' должно содержать json принятого сообщения");

        softAssertAll();
    }

    @Test(description = "Успешный разбор сообщения")
    @TmsLink("BATM-304")
    public void testBATM304() {
        var msg = BatmAccRouteS3InBatmTransAgrDataRootMessageGenerator.generate(
                ThreadLocalRandom.current().nextInt(2, 50)
        );
        String incomingMessage = CommonHelper.serializeToJsonStringIgnoringNullableFields(msg);
        KafkaHelper.sendMessage(BATM_ACC_ROUTE_S3_IN_BATM_TRANS_AGR_DATA, incomingMessage);

        String originalMessageId = msg.getMessageId();
        String id = DBHelper.BatmAccRouteS3.getAccRouteS3TransactionFileDataByMessageId(originalMessageId).get("id").toString();
        List<TransactionsItem> actualBodies = new ArrayList<>();
        for (var map : DBHelper.BatmAccRouteS3.getAccRouteS3TransactionsDataByTransactionFileId(id)) {
            actualBodies.add(TransactionsItem.builder()
                    .glCode(map.get(GL_CODE).toString())
                    .aggregationId(map.get("aggregation_id").toString())
                    .atmId(map.get("atm_id").toString())
                    .streamType((int) map.get("stream_type"))
                    .account(map.get("account_number").toString())
                    .operationCodeType(map.get("cbo").toString())
                    .operationDay(map.get("operation_date").toString())
                    .branchCode(map.get("branch_code").toString())
                    .currency(map.get("currency").toString())
                    .amount(Double.parseDouble(map.get("amount").toString()))
                    .build());
        }
        List<TransactionsItem> expectedBodies = msg.getTransactions();
        Collections.sort(expectedBodies);
        Collections.sort(actualBodies);

        softAssertEquals(actualBodies, expectedBodies,
                "Записи о транзакциях в бд несоответсвуют отправленым транзакциям");

        softAssertEquals(
                DBHelper.BatmAccRouteS3.getAccRouteS3TransactionFileDataByMessageId(originalMessageId).get("status"),
                "completed",
                "В бд неверно записалось поле 'status'");

        var kafkaResponse = (BatmAccRouteS3OutBatmTransAgrResultRootMessage) KafkaHelper.getLastRecord(BATM_ACC_ROUTE_S3_OUT_BATM_TRANS_AGR_RESULT, originalMessageId, long_timeout);
        softAssertEquals(
                kafkaResponse.getStatus(),
                "SUCCESS",
                "В json пришёл неверный статус код");

        softAssertAll();
    }

    @Test(description = "Неуспешный разбор сообщения")
    @TmsLink("BATM-305")
    public void testBATM305() {
        var msg = BatmAccRouteS3InBatmTransAgrDataRootMessageGenerator.generate(
                ThreadLocalRandom.current().nextInt(2, 10)
        );
        var body = msg.getTransactions();
        body.get(ThreadLocalRandom.current().nextInt(body.size())).setOperationCodeType(null);
        String incomingMessage = CommonHelper.serializeToJsonStringIgnoringNullableFields(msg);
        KafkaHelper.sendMessage(BATM_ACC_ROUTE_S3_IN_BATM_TRANS_AGR_DATA, incomingMessage);

        String originalMessageId = msg.getMessageId();

        var kafkaResponse = (BatmAccRouteS3OutBatmTransAgrResultRootMessage) KafkaHelper.getLastRecord(BATM_ACC_ROUTE_S3_OUT_BATM_TRANS_AGR_RESULT, originalMessageId, orcTimeout);
        softAssertEquals(
                kafkaResponse.getStatus(),
                "FAILURE",
                "В json пришёл неверный статус код");
        var transFilesData = DBHelper.BatmAccRouteS3.getAccRouteS3TransactionFileDataByMessageId(originalMessageId);
        softAssertEquals(transFilesData.get("status"),
                "error",
                "В бд неверно записалось поле 'status'");
        softAssertTrue(DBHelper.BatmAccRouteS3.getAccRouteS3TransactionsDataByTransactionFileId(transFilesData.get("id").toString()).isEmpty(),
                "В БД не должно быть записей о транзакциях");

        softAssertAll();
    }

    @Test(description = "Формирование и отправка файла в агрегатор с уникальным file_name")
    @TmsLink("BATM-306")
    public void testBATM306() {
        int steamType = BatmAccRouteS3InBatmTransAgrDataRootMessageGenerator.getRandomStreamType();
        String branchCode = DBHelper.Devices.getRandomFullAbsBranchCode();

        var accRouteS3msg = BatmAccRouteS3InBatmTransAgrDataRootMessageGenerator.generate(
                ThreadLocalRandom.current().nextInt(2, 10), Map.of(
                        ABS_BRANCH, branchCode,
                        STREAM_TYPE, steamType));
        KafkaHelper.sendMessage(BATM_ACC_ROUTE_S3_IN_BATM_TRANS_AGR_DATA, CommonHelper.serializeToJsonStringIgnoringNullableFields(accRouteS3msg));

        String originalMessageId = accRouteS3msg.getMessageId();
        var kafkaResponse = (BatmAccRouteS3OutBatmTransAgrResultRootMessage) KafkaHelper.getLastRecord(BATM_ACC_ROUTE_S3_OUT_BATM_TRANS_AGR_RESULT, originalMessageId, orcTimeout);
        softAssertEquals(
                kafkaResponse.getStatus(),
                "SUCCESS",
                "В json пришёл неверный статус код");

        String csvFilename = BatmZodOutBatmAccRouteS3EventRootMessageGenerator.generateRandomAggregatorCSVFilename(Map.of(
                STREAM_TYPE, Integer.toString(steamType),
                ABS_BRANCH, branchCode));
        var zodMsg = BatmZodOutBatmAccRouteS3EventRootMessageGenerator.generate(Map.of(
                CSV_FILENAME, csvFilename,
                STREAM_TYPE, Integer.toString(steamType),
                ABS_BRANCH, branchCode
        ));
        KafkaHelper.sendMessage(BATM_ZOD_OUT_BATM_ACC_ROUTE_S3_EVENT, CommonHelper.serializeToJsonStringIgnoringNullableFields(zodMsg));

        //csv file assertions
        File csvFile = s3Service.getObject(IF_BATM_AGR_OUT, AGGREGATION + csvFilename, long_timeout);
        List<String[]> csvList = CSVUtils.parse(csvFile);
        validateAggregatorCSVFile(csvList);

        List<TransactionsItem> actualBodies = getBodiesFromCSVFile(csvList);
        List<TransactionsItem> expectedBodies = accRouteS3msg.getTransactions();
        expectedBodies.forEach(t -> t.setAtmId(null));
        Collections.sort(expectedBodies);
        Collections.sort(actualBodies);

        //check bodies
        softAssertEquals(actualBodies, expectedBodies,
                "Записи о транзакциях в CSV файла для агрегатора несоответсвуют отправленым транзакциям");

        //check footer
        softAssertEquals(csvList.get(csvList.size() - 1)[1], Integer.toString(csvList.size() - 2),
                "Проверяющее число в футере файла должно быть равно количеству строк трнзакций");

        var kafkaResponse2 = (BatmZodInBatmAccRouteS3LoadResultRootMessage) KafkaHelper.getLastRecord(BATM_ZOD_IN_BATM_ACC_ROUTE_S3_LOAD_RESULT, zodMsg.getMessageId(), orcTimeout);
        softAssertEquals(
                kafkaResponse2.getStatus(),
                "SUCCESS",
                "В json пришёл неверный статус код");

        softAssertAll();
    }

    @Test(description = "Формирование и отправка файла в агрегатор с повторяющимся file_name")
    @TmsLink("BATM-307")
    public void testBATM307() {
        int steamType = BatmAccRouteS3InBatmTransAgrDataRootMessageGenerator.getRandomStreamType();
        String branchCode = DBHelper.Devices.getRandomFullAbsBranchCode();
        var accRouteS3msg = BatmAccRouteS3InBatmTransAgrDataRootMessageGenerator.generate(
                ThreadLocalRandom.current().nextInt(2, 10), Map.of(
                        ABS_BRANCH, branchCode,
                        STREAM_TYPE, steamType));
        KafkaHelper.sendMessage(BATM_ACC_ROUTE_S3_IN_BATM_TRANS_AGR_DATA, CommonHelper.serializeToJsonStringIgnoringNullableFields(accRouteS3msg));

        String originalMessageId = accRouteS3msg.getMessageId();

        String kafkaResponse = (String) KafkaHelper.getLastRecord(BATM_ACC_ROUTE_S3_OUT_BATM_TRANS_AGR_RESULT, originalMessageId, long_timeout * 3);
        softAssertEquals(
                CommonHelper.deserializeJsonToObject(kafkaResponse, BatmAccRouteS3OutBatmTransAgrResultRootMessage.class).getStatus(),
                "SUCCESS",
                "В json пришёл неверный статус код");

        String csvFilename = BatmZodOutBatmAccRouteS3EventRootMessageGenerator.generateRandomAggregatorCSVFilename(Map.of(
                STREAM_TYPE, Integer.toString(steamType),
                ABS_BRANCH, branchCode));
        var zodMsg = BatmZodOutBatmAccRouteS3EventRootMessageGenerator.generate(Map.of(
                CSV_FILENAME, csvFilename,
                STREAM_TYPE, Integer.toString(steamType),
                ABS_BRANCH, branchCode
        ));
        KafkaHelper.sendMessage(BATM_ZOD_OUT_BATM_ACC_ROUTE_S3_EVENT, CommonHelper.serializeToJsonStringIgnoringNullableFields(zodMsg));

        File csvFile = s3Service.getObject(IF_BATM_AGR_OUT, AGGREGATION + csvFilename, long_timeout);
        List<TransactionsItem> firstBodies = getBodiesFromCSVFile(CSVUtils.parse(csvFile));

        s3Service.deleteObject(IF_BATM_AGR_OUT, AGGREGATION + csvFilename);
        zodMsg.setMessageId(CommonModelHelper.generateRandomMessageId());
        KafkaHelper.sendMessage(BATM_ZOD_OUT_BATM_ACC_ROUTE_S3_EVENT, CommonHelper.serializeToJsonStringIgnoringNullableFields(zodMsg));

        csvFile = s3Service.getObject(IF_BATM_AGR_OUT, AGGREGATION + csvFilename, long_timeout);
        List<TransactionsItem> secondBodies = getBodiesFromCSVFile(CSVUtils.parse(csvFile));

        softAssertEquals(firstBodies, secondBodies,
                "Второй файл должен содержать те же транзакции, что и первый");

        var kafkaResponse2 = (BatmZodInBatmAccRouteS3LoadResultRootMessage) KafkaHelper.getLastRecord(BATM_ZOD_IN_BATM_ACC_ROUTE_S3_LOAD_RESULT, zodMsg.getMessageId(), orcTimeout);
        softAssertEquals(
                kafkaResponse2.getStatus(),
                "SUCCESS",
                "В json пришёл неверный статус код");

        softAssertAll();
    }

    private List<TransactionsItem> getBodiesFromCSVFile(List<String[]> csvList) {
        String csvOprDate = CommonHelper.parseDateFromPatternToAnotherPattern("dd/MM/yyyy", "yyyy-MM-dd", csvList.get(0)[5]);
        int csvStreamType = Integer.parseInt(csvList.get(0)[6]);

        List<TransactionsItem> result = new ArrayList<>();
        for (int i = 1; i < csvList.size() - 1; i++) {
            String[] arr = csvList.get(i);
            result.add(TransactionsItem.builder()
                    .glCode(arr[48])
                    .aggregationId(arr[2])
//                            .atmId()
                    .streamType(csvStreamType)
                    .account(arr[5])
                    .operationCodeType(arr[1])
                    .operationDay(csvOprDate)
                    .branchCode(arr[3])
                    .currency(arr[6])
                    .amount(Double.parseDouble(arr[7]))
                    .build());
        }
        return result;
    }

    @Test(description = "performance test")
    @Ignore
    public void testPerformance() {
        int steamType = 2;
        String branchCode = "00040";
        List<String> messageIds = new ArrayList<>();
        List<TransactionsItem> expectedBodies = new ArrayList<>();
        for (int i = 0; i < 150; i++) {
            var accRouteS3msg = BatmAccRouteS3InBatmTransAgrDataRootMessageGenerator.generate(
                    2000, Map.of(
                            ABS_BRANCH, branchCode,
                            STREAM_TYPE, steamType));
            messageIds.add(accRouteS3msg.getMessageId());
            expectedBodies.addAll(accRouteS3msg.getTransactions());
            KafkaHelper.sendMessage(BATM_ACC_ROUTE_S3_IN_BATM_TRANS_AGR_DATA, CommonHelper.serializeToJsonStringIgnoringNullableFields(accRouteS3msg));
        }

        for (String originalMessageId : messageIds) {
            String kafkaResponse = (String) KafkaHelper.getLastRecord(BATM_ACC_ROUTE_S3_OUT_BATM_TRANS_AGR_RESULT, originalMessageId, long_timeout * 7);
            softAssertEquals(JsonPath.from(kafkaResponse).getString("body.status"), "SUCCESS",
                    "В json пришёл неверный статус код");
        }

        String csvFilename = BatmZodOutBatmAccRouteS3EventRootMessageGenerator.generateRandomAggregatorCSVFilename(Map.of(
                STREAM_TYPE, Integer.toString(steamType),
                ABS_BRANCH, branchCode));
        var zodMsg = BatmZodOutBatmAccRouteS3EventRootMessageGenerator.generate(Map.of(
                CSV_FILENAME, csvFilename,
                STREAM_TYPE, Integer.toString(steamType),
                ABS_BRANCH, branchCode
        ));
        KafkaHelper.sendMessage(BATM_ZOD_OUT_BATM_ACC_ROUTE_S3_EVENT, CommonHelper.serializeToJsonStringIgnoringNullableFields(zodMsg));

        //csv file assertions
        File csvFile = s3Service.getObject(IF_BATM_AGR_OUT, AGGREGATION + csvFilename, long_timeout * 10);
        List<String[]> csvList = CSVUtils.parse(csvFile);
        validateAggregatorCSVFile(csvList);

        //check footer
        int size = csvList.size() - 2;
        softAssertEquals(csvList.get(csvList.size() - 1)[1], Integer.toString(size),
                "Проверяющее число в футере файла должно быть равно количеству строк трнзакций");

        softAssertEquals(csvList.get(csvList.size() - 1)[1], Integer.toString(expectedBodies.size()),
                "Кол-во транзакций в файле должно быть равно кол-ву отправ транзакций");

        var kafkaResponse = (BatmAccRouteS3OutBatmTransAgrResultRootMessage) KafkaHelper.getLastRecord(BATM_ACC_ROUTE_S3_OUT_BATM_TRANS_AGR_RESULT, zodMsg.getMessageId(), long_timeout * 3);
        softAssertEquals(
                kafkaResponse.getStatus(),
                "SUCCESS",
                "В json пришёл неверный статус код");

        softAssertAll();
    }
}
