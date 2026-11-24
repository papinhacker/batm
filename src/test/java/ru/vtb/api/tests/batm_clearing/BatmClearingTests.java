package ru.vtb.api.tests.batm_clearing;

import io.qameta.allure.TmsLink;
import org.apache.commons.lang3.RandomStringUtils;
import org.testng.Assert;
import org.testng.annotations.Test;
import ru.vtb.api.state.BaseAPIState;
import ru.vtb.core.common.Log;
import ru.vtb.core.data.static_data.dto.TransactionDto;
import ru.vtb.core.models.kafka.batm_clearing_in_batm_devices_atm.BatmClearingInBatmDevicesAtmRootMessage;
import ru.vtb.core.models.kafka.batm_clearing_out_batm_trans_data.BatmClearingOutBatmTransAgrDataRootMessage;
import ru.vtb.core.models.kafka.batm_clearing_out_batm_trans_data.TransactionsItem;
import ru.vtb.core.models.kafka.batm_in_siorus_devices.generators.BatmInSiorusDevicesMessageGenerator;
import ru.vtb.core.utils.*;

import java.io.File;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import static ru.vtb.core.data.static_data.B2LMask.*;
import static ru.vtb.core.data.static_data.KafkaTopic.*;
import static ru.vtb.core.data.static_data.MapsKeysConstants.*;
import static ru.vtb.core.data.static_data.S3Constants.BATM_OUTPUT;
import static ru.vtb.core.data.static_data.S3Constants.IF_BATM_WAY_IN;

public class BatmClearingTests extends BaseAPIState {

    @Test(description = "Проверка чтения топика batm-in-siorus-devices мс batm_clearing")
    @TmsLink("BATM-242")
    public void testBATM242() {
        String id = RandomStringUtils.insecure().nextNumeric(8);
        String incomingMessage = CommonHelper.serializeToJsonStringIgnoringNullableFields(
                BatmInSiorusDevicesMessageGenerator.generate(Map.of(
                        ID, id,
                        FEATURE, "Cash-in",
                        VENDOR, "WINCOR_NEW")));
        KafkaHelper.sendMessage(BATM_IN_SIORUS_DEVICES, incomingMessage);

        var response = (BatmClearingInBatmDevicesAtmRootMessage) KafkaHelper.getLastRecord(
                BATM_CLEARING_IN_BATM_DEVICES_ATM,
                id,
                long_timeout);
        Assert.assertEquals(
                response.toJson(),
                """
                        {"atmId":"%s","atmExploitationStatus":"operating","locationType":"N"}""".formatted(id),
                "В топик не помещено сообщение с данными созданного устройства."
        );

        Map<String, Object> deviceData = DBHelper.Clearing.getDeviceDataByID(id);
        softAssertEquals(deviceData.get("atm_id"), id, "В БД записались неверные данные о банкомате");
        softAssertEquals(deviceData.get("status"), "operating", "В БД записались неверные данные о банкомате");
        softAssertEquals(deviceData.get("active"), false, "В БД записались неверные данные о банкомате");
        softAssertEquals(
                CommonHelper.parseDateFromPatternToAnotherPattern("yyyy-MM-dd HH:mm:ss",
                        "yyyy-MM-dd", deviceData.get("sys_changed_datetime").toString()),
                CommonHelper.getCurrentDateInFormat("yyyy-MM-dd"),
                "В БД записались неверные данные о банкомате");

        incomingMessage = CommonHelper.serializeToJsonStringIgnoringNullableFields(
                BatmInSiorusDevicesMessageGenerator.generate(Map.of(
                        ID, id,
                        FEATURE, "Cash-in",
                        VENDOR, "WINCOR_NEW",
                        ATM_EXPLOITATION_STATUS, "decommissioned")));
        KafkaHelper.sendMessage(BATM_IN_SIORUS_DEVICES, incomingMessage);

        CommonHelper.sleep(3000L);
        deviceData = DBHelper.Clearing.getDeviceDataByID(id);
        softAssertEquals(deviceData.get("atm_id"), id, "В БД не записались обновлённые данные о банкомате");
        softAssertEquals(deviceData.get("status"), "decommissioned", "В БД не записались обновлённые данные о банкомате");
        softAssertEquals(deviceData.get("active"), false, "В БД не записались обновлённые данные о банкомате");
        softAssertEquals(
                CommonHelper.parseDateFromPatternToAnotherPattern("yyyy-MM-dd HH:mm:ss",
                        "yyyy-MM-dd", deviceData.get("sys_changed_datetime").toString()),
                CommonHelper.getCurrentDateInFormat("yyyy-MM-dd"),
                "В БД не записались обновлённые данные о банкомате");

        softAssertAll();
    }

