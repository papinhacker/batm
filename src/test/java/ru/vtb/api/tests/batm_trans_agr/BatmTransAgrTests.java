package ru.vtb.api.tests.batm_trans_agr;

import io.qameta.allure.TmsLink;
import io.restassured.path.json.JsonPath;
import org.testng.annotations.Test;
import ru.vtb.api.state.BaseAPIState;
import ru.vtb.core.data.static_data.SemiTransaction;
import ru.vtb.core.data.static_data.dto.TransactionDto;
import ru.vtb.core.models.kafka.batm_acc_route_in_batm_trans_pp_data.BatmAccRouteInBatmTransPpDataRootMessage;
import ru.vtb.core.models.kafka.batm_acc_route_in_batm_trans_pp_data.gerators.BatmAccRouteInBatmTransPpDataMessageGenerator;
import ru.vtb.core.models.kafka.batm_acc_route_pp_in_batm_trans_pp_data.BatmAccRoutePpInBatmTransPpDataRootMessage;
import ru.vtb.core.models.kafka.batm_acc_route_pp_in_batm_trans_pp_result.BatmAccRoutePpOutBatmTransPpResultRootMessage;
import ru.vtb.core.models.kafka.batm_acc_route_s3_in_batm_trans_agr_data.BatmAccRouteS3InBatmTransAgrDataRootMessage;
import ru.vtb.core.models.kafka.batm_clearing_out_batm_trans_data.BatmClearingOutBatmTransAgrDataRootMessage;
import ru.vtb.core.models.kafka.batm_clearing_out_batm_trans_data.TransactionsItem;
import ru.vtb.core.models.kafka.batm_report_in_batm_trans_agr_transaction.BatmReportInBatmTransAgrTransactionRootMessage;
import ru.vtb.core.models.kafka.batm_trans_out_batm_dp_inst.BatmTransOutBatmDpInstRootMessage;
import ru.vtb.core.models.kafka.batm_trans_out_batm_dp_trans.BatmTransOutBatmDptransRootMessage;
import ru.vtb.core.utils.*;

import java.io.File;
import java.util.*;
import java.util.concurrent.ThreadLocalRandom;

import static ru.vtb.core.data.static_data.B2LMask.D_038cto;
import static ru.vtb.core.data.static_data.B2LMask._298cto;
import static ru.vtb.core.data.static_data.KafkaTopic.*;
import static ru.vtb.core.data.static_data.MapsKeysConstants.AMOUNT_FEE;
import static ru.vtb.core.data.static_data.MapsKeysConstants.TERMINAL_ID;
import static ru.vtb.core.data.static_data.S3Constants.BATM_OUTPUT;
import static ru.vtb.core.data.static_data.S3Constants.IF_BATM_WAY_IN;

public class BatmTransAgrTests extends BaseAPIState {

    @Test(description = "Проверка расчета суммы с комиссией Cash-In в генератор")
    @TmsLink("BATM-481")
    public void testBATM481() {
        String b2lFilename = B2LHelper.generateBase2LikeFileName(D_038cto);
        var dto = B2LHelper.getNewCashInTransactionReference(Map.of(
                AMOUNT_FEE, String.valueOf(ThreadLocalRandom.current().nextInt(1, 999)),
                TERMINAL_ID, "LA151190"
        ));
        var incomingJson = UfxHelper.generateJSONUfxModelsBasedOnBase2LikeTransactions(List.of(dto), b2lFilename);
        KafkaHelper.sendMessage(BATM_CLEARING_OUT_BATM_TRANS_AGR_DATA, CommonHelper.serializeToJsonStringIgnoringNullableFields(incomingJson));
        double buff = Integer.parseInt(dto.getDestinationAmount()) + Integer.parseInt(dto.getSurchargeAmount());
        String resultAmount = String.format(Locale.US, "%.2f", buff / 100);

        KafkaHelper.getLastRecord(BATM_ACC_ROUTE_IN_BATM_TRANS_PP_DATA, resultAmount, long_timeout);
    }

    @Test(description = "Проверка расчета суммы с комиссией Cash-In в агрегатор")
    @TmsLink("BATM-482")
    public void testBATM482() {
        String b2lFilename = B2LHelper.generateBase2LikeFileName(D_038cto);
        var dto = B2LHelper.getNewCashInTransactionReference(Map.of(
                AMOUNT_FEE, String.valueOf(ThreadLocalRandom.current().nextInt(1, 999))
        ));
        var incomingJson = UfxHelper.generateJSONUfxModelsBasedOnBase2LikeTransactions(List.of(dto), b2lFilename);
        KafkaHelper.sendMessage(BATM_CLEARING_OUT_BATM_TRANS_AGR_DATA, CommonHelper.serializeToJsonStringIgnoringNullableFields(incomingJson));
        double buff = Integer.parseInt(dto.getDestinationAmount()) + Integer.parseInt(dto.getSurchargeAmount());
        String resultAmount = String.format(Locale.US, "%.2f", buff / 100);

        KafkaHelper.getLastRecord(BATM_ACC_ROUTE_IN_BATM_TRANS_S3_DATA, resultAmount, long_timeout);
    }

