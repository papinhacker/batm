package ru.vtb.api.tests.batm_dp;

import io.qameta.allure.TmsLink;
import io.restassured.path.json.JsonPath;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;
import ru.vtb.api.state.BaseAPIState;
import ru.vtb.core.common.Log;
import ru.vtb.core.data.static_data.dto.TransactionDto;
import ru.vtb.core.utils.B2LHelper;
import ru.vtb.core.utils.DBHelper;
import ru.vtb.core.utils.KafkaHelper;
import ru.vtb.core.utils.ZipUtils;
import ru.vtb.data_providers.BatmDpDP;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static ru.vtb.core.data.static_data.B2LMask.CORP_038cto;
import static ru.vtb.core.data.static_data.B2LMask._298cto;
import static ru.vtb.core.data.static_data.KafkaTopic.BATM_TRANS_OUT_BATM_DP_STREAMING;
import static ru.vtb.core.data.static_data.MapsKeysConstants.*;
import static ru.vtb.core.data.static_data.MapsValuesConstants.*;
import static ru.vtb.core.data.static_data.S3Constants.BATM_OUTPUT;
import static ru.vtb.core.data.static_data.S3Constants.IF_BATM_WAY_IN;
import static ru.vtb.core.utils.B2LHelper.getBundleOfWithdrawalAndWithdrawalReversedTransactions;

/**
 * Сьюта batm dp сущестсвует в виде одного большого параметризированного теста. Коммит 'batm dp tests refactoring' от 11.08.2025
 */
public class BatmDpTests extends BaseAPIState {

    @BeforeMethod
    public void before() {
        setAllureInfo("BATM DP");
    }

    @Test(description = "Проверка формирования стримминга: Снятие по корп-картам прямая транзакция")
    @TmsLink("BATM-298")
    public void testBATM298() {
        String b2lFilename = B2LHelper.generateBase2LikeFileName(CORP_038cto);

        TransactionDto transaction = B2LHelper.getNewWithdrawTransactionReference(Map.of(TERMINAL_TYPE, CORP_TO_JSON_M_T));

        File b2lFIle = B2LHelper.generateBase2LikeFile(b2lFilename,
                transaction);
        s3Service.putObject(IF_BATM_WAY_IN, BATM_OUTPUT, ZipUtils.compressToZip(b2lFIle));

        String rrn = transaction.getRetrievalReferenceNumber();
        String accNum = DBHelper.getDebitOrCreditAccountsNumber(
                DBHelper.getAgreementDataByAccessorNumber(transaction.getTerminalID().trim(), long_timeout).get(0).get("id").toString());

        String record = (String) KafkaHelper.getLastRecord(BATM_TRANS_OUT_BATM_DP_STREAMING, List.of("A1AT", rrn), long_timeout * 3);
        String transactionId = JsonPath.from(record).getString("body.transactionId");

        Map<String, Object> params = Map.of(
                OPERATION_TYPE, CORP_CARD,
                OPERATION_TYPE_DESCRIPTION, WITHDRAWAL,
                PURPOSE, "A1ATbc/ Операции выдачи по банковским корпоративным картам ВТБ (ПАО) в банкоматах ВТБ (ПАО)",
                INNER_TYPE_CODE, "WI.004",
                DEBIT_ACCOUNT, "30232810700000037701");
        var data = DBHelper.BatmDP.getOperationDetails(transactionId);
        softAssertEquals(data.get(DEBIT_BRANCH).toString(), "00000", "Поле 'debit_branch' было неверно записано в БД");
        assertCommonFields(data, params, accNum, false);
    }