    private void checkUfxJson(String b2lFilename) {
        String myUfx = UfxHelper.generateJsonUfxFileFromBase2Like(b2lFilename);
        var myJson = CommonHelper.deserializeJsonToObject(myUfx, BatmClearingOutBatmTransAgrDataRootMessage.class);
        var clearingJson = (BatmClearingOutBatmTransAgrDataRootMessage) KafkaHelper.getLastRecord(BATM_CLEARING_OUT_BATM_TRANS_AGR_DATA, b2lFilename, long_timeout);

        var clearingTransactions = clearingJson.getTransactions();
        for (var transaction : clearingTransactions) {
            transaction.setId(null);
        }
        softAssertEquals(
                clearingTransactions,
                myJson.getTransactions(),
                "Json ufx bodies are different");
    }

    @Test(description = "Проверка конвертации B2L->JSON")
    @TmsLink("BATM-338")
    public void testBATM338() {
        String b2lFilename = B2LHelper.generateBase2LikeFileName(_298cto);
        File b2lFIle = B2LHelper.generateBase2LikeFile(b2lFilename,
                Map.of(TERMINAL_TYPE, FL_TO_JSON_M_T),
                100);
        s3Service.putObject(IF_BATM_WAY_IN, BATM_OUTPUT, b2lFIle);

        checkUfxJson(b2lFilename);
        softAssertAll();
    }

    @Test(description = "Проверка сохранения передаваемых в JSON транзакций  в бд batm_clearing")
    @TmsLink("BATM-346")
    public void testBATM346() {
        String b2lFilename = B2LHelper.generateBase2LikeFileName(_298cto);
        File clearingB2lFIle = B2LHelper.generateBase2LikeFile(b2lFilename, Map.of(TERMINAL_TYPE, FL_TO_JSON_M_T));
        s3Service.putObject(IF_BATM_WAY_IN, BATM_OUTPUT, clearingB2lFIle);

        String myUfx = UfxHelper.generateJsonUfxFileFromBase2Like(b2lFilename);
        KafkaHelper.getLastRecord(BATM_CLEARING_OUT_BATM_TRANS_AGR_DATA, b2lFilename, long_timeout);

        var myJson = CommonHelper.deserializeJsonToObject(myUfx, BatmClearingOutBatmTransAgrDataRootMessage.class);
        var body = myJson.getTransactions().get(0);
        var clearingData = DBHelper.Clearing.getDataOfClearingTransactions(body.getAuthCode(), body.getRrn());
        softAssertEquals(clearingData.get("trans_date").toString(),
                CommonHelper.parseDateFromPatternToAnotherPattern("yyyy-MM-dd'T'HH:mm:ss", "yyyy-MM-dd HH:mm:ss.S", body.getTransDate()),
                "Поле 'trans_date' неверно записалось в бд клиринга");
        softAssertEquals(clearingData.get("trans_type"), body.getTransType(), "Поле 'trans_type' неверно записалось в бд клиринга");
        softAssertEquals(clearingData.get("bank_product_type"), body.getBankProductType(), "Поле 'bank_product_type' неверно записалось в бд клиринга");
//        softAssertEquals(clearingData.get("arn"), body.getArn(), "Поле 'arn' неверно записалось в бд клиринга");
        softAssertEquals(clearingData.get("name_file"), body.getNameFile(), "Поле 'name_file' неверно записалось в бд клиринга");
        softAssertEquals(clearingData.get("auth_code"), body.getAuthCode(), "Поле 'auth_code' неверно записалось в бд клиринга");
        softAssertEquals(clearingData.get("rrn"), body.getRrn(), "Поле 'rrn' неверно записалось в бд клиринга");
        //todo перделать использования ориг данных с сообщения из кафки на фал клиринга
//        if (body.getTrn() != null)
//            softAssertEquals(clearingData.get("trn"), body.getTrn(), "Поле 'trn' неверно записалось в бд клиринга");
//        softAssertEquals(clearingData.get("sic"), body.getSic(), "Поле 'sic' неверно записалось в бд клиринга");
//        softAssertEquals(clearingData.get("country"), body.getCountry(), "Поле 'country' неверно записалось в бд клиринга");
//        softAssertEquals(clearingData.get("city"), body.getCity(), "Поле 'city' неверно записалось в бд клиринга");
//        softAssertEquals(clearingData.get("merchant_name"), body.getMerchantName(), "Поле 'merchant_name' неверно записалось в бд клиринга");
//        if (body.getMerchantID() != null)
//            softAssertEquals(clearingData.get("merchant_id"), body.getMerchantID(), "Поле 'merchant_id' неверно записалось в бд клиринга");
//        softAssertEquals(clearingData.get("card_number"), body.getCardNumber(), "Поле 'card_number' неверно записалось в бд клиринга");
//        if (body.getCardType() != null)
//            softAssertEquals(clearingData.get("card_type"), body.getCardType(), "Поле 'card_type' неверно записалось в бд клиринга");
//        softAssertEquals(clearingData.get("atm_id"), body.getAtmId(), "Поле 'atm_id' неверно записалось в бд клиринга");
//        softAssertEquals(clearingData.get("card_expiry").toString(), body.getCardExpiry(), "Поле 'card_expiry' неверно записалось в бд клиринга");
        softAssertEquals(clearingData.get("currency"), body.getCurrency(), "Поле 'currency' неверно записалось в бд клиринга");
        softAssertEquals(Double.valueOf(clearingData.get("amount").toString()), body.getAmount(), "Поле 'amount' неверно записалось в бд клиринга");
        softAssertAll();
    }