    @Test(description = "Копия Проверка расчета суммы с комиссией Cash-Withdrawal в генератор")
    @TmsLink("BATM-483")
    public void testBATM483() {
        String b2lFilename = B2LHelper.generateBase2LikeFileName(D_038cto);
        var dto = B2LHelper.getNewWithdrawTransactionReference(Map.of(
                AMOUNT_FEE, String.valueOf(ThreadLocalRandom.current().nextInt(1, 999)),
                TERMINAL_ID, "LA151190"
        ));
        var incomingJson = UfxHelper.generateJSONUfxModelsBasedOnBase2LikeTransactions(List.of(dto), b2lFilename);
        KafkaHelper.sendMessage(BATM_CLEARING_OUT_BATM_TRANS_AGR_DATA, CommonHelper.serializeToJsonStringIgnoringNullableFields(incomingJson));
        double buff = Integer.parseInt(dto.getDestinationAmount()) - Integer.parseInt(dto.getSurchargeAmount());
        String resultAmount = String.format(Locale.US, "%.2f", buff / 100);

        KafkaHelper.getLastRecord(BATM_ACC_ROUTE_IN_BATM_TRANS_PP_DATA, resultAmount, long_timeout);
    }

    @Test(description = "Проверка расчета суммы с комиссией Cash-Withdrawal в агрегатор")
    @TmsLink("BATM-484")
    public void testBATM484() {
        String b2lFilename = B2LHelper.generateBase2LikeFileName(D_038cto);
        var dto = B2LHelper.getNewWithdrawTransactionReference(Map.of(
                AMOUNT_FEE, String.valueOf(ThreadLocalRandom.current().nextInt(1, 999))
        ));
        var incomingJson = UfxHelper.generateJSONUfxModelsBasedOnBase2LikeTransactions(List.of(dto), b2lFilename);
        KafkaHelper.sendMessage(BATM_CLEARING_OUT_BATM_TRANS_AGR_DATA, CommonHelper.serializeToJsonStringIgnoringNullableFields(incomingJson));
        double buff = Integer.parseInt(dto.getDestinationAmount()) - Integer.parseInt(dto.getSurchargeAmount());
        String resultAmount = String.format(Locale.US, "%.2f", buff / 100);

        KafkaHelper.getLastRecord(BATM_ACC_ROUTE_IN_BATM_TRANS_S3_DATA, resultAmount, long_timeout);
    }


    protected void testTransAgrToAggregator(String b2lFilename, List<TransactionDto> trans) {
        var incomingJson = UfxHelper.generateJSONUfxModelsBasedOnBase2LikeTransactions(trans, b2lFilename);
        KafkaHelper.sendMessage(BATM_CLEARING_OUT_BATM_TRANS_AGR_DATA, CommonHelper.serializeToJsonStringIgnoringNullableFields(incomingJson));
        checkClearingData(incomingJson);

        var randomTrans = CommonHelper.getRandomFromList(incomingJson.getTransactions());
        var step3resp = (BatmAccRouteS3InBatmTransAgrDataRootMessage) KafkaHelper.getLastRecord(BATM_ACC_ROUTE_IN_BATM_TRANS_S3_DATA,
                randomTrans.getRrn() + randomTrans.getAuthCode(), long_timeout);

        checkSemiTransactions(incomingJson, step3resp);
        CommonHelper.waitForValueChangeAndGet(() ->
                DBHelper.TransAgr.getTransactionsMetaData(incomingJson.getMessageId()), orcTimeout);

        String respJson = (String) KafkaHelper.getLastRecord(BATM_ACC_ROUTE_OUT_BATM_TRANS_S3_RESULT, step3resp.getMessageId(), long_timeout);
        softAssertEquals(JsonPath.from(respJson).getString("body.status"), "SUCCESS",
                "Сервис BATM-ACC-ROUTE-S3 вернул неверный статус о получении полупроводок");

        softAssertAll();
    }