    @Test(description = "Проверка формирования стримминга: Снятие по корп-картам транзакция отмены")
    @TmsLink("BATM-299")
    public void testBATM299() {
        String b2lFilename = B2LHelper.generateBase2LikeFileName(CORP_038cto);

        List<TransactionDto> transactions = getBundleOfWithdrawalAndWithdrawalReversedTransactions(
                Map.of(TERMINAL_TYPE, CORP_TO_JSON_M_T));
        var currTrans = transactions.get(1);

        File b2lFIle = B2LHelper.generateBase2LikeFile(b2lFilename,
                transactions);
        s3Service.putObject(IF_BATM_WAY_IN, BATM_OUTPUT, ZipUtils.compressToZip(b2lFIle));

        String rrn = currTrans.getRetrievalReferenceNumber();
        String accNum = DBHelper.getDebitOrCreditAccountsNumber(
                DBHelper.getAgreementDataByAccessorNumber(currTrans.getTerminalID().trim(), long_timeout).get(0).get("id").toString());

        String record = (String) KafkaHelper.getLastRecord(BATM_TRANS_OUT_BATM_DP_STREAMING, List.of("A1AT", rrn), long_timeout * 3);
        String transactionId = JsonPath.from(record).getString("body.transactionId");

        Map<String, Object> params = Map.of(
                OPERATION_TYPE, CORP_CARD,
                OPERATION_TYPE_DESCRIPTION, WITHDRAWAL_REVERSAL,
                PURPOSE, "a1ATbc/ Отмена операции выдачи по банковским корпоративным картам ВТБ (ПАО) в банкоматах ВТБ (ПАО)",
                INNER_TYPE_CODE, "CAWI.004",
                CREDIT_ACCOUNT, "30232810700000037701");
        var data = DBHelper.BatmDP.getOperationDetails(transactionId);
        softAssertEquals(data.get(CREDIT_BRANCH).toString(), "00000", "Поле 'credit_branch' было неверно записано в БД");
        assertCommonFields(data, params, accNum, true);
    }

    @Test(description = "Проверка формирования стримминга: Внесение по корп-картам прямая транзакция_CORP_CARD")
    @TmsLink("BATM-400")
    public void testBATM400() {
        String b2lFilename = B2LHelper.generateBase2LikeFileName(CORP_038cto);

        TransactionDto transaction = B2LHelper.getNewCashInTransactionReference(Map.of(TERMINAL_TYPE, CORP_TO_JSON_M_T));

        File b2lFIle = B2LHelper.generateBase2LikeFile(b2lFilename,
                transaction);
        s3Service.putObject(IF_BATM_WAY_IN, BATM_OUTPUT, ZipUtils.compressToZip(b2lFIle));

        String rrn = transaction.getRetrievalReferenceNumber();
        String accNum = DBHelper.getDebitOrCreditAccountsNumber(
                DBHelper.getAgreementDataByAccessorNumber(transaction.getTerminalID().trim(), long_timeout).get(0).get("id").toString());

        String record = (String) KafkaHelper.getLastRecord(BATM_TRANS_OUT_BATM_DP_STREAMING, List.of("A1AT", rrn), long_timeout * 3);
        String transactionId = JsonPath.from(record).getString("body.transactionId");

        Map<String, Object> params = Map.of(
                OPERATION_TYPE, CORP_CARD,
                OPERATION_TYPE_DESCRIPTION, CASH_IN,
                PURPOSE, "Y1ATbc/ Операции внесения по банковским корпоративным картам ВТБ (ПАО) в банкоматах ВТБ (ПАО)",
                INNER_TYPE_CODE, "DE.004",
                CREDIT_ACCOUNT, "30233810100000047701");
        var data = DBHelper.BatmDP.getOperationDetails(transactionId);
        softAssertEquals(data.get(CREDIT_BRANCH).toString(), "00000", "Поле 'credit_branch' было неверно записано в БД");
        assertCommonFields(data, params, accNum, true);
    }

    @Test(description = "Проверка формирования стримминга: Снятие прямая транзакция",
            dataProvider = "BatmDpBATM300", dataProviderClass = BatmDpDP.class)
    @TmsLink("BATM-300")
    public void testBATM300(Map<String, Object> params) {
        String b2lFilename = B2LHelper.generateBase2LikeFileName(_298cto);

        TransactionDto transaction = B2LHelper.getNewWithdrawTransactionReference(Map.of(
                ACCOUNT_NUMBER, (String) params.get(BIN),
                TERMINAL_TYPE, FL_TO_JSON_M_T));

        File b2lFIle = B2LHelper.generateBase2LikeFile(b2lFilename,
                transaction);
        s3Service.putObject(IF_BATM_WAY_IN, BATM_OUTPUT, b2lFIle);

        String rrn = transaction.getRetrievalReferenceNumber();
        String accNum = DBHelper.getDebitOrCreditAccountsNumber(
                DBHelper.getAgreementDataByAccessorNumber(transaction.getTerminalID().trim(), long_timeout).get(0).get("id").toString());

        String record = (String) KafkaHelper.getLastRecord(BATM_TRANS_OUT_BATM_DP_STREAMING, List.of("A1AT", rrn), long_timeout * 3);
        String transactionId = JsonPath.from(record).getString("body.transactionId");

        var data = DBHelper.BatmDP.getOperationDetails(transactionId);
        softAssertNull(data.get(DEBIT_BRANCH), "Поле 'debit_branch' должно отсутствовать в БД");
        assertCommonFields(data, params, accNum, false);
    }