    @Test(description = "Проверка разделения транзакций на пачк при конвертации из B2L в JSON ")
    @TmsLink("BATM-349")
    public void testBATM349() {
        String b2lFilename = B2LHelper.generateBase2LikeFileName(_298cto);
        double count = 2100;
        File b2lFIle = B2LHelper.generateBase2LikeFile(b2lFilename,
                Map.of(TERMINAL_TYPE, FL_TO_JSON_M_T),
                (int) count);
        s3Service.putObject(IF_BATM_WAY_IN, BATM_OUTPUT, b2lFIle);

        List<TransactionsItem> bodies = new LinkedList<>();
        double buff = count / 1000;
        buff = Math.ceil(buff);
        for (int i = 0; i < buff; i++) {
            var clearingUfx = (BatmClearingOutBatmTransAgrDataRootMessage) KafkaHelper.getLastRecord(BATM_CLEARING_OUT_BATM_TRANS_AGR_DATA, b2lFilename, long_timeout);
            bodies.addAll(clearingUfx.getTransactions());
        }

        softAssertEquals(
                bodies.size(),
                (int) count,
                "Batm clearing обработал не все транзакции в json");
        softAssertAll();
    }

    @Test(description = "Проверка транзакционных ограничений конвертации по MCC в UFX 298cto")
    @TmsLink("BATM-401")
    public void testBATM401() {
        String b2lFilename = B2LHelper.generateBase2LikeFileName(_298cto);
        var trans6011mcc = B2LHelper.getBundleOfWithdrawalAndWithdrawalReversedAndCashInTransactions(Map.of(
                TERMINAL_TYPE, FL_TO_JSON_M_T));
        var transNot6011mcc = B2LHelper.getBundleOfWithdrawalAndWithdrawalReversedAndCashInTransactions(Map.of(
                TERMINAL_TYPE, FL_TO_JSON_M_T,
                MCC, "6012"));
        trans6011mcc.addAll(transNot6011mcc);

        File b2lFIle = B2LHelper.generateBase2LikeFile(b2lFilename,
                trans6011mcc);
        s3Service.putObject(IF_BATM_WAY_IN, BATM_OUTPUT, b2lFIle);

        checkUfxJson(b2lFilename);
        softAssertAll();
    }

    @Test(description = "Проверка транзакционных ограничений конвертации по MCC в JSON CORP_038ctd")
    @TmsLink("BATM-402")
    public void testBATM402() {
        String b2lFilename = B2LHelper.generateBase2LikeFileName(CORP_038ctd);
        var trans6011mcc = B2LHelper.getNewCashInTransactionReference(Map.of(
                TERMINAL_TYPE, CORP_TO_JSON_M_T));
        var trans6012mcc = B2LHelper.getNewCashInTransactionReference(Map.of(
                TERMINAL_TYPE, CORP_TO_JSON_M_T,
                MCC, "6012"));
        var transIncorrect = B2LHelper.getNewCashInTransactionReference(Map.of(
                TERMINAL_TYPE, CORP_TO_JSON_M_T,
                MCC, "9999"));
        var transP2P = B2LHelper.getNewCashInTransactionReference(Map.of(
                TERMINAL_TYPE, CORP_TO_JSON_M_T,
                MCC, "6012")).addP2PAttributes();

        File b2lFIle = B2LHelper.generateBase2LikeFile(b2lFilename,
                List.of(trans6011mcc, trans6012mcc, transIncorrect, transP2P));
        s3Service.putObject(IF_BATM_WAY_IN, BATM_OUTPUT, b2lFIle);

        checkUfxJson(b2lFilename);
        softAssertAll();
    }