    protected void testTransAgrToGenerator(String b2lFilename, List<TransactionDto> trans) {
        var incomingJson = UfxHelper.generateJSONUfxModelsBasedOnBase2LikeTransactions(trans, b2lFilename);
        KafkaHelper.sendMessage(BATM_CLEARING_OUT_BATM_TRANS_AGR_DATA, CommonHelper.serializeToJsonStringIgnoringNullableFields(incomingJson));
        checkClearingData(incomingJson);

        List<String> msgIds = new ArrayList<>();
        checkToGeneratorTransactions(incomingJson, msgIds);

        CommonHelper.waitForValueChangeAndGet(() ->
                DBHelper.TransAgr.getTransactionsMetaData(incomingJson.getMessageId()), orcTimeout);

        for (String msgId : msgIds) {
            //todo добааить полноту проверку сообщения где-то в июле 2026
            KafkaHelper.getLastRecord(BATM_ACC_ROUTE_OUT_BATM_TRANS_PP_RESULT, msgId, long_timeout);
        }

        softAssertAll();
    }

    private void checkClearingData(BatmClearingOutBatmTransAgrDataRootMessage expectedOriginal) {
        List<Map<String, Object>> fromDB = DBHelper.TransAgr.getTransactionsData(expectedOriginal.getMessageId());
        List<Map<String, Object>> expected = new ArrayList<>();
        for (TransactionsItem body : expectedOriginal.getTransactions()) {
            Map<String, Object> buff = new LinkedHashMap<>();
            //todo определиться с проверяемыми данными по новым контрактам
            buff.put("trans_date", CommonHelper.parseDateFromPatternToAnotherPattern(
                    "yyyy-MM-dd'T'HH:mm:ss",
                    "yyyy-MM-dd HH:mm:ss.S",
                    body.getTransDate()));
            buff.put("trans_type", body.getTransType());
            buff.put("bank_product_type", body.getBankProductType());
            buff.put("arn", body.getArn());
            buff.put("name_file_clearing", body.getNameFile());
            buff.put("auth_code", body.getAuthCode());
//            buff.put("sic", body.getSic());
            buff.put("rrn", body.getRrn());
//            buff.put("trn", body.getTrn());
//            buff.put("card_number", body.getCardNumber());
            buff.put("atm_id", body.getAtmId());
            buff.put("amount", body.getAmount());
            buff.put("currency", body.getCurrency());
            buff.put("amount_fee", String.format(Locale.US, "%.2f", body.getAmountFee()));
//            buff.put("atm_migration", body.getAtmMigration());
            expected.add(buff);
        }
        Comparator<Map<String, Object>> comp = Comparator.comparing((Map<String, Object> m) -> m.get("rrn").toString()).thenComparing(m -> m.get("trans_type").toString());
        fromDB.sort(comp);
        expected.sort(comp);

        softAssertEquals(fromDB.toString(), expected.toString(), "Данные транзакций неверно записались в таблицу 'clearing_data'");
    }

    private void checkSemiTransactions(BatmClearingOutBatmTransAgrDataRootMessage expected, BatmAccRouteS3InBatmTransAgrDataRootMessage actual) {
        List<SemiTransaction> expectedBuff = SemiTransaction.generateSemiTransactions(expected);
        List<SemiTransaction> actualBuff = SemiTransaction.generateSemiTransactions(actual);

        softAssertEquals(actualBuff, expectedBuff, "Транс агр сформировал неверные полупроводки для агрегатора");
    }

    private void checkToGeneratorTransactions(BatmClearingOutBatmTransAgrDataRootMessage incomingJson, List<String> messages) {
        var expected = BatmAccRouteInBatmTransPpDataMessageGenerator
                .generateBatmAccRouteInBatmTransPpDataMessages(incomingJson);
        List<BatmAccRouteInBatmTransPpDataRootMessage> actual = new ArrayList<>();
        for (var body : incomingJson.getTransactions()) {
            var response = (BatmAccRouteInBatmTransPpDataRootMessage) KafkaHelper.getLastRecord(BATM_ACC_ROUTE_IN_BATM_TRANS_PP_DATA,
                    body.getAmount().toString(), long_timeout);
            messages.add(response.getTransactionId());
            response.setMessageId(null);
            response.setMessageDate(null);
            actual.add(response);
        }
        Collections.sort(actual);
        Collections.sort(expected);
        softAssertEquals(actual, expected, "Транс агр сформировал неверные полупроводки для генератора");
    }