    @Test(description = "Проверка формирования стримминга: Снятие транзакция отмены",
            dataProvider = "BatmDpBATM301", dataProviderClass = BatmDpDP.class)
    @TmsLink("BATM-301")
    public void testBATM301(Map<String, Object> params) {
        String b2lFilename = B2LHelper.generateBase2LikeFileName(_298cto);

        List<TransactionDto> transactions = getBundleOfWithdrawalAndWithdrawalReversedTransactions(Map.of(
                TERMINAL_TYPE, FL_TO_JSON_M_T,
                ACCOUNT_NUMBER, (String) params.get(BIN)));
        var currTrans = transactions.get(1);

        File b2lFIle = B2LHelper.generateBase2LikeFile(b2lFilename,
                transactions);
        s3Service.putObject(IF_BATM_WAY_IN, BATM_OUTPUT, b2lFIle);

        String rrn = currTrans.getRetrievalReferenceNumber();
        String accNum = DBHelper.getDebitOrCreditAccountsNumber(
                DBHelper.getAgreementDataByAccessorNumber(currTrans.getTerminalID().trim(), long_timeout).get(0).get("id").toString());

        String record = (String) KafkaHelper.getLastRecord(BATM_TRANS_OUT_BATM_DP_STREAMING, List.of("A1AT", rrn), long_timeout * 3);
        String transactionId = JsonPath.from(record).getString("body.transactionId");

        var data = DBHelper.BatmDP.getOperationDetails(transactionId);
        softAssertNull(data.get(CREDIT_BRANCH), "Поле 'credit_branch' должно отсутствовать в БД");
        assertCommonFields(data, params, accNum, true);
    }

    @Test(description = "Проверка формирования стримминга: Внесение прямая транзакция",
            dataProvider = "BatmDpBATM302", dataProviderClass = BatmDpDP.class)
    @TmsLink("BATM-302")
    public void testBATM302(Map<String, Object> params) {
        String b2lFilename = B2LHelper.generateBase2LikeFileName(_298cto);

        TransactionDto transaction = B2LHelper.getNewCashInTransactionReference(Map.of(
                ACCOUNT_NUMBER, (String) params.get(BIN),
                TERMINAL_TYPE, FL_TO_JSON_M_T));

        File b2lFIle = B2LHelper.generateBase2LikeFile(b2lFilename,
                transaction);
        s3Service.putObject(IF_BATM_WAY_IN, BATM_OUTPUT, b2lFIle);

        String rrn = transaction.getRetrievalReferenceNumber();
        String accNum = DBHelper.getDebitOrCreditAccountsNumber(
                DBHelper.getAgreementDataByAccessorNumber(transaction.getTerminalID().trim(), long_timeout).get(0).get("id").toString());

        String record = (String) KafkaHelper.getLastRecord(BATM_TRANS_OUT_BATM_DP_STREAMING, List.of("A1AT", rrn), long_timeout * 3);
        String transactionId = JsonPath.from(record).getString("body.transactionId");

        var data = DBHelper.BatmDP.getOperationDetails(transactionId);
        softAssertNull(data.get(CREDIT_ACCOUNT), "Поле 'credit_account' должно отсутствовать в БД");
        assertCommonFields(data, params, accNum, true);
    }