    @Test(description = "Проверка транзакционных ограничений конвертации по MCC в JSON OPS_038ctd")
    @TmsLink("BATM-414")
    public void testBATM414() {
        String b2lFilename = B2LHelper.generateBase2LikeFileName(OPS_038ctd);
        var trans6011mcc = B2LHelper.getNewCashInTransactionReference(Map.of(
                TERMINAL_TYPE, FL_TO_JSON_M_T));
        var trans6012mcc = B2LHelper.getNewCashInTransactionReference(Map.of(
                TERMINAL_TYPE, FL_TO_JSON_M_T,
                MCC, "6012"));
        var transIncorrect = B2LHelper.getNewCashInTransactionReference(Map.of(
                TERMINAL_TYPE, FL_TO_JSON_M_T,
                MCC, "9999"));

        File b2lFIle = B2LHelper.generateBase2LikeFile(b2lFilename,
                List.of(trans6011mcc, trans6012mcc, transIncorrect));
        s3Service.putObject(IF_BATM_WAY_IN, BATM_OUTPUT, b2lFIle);

        checkUfxJson(b2lFilename);
        softAssertAll();
    }

    @Test(description = "Проверка конвертации B2L->JSON, atm_migration=true, CORP_038cto")
    @TmsLink("BATM-419")
    public void testBATM419() {
        String b2lFilename = B2LHelper.generateBase2LikeFileName(CORP_038cto);

        File b2lFIle = B2LHelper.generateBase2LikeFile(b2lFilename,
                B2LHelper.getBundleOfWithdrawalAndWithdrawalReversedTransactions(Map.of(
                        TERMINAL_TYPE, CORP_TO_JSON_M_T)));
        s3Service.putObject(IF_BATM_WAY_IN, BATM_OUTPUT, b2lFIle);

        checkUfxJson(b2lFilename);
        softAssertAll();
    }

    @Test(description = "Проверка конвертации B2L->JSON, atm_migration=true, CORP_038ctd")
    @TmsLink("BATM-420")
    public void testBATM420() {
        String b2lFilename = B2LHelper.generateBase2LikeFileName(CORP_038ctd);

        File b2lFIle = B2LHelper.generateBase2LikeFile(b2lFilename,
                B2LHelper.getNewCashInTransactionReference(Map.of(
                        TERMINAL_TYPE, CORP_TO_JSON_M_T)));
        s3Service.putObject(IF_BATM_WAY_IN, BATM_OUTPUT, b2lFIle);

        checkUfxJson(b2lFilename);
        softAssertAll();
    }

    @Test(description = "Проверка конвертации B2L->JSON, atm_migration=false, 298cto")
    @TmsLink("BATM-421")
    public void testBATM421() {
        String b2lFilename = B2LHelper.generateBase2LikeFileName(_298cto);

        File b2lFIle = B2LHelper.generateBase2LikeFile(b2lFilename,
                B2LHelper.getBundleOfWithdrawalAndWithdrawalReversedAndCashInTransactions(Map.of(
                        TERMINAL_TYPE, FL_TO_JSON_M_F,
                        MCC, "6011")));
        s3Service.putObject(IF_BATM_WAY_IN, BATM_OUTPUT, b2lFIle);

        checkUfxJson(b2lFilename);
        softAssertAll();
    }

    @Test(description = "Проверка конвертации B2L->JSON, atm_migration=false, VTB24_038cto")
    @TmsLink("BATM-422")
    public void testBATM422() {
        String b2lFilename = B2LHelper.generateBase2LikeFileName(VTB24_038cto);

        File b2lFIle = B2LHelper.generateBase2LikeFile(b2lFilename,
                B2LHelper.getBundleOfWithdrawalAndWithdrawalReversedAndCashInTransactions(Map.of(
                        TERMINAL_TYPE, FL_TO_JSON_M_F)));
        s3Service.putObject(IF_BATM_WAY_IN, BATM_OUTPUT, b2lFIle);

        softAssertNull(KafkaHelper.getLastRecord(BATM_CLEARING_OUT_BATM_TRANS_AGR_DATA, List.of(b2lFilename), long_timeout, true),
                "В топике должно отсутствовать сообщение об обработке файла клиринга");

        softAssertAll();
    }

