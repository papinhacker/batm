package ru.vtb.api.tests.batm_acc;

import io.qameta.allure.TmsLink;
import org.joda.time.DateTime;
import org.testng.annotations.Ignore;
import org.testng.annotations.Test;
import ru.vtb.api.state.BaseAPIState;
import ru.vtb.core.models.common.CommonModelHelper;
import ru.vtb.core.models.kafka.batm_acc_in_batm_devices_acc.BatmAccInBatmDevicesAccRootMessage;
import ru.vtb.core.models.kafka.batm_acc_in_batm_devices_acc.generators.BatmAccInBatmDevicesAccMessageGenerator;
import ru.vtb.core.models.kafka.batm_acc_in_batm_trans_acc_result.BatmAccInBatmTransAccResultRootMessage;
import ru.vtb.core.models.kafka.batm_acc_in_batm_trans_reversal.BatmAccInBatmTransReversalRootMessage;
import ru.vtb.core.models.kafka.batm_acc_out_batm_trans_acc.BatmAccOutBatmTransAccRootMessage;
import ru.vtb.core.models.kafka.batm_acc_out_batm_trans_acc.generators.BatmAccOutBatmTransAccRootMessageGenerator;
import ru.vtb.core.models.kafka.batm_acc_out_batm_trans_reversal_result.BatmAccOutBatmTransReversalResultRootMessage;
import ru.vtb.core.models.kafka.batm_acc_out_batm_trans_reversal_result.generators.BatmAccOutBatmTransReversalResultRootMessageGenerator;
import ru.vtb.core.models.kafka.batm_in_siorus_acc_migration.BatmInSiorusAccMigrationRootMessage;
import ru.vtb.core.models.kafka.batm_sior_product_out_batm_acc_new.generators.BatmSiorProductOutBatmAccNewRootMessageGenerator;
import ru.vtb.core.utils.AccountsHelper;
import ru.vtb.core.utils.CommonHelper;
import ru.vtb.core.utils.DBHelper;
import ru.vtb.core.utils.KafkaHelper;

import java.text.SimpleDateFormat;
import java.util.Map;
import java.util.UUID;

import static ru.vtb.core.data.static_data.KafkaTopic.*;

public class BatmAccTests extends BaseAPIState {

    @Test(description = "Приём сообщения содержащего счета УС")
    @TmsLink("BATM-330")
    public void testBATM330() {
        var msg = BatmAccInBatmDevicesAccMessageGenerator.generate();
        KafkaHelper.sendMessage(BATM_ACC_IN_BATM_DEVICES_ACC, msg);

        var data = DBHelper.BatmAcc.getAccountDataFromBatmAcc(msg.getAccounts().get(0).getNumber(), msg.getAtmId());
        checkBatmAccAccountData(data, msg);
        softAssertEquals(data.get("status_load"), "NEW",
                "Поле 'status_load' имеет неверное значение в таблице active_accounts");
        softAssertAll();
    }

    @Test(description = "Отгрузка нового счёта потребителю")
    @TmsLink("BATM-334")
    public void testBATM334() {
        var msg = BatmAccInBatmDevicesAccMessageGenerator.generate();
        KafkaHelper.sendMessage(BATM_ACC_IN_BATM_DEVICES_ACC, msg);

        var actual = (BatmAccOutBatmTransAccRootMessage)
                KafkaHelper.getLastRecord(BATM_ACC_OUT_BATM_TRANS_ACC, msg.getAccounts().get(0).getNumber(), orcTimeout);
        actual.setMessageId(null);
        var exp = BatmAccOutBatmTransAccRootMessageGenerator.convertFromBatmAccInBatmDevicesAccRootMessage(msg);
        exp.setMessageId(null);
        softAssertEquals(
                actual,
                exp,
                "В топик 'batm_acc-out-batm_trans-acc' пришло сообщение с невереными данными");

        softAssertAll();
    }

    @Test(description = "Получение статуса отгрузки заведённого счёта")
    @TmsLink("BATM-335")
    public void testBATM335() {
        var msg = BatmAccInBatmDevicesAccMessageGenerator.generate();
        KafkaHelper.sendMessage(BATM_ACC_IN_BATM_DEVICES_ACC, msg);
        var msg2 = BatmAccInBatmTransAccResultRootMessage.builder()
                .originalMessageId(msg.getMessageId())
                .statusLoad("SUCCESS")
                .build();
        KafkaHelper.sendMessage(BATM_ACC_IN_BATM_TRANS_ACC_RESULT, msg2);

        var data = CommonHelper.waitForValueChangeAndGet(
                () -> DBHelper.BatmAcc.getAccountDataFromBatmAcc(msg.getAccounts().get(0).getNumber(), msg.getAtmId()),
                orcTimeout);
        softAssertEquals(data.get("status_load"), "SUCCESS",
                "Поле 'status_load' имеет неверное значение в таблице active_accounts");

        softAssertAll();
    }