    private void assertCommonFields(Map<String, Object> actual, Map<String, Object> expected, String accountNumber, boolean reversalTransaction) {
        softAssertEquals(actual.get(OPERATION_TYPE), expected.get(OPERATION_TYPE), "Поле 'operation_type' было неверно записано в БД");
        softAssertEquals(actual.get(OPERATION_TYPE_DESCRIPTION), expected.get(OPERATION_TYPE_DESCRIPTION), "Поле 'operation_type_description' было неверно записано в БД");
        softAssertEquals(actual.get(PURPOSE), expected.get(PURPOSE), "Поле 'purpose' было неверно записано в БД");
        String innerTypeCode = actual.get(INNER_TYPE_CODE).toString();
        softAssertEquals(innerTypeCode, expected.get(INNER_TYPE_CODE).toString(), "Поле 'inner_type_code' было неверно записано в БД");
        switch (innerTypeCode) {
            case "WI.004" -> {
                softAssertEquals(actual.get(VKUS_CODE), "2213.0000.0003", "Поле 'vkus_code' было неверно записано в БД");
                softAssertEquals(actual.get(TBI_CODE), "2213.0003", "Поле 'tbi_code' было неверно записано в БД");
                softAssertEquals(actual.get(DIRECTION), "Decrease", "Поле 'direction' было неверно записано в БД");
            }
            case "CAWI.004" -> {
                softAssertEquals(actual.get(VKUS_CODE), "2213.0000.0004", "Поле 'vkus_code' было неверно записано в БД");
                softAssertEquals(actual.get(TBI_CODE), "2213.0004", "Поле 'tbi_code' было неверно записано в БД");
                softAssertEquals(actual.get(DIRECTION), "Increase", "Поле 'direction' было неверно записано в БД");
            }
            case "DE.004" -> {
                softAssertEquals(actual.get(VKUS_CODE), "2213.0000.0005", "Поле 'vkus_code' было неверно записано в БД");
                softAssertEquals(actual.get(TBI_CODE), "2213.0005", "Поле 'tbi_code' было неверно записано в БД");
                softAssertEquals(actual.get(DIRECTION), "Increase", "Поле 'direction' было неверно записано в БД");
            }
        }
        if (reversalTransaction) {
            softAssertEquals(actual.get(DEBIT_ACCOUNT), accountNumber, "Поле 'debit_account' было неверно записано в БД");
            softAssertEquals(actual.get(CREDIT_ACCOUNT), expected.get(CREDIT_ACCOUNT), "Поле 'credit_account' было неверно записано в БД");
        } else {
            softAssertEquals(actual.get(DEBIT_ACCOUNT), expected.get(DEBIT_ACCOUNT), "Поле 'debit_account' было неверно записано в БД");
            softAssertEquals(actual.get(CREDIT_ACCOUNT), accountNumber, "Поле 'credit_account' было неверно записано в БД");
        }
        //step 3
        var data = DBHelper.BatmDP.getOutboxMessagesData(actual.get(INSTRUCTION_ID).toString(), actual.get(TRANSACTION_ID).toString());
        List<String> messageIds = new ArrayList<>();
        for (var map : data) {
            softAssertEquals(map.get("key").toString(), actual.get("account_document_id").toString(), "Поле 'key' было неверно записано в БД");
            messageIds.add(map.get("message_id").toString());
        }
        //step 4
        var msgSentData = DBHelper.BatmDP.getOutboxMessagesSentData(messageIds);
        softAssertEquals(msgSentData.stream().distinct().count(), 1L, "Значение 'kafka_partition' для всех записей должны быть одинаковые");
        softAssertAll();
    }

    @Test()
    public void testBATM406debug() {
        String b2lFilename = B2LHelper.generateBase2LikeFileName(_298cto);

        var transParams = Map.of(TERMINAL_TYPE, FL_TO_JSON_M_F);
        List<TransactionDto> trans = new ArrayList<>();
        trans.add(B2LHelper.getNewCashInTransactionReference(transParams));
        trans.addAll(B2LHelper.getBundleOfWithdrawalAndWithdrawalReversedTransactions(transParams));

        File b2lFIle = B2LHelper.generateBase2LikeFile(b2lFilename,
                trans);
        s3Service.putObject(IF_BATM_WAY_IN, BATM_OUTPUT, b2lFIle);

//        String record = KafkaHelper.getLastRecord(BATM_TRANS_OUT_BATM_DP_STREAMING, List.of("A1AT", rrn), long_timeout * 3);
//        String transactionId = JsonPath.from(record).getString("body.transactionId");

        Log.debug("cashIn rrn " + trans.get(0).getRetrievalReferenceNumber());
        Log.debug("withdraw rrn " + trans.get(1).getRetrievalReferenceNumber());
        Log.debug("withdrawRev rrn " + trans.get(2).getRetrievalReferenceNumber());
    }
}