    @Test(description = "Проверка конвертации B2L->JSON, atm_migration=true, VTB24_038cto")
    @TmsLink("BATM-423")
    public void testBATM423() {
        String b2lFilename = B2LHelper.generateBase2LikeFileName(VTB24_038cto);

        File b2lFIle = B2LHelper.generateBase2LikeFile(b2lFilename,
                B2LHelper.getBundleOfWithdrawalAndWithdrawalReversedAndCashInTransactions(Map.of(
                        TERMINAL_TYPE, FL_TO_JSON_M_T)));
        s3Service.putObject(IF_BATM_WAY_IN, BATM_OUTPUT, b2lFIle);

        checkUfxJson(b2lFilename);
        softAssertAll();
    }

    @Test(description = "Проверка конвертации B2L->JSON, atm_migration=true, D_038cto")
    @TmsLink("BATM-424")
    public void testBATM424() {
        String b2lFilename = B2LHelper.generateBase2LikeFileName(D_038cto);

        File b2lFIle = B2LHelper.generateBase2LikeFile(b2lFilename,
                B2LHelper.getAllTransactionTypes(Map.of(
                        TERMINAL_TYPE, FL_TO_JSON_M_T)));
        s3Service.putObject(IF_BATM_WAY_IN, BATM_OUTPUT, b2lFIle);

        checkUfxJson(b2lFilename);
        softAssertAll();
    }

    @Test(description = "Проверка конвертации B2L->JSON, atm_migration=false, D_038cto")
    @TmsLink("BATM-425")
    public void testBATM425() {
        String b2lFilename = B2LHelper.generateBase2LikeFileName(D_038cto);

        File b2lFIle = B2LHelper.generateBase2LikeFile(b2lFilename,
                B2LHelper.getAllTransactionTypes(Map.of(
                        TERMINAL_TYPE, FL_TO_JSON_M_F)));
        s3Service.putObject(IF_BATM_WAY_IN, BATM_OUTPUT, b2lFIle);

        softAssertNull(KafkaHelper.getLastRecord(BATM_CLEARING_OUT_BATM_TRANS_AGR_DATA, List.of(b2lFilename), long_timeout, true),
                "В топике должно отсутствовать сообщение об обработке файла клиринга");
        softAssertAll();
    }

    @Test(description = "Проверка конвертации B2L->JSON, atm_migration=false, OPS_038ctd")
    @TmsLink("BATM-426")
    public void testBATM426() {
        String b2lFilename = B2LHelper.generateBase2LikeFileName(OPS_038ctd);

        File b2lFIle = B2LHelper.generateBase2LikeFile(b2lFilename,
                List.of(B2LHelper.getNewCashInTransactionReference(Map.of(
                        TERMINAL_TYPE, FL_TO_JSON_M_F))));
        s3Service.putObject(IF_BATM_WAY_IN, BATM_OUTPUT, b2lFIle);

        softAssertNull(KafkaHelper.getLastRecord(BATM_CLEARING_OUT_BATM_TRANS_AGR_DATA, List.of(b2lFilename), long_timeout, true),
                "В топике должно отсутствовать сообщение об обработке файла клиринга");
        softAssertAll();
    }

    @Test(description = "Проверка конвертации B2L->JSON, atm_migration=true, OPS_038ctd")
    @TmsLink("BATM-427")
    public void testBATM427() {
        String b2lFilename = B2LHelper.generateBase2LikeFileName(OPS_038ctd);

        File b2lFIle = B2LHelper.generateBase2LikeFile(b2lFilename,
                List.of(B2LHelper.getNewCashInTransactionReference(Map.of(
                        TERMINAL_TYPE, FL_TO_JSON_M_T))));
        s3Service.putObject(IF_BATM_WAY_IN, BATM_OUTPUT, b2lFIle);

        checkUfxJson(b2lFilename);
        softAssertAll();
    }