    @Test(description = "Поиск счетов для оформления reversal-операций")
    @TmsLink("BATM-336")
    public void testBATM336() {
        var msg = BatmSiorProductOutBatmAccNewRootMessageGenerator.generate();
        KafkaHelper.sendMessage(BATM_SIOR_PRODUCT_OUT_BATM_ACC_NEW, msg);

        var msg2 = BatmAccInBatmTransReversalRootMessage.builder()
                .messageId(CommonModelHelper.generateRandomMessageId())
                .transDate(new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss")
                        .format(new DateTime().plusMinutes(10).toDate()))
                .atmId(msg.getAtmId())
                .currency("643")
                .build();
        KafkaHelper.sendMessage(BATM_ACC_IN_BATM_TRANS_REVERSAL, msg2);

        var act = (BatmAccOutBatmTransReversalResultRootMessage)
                KafkaHelper.getLastRecord(BATM_ACC_OUT_BATM_TRANS_REVERSAL_RESULT, msg.getAtmId(), long_timeout);
        var data = DBHelper.BatmAcc.getAccountDataFromBatmAcc(msg.getAccountNumber(), msg.getAtmId());
        var exp = BatmAccOutBatmTransReversalResultRootMessageGenerator.generateFromDbSource(data, msg2.getMessageId());
        softAssertEquals(
                act,
                exp,
                "В топик пришло сообщение не соответсвующее таблице active_accounts");

        softAssertAll();
    }

    // Этот функционал временно выпилен из сервиса
    @Deprecated
    @Ignore
    @Test(description = "Прием сообщения с измененными счетами УС")
    @TmsLink("BATM-397")
    public void testBATM397() {
        var msg = BatmAccInBatmDevicesAccMessageGenerator.generate();
        KafkaHelper.sendMessage(BATM_ACC_IN_BATM_DEVICES_ACC, msg);

        var msg2 = msg;
        msg2.setMessageId(CommonModelHelper.generateRandomMessageId());
        msg2.getAccounts().get(0).setNumber(AccountsHelper.generateRandomAccountNumber(msg.getAccounts().get(0).getCurrency()));
        KafkaHelper.sendMessage(BATM_ACC_IN_BATM_DEVICES_ACC, msg2);

        checkBatmAccAccountData(DBHelper.BatmAcc.getAccountDataFromBatmAcc(msg2.getAccounts().get(0).getNumber(), msg2.getAtmId()), msg2);
        checkBatmAccAccountData(DBHelper.BatmAcc.getAccountHistoryDataFromBatmAcc(msg.getAccounts().get(0).getNumber(), msg.getAtmId()), msg);
        softAssertAll();
    }

    @Test(description = "Поиск счетов для оформления reversal-операций из history_accounts")
    @TmsLink("BATM-509")
    public void testBATM509() {
        var msg = BatmSiorProductOutBatmAccNewRootMessageGenerator.generate();
        KafkaHelper.sendMessage(BATM_SIOR_PRODUCT_OUT_BATM_ACC_NEW, msg);

        var msg2 = BatmInSiorusAccMigrationRootMessage.builder()
                .absCode("PPKS")
                .atmId(msg.getAtmId())
                .productInstanceId(UUID.randomUUID().toString())
                .productInstanceType("CashMachine")
                .branchCode(DBHelper.TransAgrSrv.getRandomBranchCode(msg.getBranchCode()))
                .productInstanceStatus("Active")
                .registerId(UUID.randomUUID().toString())
                .registerType("20208_USBanka")
                .accountId(UUID.randomUUID().toString())
                .currency("RUB")
                .accountNumber(msg.getAccountNumber())
                .registerStatus("Active")
                .build();
        KafkaHelper.sendMessage(BATM_IN_SIORUS_ACC_MIGRATION, msg2);
        DBHelper.BatmAcc.decreaseHistoryAccountsStartTime(msg2.getAtmId());

        var msg3 = BatmAccInBatmTransReversalRootMessage.builder()
                .messageId(CommonModelHelper.generateRandomMessageId())
                .transDate("2024-07-10T17:35:49")
                .atmId(msg.getAtmId())
                .currency("643")
                .build();
        KafkaHelper.sendMessage(BATM_ACC_IN_BATM_TRANS_REVERSAL, msg3);

        var act = (BatmAccOutBatmTransReversalResultRootMessage) KafkaHelper.getLastRecord(BATM_ACC_OUT_BATM_TRANS_REVERSAL_RESULT, msg.getAccountId(), long_timeout);
        var data = DBHelper.BatmAcc.getAccountHistoryDataFromBatmAcc(msg.getAccountNumber(), msg.getAtmId());
        var exp = BatmAccOutBatmTransReversalResultRootMessageGenerator.generateFromDbSource(data, msg3.getMessageId());
        softAssertEquals(
                act,
                exp,
                "В топик пришло сообщение не соответсвующее таблице history_accounts");

        softAssertAll();
    }

    private void checkBatmAccAccountData(Map<String, Object> databaseData, BatmAccInBatmDevicesAccRootMessage msg) {
        softAssertEquals(databaseData.get("message_id"), msg.getMessageId(),
                "Поле 'message_id' имеет неверное значение в таблице active_accounts");
        softAssertEquals(databaseData.get("atm_id"), msg.getAtmId(),
                "Поле 'atm_id' имеет неверное значение в таблице active_accounts");
        softAssertEquals(databaseData.get("account_number"), msg.getAccounts().get(0).getNumber(),
                "Поле 'account_number' имеет неверное значение в таблице active_accounts");
        softAssertEquals(databaseData.get("currency"), msg.getAccounts().get(0).getCurrency(),
                "Поле 'currency' имеет неверное значение в таблице active_accounts");
        softAssertEquals(databaseData.get("abs_base_branch"), msg.getAbsBaseBranch(),
                "Поле 'abs_base_branch' имеет неверное значение в таблице active_accounts");
    }
}