    @Test(description = "Проверка Маршрутизации проводок в ROUTE-PP ")
    @TmsLink("BATM-415")
    public void testBATM415() {
        String device = "LA151190";

        String b2lFilename = B2LHelper.generateBase2LikeFileName(_298cto);
        File b2lFIle = B2LHelper.generateBase2LikeFile(b2lFilename,
                List.of(B2LHelper.getNewWithdrawTransactionReference(Map.of(TERMINAL_ID, device))));
        s3Service.putObject(IF_BATM_WAY_IN, BATM_OUTPUT, b2lFIle);
        var myJson = (BatmClearingOutBatmTransAgrDataRootMessage) KafkaHelper.getLastRecord(BATM_CLEARING_OUT_BATM_TRANS_AGR_DATA, b2lFilename, long_timeout);
        String step1Id = myJson.getTransactions().get(0).getId();

        var step2 = DBHelper.TransAgrSrv.getClearingData(step1Id);
        softAssertTrue(
                Boolean.parseBoolean(step2.get("ms_generator").toString()),
                "err1");

        var step3 = DBHelper.TransAgrSrv.getTransactionGpData(step1Id);
        String step3Id = step3.get("id").toString();

        var step4 = DBHelper.TransAgrSrv.getInstructionGpData(step1Id);

        var step5 = DBHelper.TransAgrSrv.getAccountingGpData(step3Id);

        var step6 = (BatmAccRoutePpInBatmTransPpDataRootMessage) KafkaHelper.getFirstRecord(BATM_ACC_ROUTE_PP_IN_BATM_TRANS_PP_DATA, step3Id, timeout);

        var step7_1 = (BatmTransOutBatmDpInstRootMessage) KafkaHelper.getFirstRecord(BATM_TRANS_OUT_BATM_DP_INST, List.of(step1Id, "New"), timeout);
        String instrId = step7_1.getInstructionId();
        var step7_2 = (BatmTransOutBatmDpInstRootMessage) KafkaHelper.getFirstRecord(BATM_TRANS_OUT_BATM_DP_INST, List.of(instrId, "Accepted"), timeout);
        var step7_3 = (BatmTransOutBatmDpInstRootMessage) KafkaHelper.getFirstRecord(BATM_TRANS_OUT_BATM_DP_INST, List.of(instrId, "Processing"), timeout);

        var step8_1 = (BatmTransOutBatmDptransRootMessage) KafkaHelper.getFirstRecord(BATM_TRANS_OUT_BATM_DP_TRANS, List.of(step3Id, "New"), timeout);
        var step8_2 = (BatmTransOutBatmDptransRootMessage) KafkaHelper.getFirstRecord(BATM_TRANS_OUT_BATM_DP_TRANS, List.of(step3Id, "Accepted"), timeout);

        var step9_1 = (BatmReportInBatmTransAgrTransactionRootMessage) KafkaHelper.getFirstRecord(BATM_REPORT_IN_BATM_TRANS_AGR_TRANSACTION, List.of(step3Id, "New"), timeout);
        var step9_2 = (BatmReportInBatmTransAgrTransactionRootMessage) KafkaHelper.getFirstRecord(BATM_REPORT_IN_BATM_TRANS_AGR_TRANSACTION, List.of(step3Id, "Accepted"), timeout);

        var step10 = (BatmAccRoutePpOutBatmTransPpResultRootMessage) KafkaHelper.getFirstRecord(BATM_ACC_ROUTE_PP_OUT_BATM_TRANS_PP_RESULT, List.of(step3Id, "SUCCESS_PP"), timeout);

        var step11 = (BatmReportInBatmTransAgrTransactionRootMessage) KafkaHelper.getLastRecord(BATM_REPORT_IN_BATM_TRANS_AGR_TRANSACTION, List.of(step3Id, "Executed"), timeout);

        var step12_1 = (BatmTransOutBatmDptransRootMessage) KafkaHelper.getLastRecord(BATM_TRANS_OUT_BATM_DP_TRANS, List.of(step3Id, "Processing"), timeout);
        var step12_2 = (BatmTransOutBatmDptransRootMessage) KafkaHelper.getLastRecord(BATM_TRANS_OUT_BATM_DP_TRANS, List.of(step3Id, "Executed"), timeout);

        var step13 = (BatmTransOutBatmDpInstRootMessage) KafkaHelper.getFirstRecord(BATM_TRANS_OUT_BATM_DP_INST, List.of(instrId, "Executed"), timeout);

        var step14 = (BatmAccRoutePpOutBatmTransPpResultRootMessage) KafkaHelper.getFirstRecord(BATM_ACC_ROUTE_PP_OUT_BATM_TRANS_PP_RESULT, List.of(step3Id, "SUCCESS_GP"), timeout);

        var step15 = (BatmReportInBatmTransAgrTransactionRootMessage) KafkaHelper.getFirstRecord(BATM_REPORT_IN_BATM_TRANS_AGR_TRANSACTION, List.of(step3Id, "Ok"), timeout);
        softAssertAll();
    }
}