    @Test(description = "Проверка транзакционных ограничений конвертации по MCC в JSON CORP_038cto")
    @TmsLink("BATM-428")
    public void testBATM428() {
        String b2lFilename = B2LHelper.generateBase2LikeFileName(CORP_038cto);
        List<TransactionDto> trans = new ArrayList<>();
        var trans6011mcc = B2LHelper.getBundleOfWithdrawalAndWithdrawalReversedTransactions(Map.of(
                TERMINAL_TYPE, CORP_TO_JSON_M_T));
        var trans6012mcc = B2LHelper.getBundleOfWithdrawalAndWithdrawalReversedTransactions(Map.of(
                TERMINAL_TYPE, CORP_TO_JSON_M_T,
                MCC, "6012"));
        var transIncorrect = B2LHelper.getBundleOfWithdrawalAndWithdrawalReversedTransactions(Map.of(
                TERMINAL_TYPE, CORP_TO_JSON_M_T,
                MCC, "9999"));
        trans.addAll(trans6011mcc);
        trans.addAll(trans6012mcc);
        trans.addAll(transIncorrect);

        File b2lFIle = B2LHelper.generateBase2LikeFile(b2lFilename,
                trans);
        s3Service.putObject(IF_BATM_WAY_IN, BATM_OUTPUT, b2lFIle);

        checkUfxJson(b2lFilename);
        softAssertAll();
    }

    @Test(description = "Проверка транзакционных ограничений конвертации по MCC в JSON VTB24_038cto")
    @TmsLink("BATM-429")
    public void testBATM429() {
        String b2lFilename = B2LHelper.generateBase2LikeFileName(VTB24_038cto);
        List<TransactionDto> trans = new ArrayList<>();
        var trans6011mcc = B2LHelper.getBundleOfWithdrawalAndWithdrawalReversedAndCashInTransactions(Map.of(
                TERMINAL_TYPE, FL_TO_JSON_M_T));
        var trans6012mcc = B2LHelper.getBundleOfWithdrawalAndWithdrawalReversedAndCashInTransactions(Map.of(
                TERMINAL_TYPE, FL_TO_JSON_M_T,
                MCC, "6012"));
        var transIncorrect = B2LHelper.getBundleOfWithdrawalAndWithdrawalReversedAndCashInTransactions(Map.of(
                TERMINAL_TYPE, FL_TO_JSON_M_T,
                MCC, "9999"));
        trans.addAll(trans6011mcc);
        trans.addAll(trans6012mcc);
        trans.addAll(transIncorrect);

        File b2lFIle = B2LHelper.generateBase2LikeFile(b2lFilename,
                trans);
        s3Service.putObject(IF_BATM_WAY_IN, BATM_OUTPUT, b2lFIle);

        checkUfxJson(b2lFilename);
        softAssertAll();
    }

    @Test(description = "Проверка транзакционных ограничений конвертации по MCC в JSON D_038cto")
    @TmsLink("BATM-430")
    public void testBATM430() {
        String b2lFilename = B2LHelper.generateBase2LikeFileName(D_038cto);
        List<TransactionDto> trans = new ArrayList<>();
        var trans6011mcc = B2LHelper.getAllTransactionTypes(Map.of(
                TERMINAL_TYPE, FL_TO_JSON_M_T));
        var trans6012mcc = B2LHelper.getAllTransactionTypes(Map.of(
                TERMINAL_TYPE, FL_TO_JSON_M_T,
                MCC, "6012"));
        var trans6536mcc = B2LHelper.getNewCashInTransactionReference(Map.of(
                TERMINAL_TYPE, FL_TO_JSON_M_T,
                MCC, "6536"));
        var transIncorrect = B2LHelper.getAllTransactionTypes(Map.of(
                TERMINAL_TYPE, FL_TO_JSON_M_T,
                MCC, "9999"));
        trans.addAll(trans6011mcc);
        trans.addAll(trans6012mcc);
        trans.add(trans6536mcc);
        trans.addAll(transIncorrect);

        File b2lFIle = B2LHelper.generateBase2LikeFile(b2lFilename,
                trans);
        s3Service.putObject(IF_BATM_WAY_IN, BATM_OUTPUT, b2lFIle);

        checkUfxJson(b2lFilename);
        softAssertAll();
    }

    @Test(description = "Проверка конвертации B2L->JSON, atm_migration=true, 038cts (Самоинкассация)")
    @TmsLink("BATM-505")
    public void testBATM505() {
        String b2lFilename = B2LHelper.generateBase2LikeFileName(_038cts);
        var trans = B2LHelper.getNewSelfCollectionCashInTransactionReference(Map.of(
                TERMINAL_TYPE, CORP_TO_JSON_M_T));

        File b2lFIle = B2LHelper.generateBase2LikeFile(b2lFilename,
                trans);
        s3Service.putObject(IF_BATM_WAY_IN, BATM_OUTPUT, b2lFIle);

        checkUfxJson(b2lFilename);
        softAssertAll();
    }
}
