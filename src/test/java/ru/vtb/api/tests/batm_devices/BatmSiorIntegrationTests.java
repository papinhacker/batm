package ru.vtb.api.tests.batm_devices;

import io.qameta.allure.TmsLink;
import org.apache.commons.lang3.RandomStringUtils;
import org.testng.annotations.Test;
import ru.vtb.api.state.BaseAPIState;
import ru.vtb.core.models.kafka.batm_acc_in_batm_devices_acc.AccountsItem;
import ru.vtb.core.models.kafka.batm_acc_in_batm_devices_acc.BatmAccInBatmDevicesAccRootMessage;
import ru.vtb.core.models.kafka.batm_clearing_in_batm_devices_atm.BatmClearingInBatmDevicesAtmRootMessage;
import ru.vtb.core.models.kafka.batm_clearing_in_batm_devices_atm.generators.BatmClearingInBatmDevicesAtmRootMessageGenerator;
import ru.vtb.core.models.kafka.batm_dp_in_batm_devices_atm.BatmDpInBatmDevicesAtmRootMessage;
import ru.vtb.core.models.kafka.batm_dp_in_batm_devices_atm.generators.BatmDpInBatmDevicesAtmRootMessageGenerator;
import ru.vtb.core.models.kafka.batm_in_siorus_devices.BatmInSiorusDevicesRootMessage;
import ru.vtb.core.models.kafka.batm_in_siorus_devices.Body;
import ru.vtb.core.models.kafka.batm_in_siorus_devices.body.AccountItem;
import ru.vtb.core.models.kafka.batm_in_siorus_devices.body.Accounts;
import ru.vtb.core.models.kafka.batm_in_siorus_devices.generators.BatmInSiorusDevicesMessageGenerator;
import ru.vtb.core.utils.AccountsHelper;
import ru.vtb.core.utils.CommonHelper;
import ru.vtb.core.utils.DBHelper;
import ru.vtb.core.utils.KafkaHelper;
import ru.vtb.data_providers.BatmDevicesDP;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static ru.vtb.core.data.static_data.KafkaTopic.*;
import static ru.vtb.core.data.static_data.MapsKeysConstants.*;
import static ru.vtb.core.models.kafka.batm_in_siorus_devices.generators.BatmInSiorusDevicesMessageGenerator.generateSIORHeader;
import static ru.vtb.core.utils.CommonHelper.serializeToJsonStringIgnoringNullableFields;

public class BatmSiorIntegrationTests extends BaseAPIState {

    @Test(description = "Проверка чтения/записи топиков кафки сервисом batm_integr_sior")
    @TmsLink("BATM-197")
    public void testBATM197() {
        String id = RandomStringUtils.insecure().nextNumeric(8);
        var incomingMessage = BatmInSiorusDevicesMessageGenerator.generate(Map.of(ID, id));
        KafkaHelper.sendMessage(BATM_IN_SIORUS_DEVICES, incomingMessage, false);
        var body = incomingMessage.getBody();
        body.setAccounts(null);

        softAssertEquals(BatmInSiorusDevicesMessageGenerator.generateBodyFromDBSource(DBHelper.Devices.getDeviceDataByDeviceId(id, long_timeout).get(0)),
                body,
                "В БД не создалась запись полученного договора");

        var kafkaResponse = (BatmClearingInBatmDevicesAtmRootMessage) KafkaHelper.getLastRecord(BATM_CLEARING_IN_BATM_DEVICES_ATM, id, timeout);

        softAssertEquals(
                kafkaResponse.getAtmId(),
                id,
                "В топик помещено сообщение с неверным ИД устройства");
        softAssertEquals(
                kafkaResponse.getAtmExploitationStatus(),
                "operating",
                "В топик помещено сообщение с неверным статусом устройства");
        softAssertNotNull(kafkaResponse.getLocationType(),
                "В топик помещено сообщение без инициализированного поля 'locationType'");

        var dpMsg = (BatmDpInBatmDevicesAtmRootMessage) KafkaHelper.getLastRecord(BATM_DP_IN_BATM_DEVICES_ATM,
                id,
                timeout);
        dpMsg.setUpdateTime(null);
        softAssertEquals(dpMsg, BatmDpInBatmDevicesAtmRootMessageGenerator.generate(incomingMessage),
                "В топик 'batm_dp-in-batm_devices-atm' не пришло сообщение");
        softAssertAll();
    }

    @Test(description = "Проверка чтения/записи топиков кафки сервисом batm_devices негативный сценарий")
    @TmsLink("BATM-275")
    public void testBATM275() {
        String id = RandomStringUtils.insecure().nextNumeric(8);
        BatmInSiorusDevicesRootMessage pojo = new BatmInSiorusDevicesRootMessage(Body.builder()
                .id(id)
                .atmExploitationStatus("operating")
                .build(), generateSIORHeader());

        String incomingMessage = serializeToJsonStringIgnoringNullableFields(pojo);
        KafkaHelper.sendMessage(BATM_IN_SIORUS_DEVICES, incomingMessage);

        softAssertTrue(DBHelper.Devices.getDeviceDataByDeviceId(id, short_timeout).isEmpty(),
                "В БД создалась запись полученного договора");
    }

    @Test(description = "Фиксация входящих сообщений по АТМ/АДМ в статусе <>operation/new/decommissioned/utilized/withdrawn",
            dataProvider = "BatmDevicesWrongAtmExplorationStatuses", dataProviderClass = BatmDevicesDP.class)
    @TmsLink("BATM-292")
    public void testBATM292(String atmExploitationStatus) {
        String id = RandomStringUtils.insecure().nextNumeric(8);
        String incomingMessage = serializeToJsonStringIgnoringNullableFields(
                BatmInSiorusDevicesMessageGenerator.generate(Map.of(
                        ID, id,
                        ATM_EXPLOITATION_STATUS, atmExploitationStatus)));
        KafkaHelper.sendMessage(BATM_IN_SIORUS_DEVICES, incomingMessage);
        softAssertFalse(DBHelper.Devices.getDeviceHistoryDataByDeviceID(id).isEmpty(),
                "Запись в БД batm_devices должжна присутствовать");

        softAssertFalse(DBHelper.Devices.getDeviceHistoryDataByDeviceID(id).isEmpty(),
                "Запись в таблице 'devices_history_action' должжна присутствовать");

        softAssertFalse(DBHelper.Devices.getDeviceDataByDeviceId(id).isEmpty(),
                "Запись в таблице 'devices' должжна присутствовать");

        softAssertFalse(Boolean.parseBoolean(DBHelper.Devices.getDeviceAccountsDataByDeviceID(id).get("is_in_acc").toString()),
                "Поле 'is_in_acc' в таблице 'devices_accounts' должжна быть false");
        softAssertAll();
    }

    @Test(description = "Проверка создания УС в филиале Крым 3779")
    @TmsLink("BATM-310")
    public void testBATM310() {
        String id = RandomStringUtils.insecure().nextNumeric(8);
        String incomingMessage = serializeToJsonStringIgnoringNullableFields(
                BatmInSiorusDevicesMessageGenerator.generate(Map.of(
                        ID, id,
                        ABS_BASE_BRANCH, "3779",
                        ABS_BRANCH, "0000")));
        KafkaHelper.sendMessage(BATM_IN_SIORUS_DEVICES, incomingMessage);

        var deviceData = DBHelper.Devices.getDeviceDataByDeviceId(id, long_timeout).get(0);
        softAssertEquals(deviceData.get("number").toString().split("-")[1], "0000",
                "Поле 'number' должно содеоржать abs branch в первых четырёх цифрах");
        softAssertEquals(deviceData.get("abs_base_branch"), "3779",
                "Поле 'abs_base_branch' неверно записалось в бд");
        softAssertEquals(deviceData.get("abs_branch_main"), "00000",
                "Поле 'abs_branch_main' неверно записалось в бд");
        softAssertEquals(deviceData.get("location_type"), "Y",
                "Поле 'location_type' неверно записалось в бд");

        var kafkaResponse = (BatmClearingInBatmDevicesAtmRootMessage) KafkaHelper.getLastRecord(
                BATM_CLEARING_IN_BATM_DEVICES_ATM,
                id,
                timeout);

        softAssertEquals(
                kafkaResponse,
                BatmClearingInBatmDevicesAtmRootMessageGenerator.generate(id),
                "В топик было помещено неверное сообщение.");
        softAssertAll();
    }

    @Test(description = "Проверка создания УС 511 проекта")
    @TmsLink("BATM-311")
    public void testBATM311() {
        String id = RandomStringUtils.insecure().nextNumeric(8);
        var absBaseBranches = DBHelper.Devices.getAbsBaseBranchCodeReference(true);
        String absBaseBranch = CommonHelper.getRandomFromList(absBaseBranches);
        String incomingMessage = serializeToJsonStringIgnoringNullableFields(
                BatmInSiorusDevicesMessageGenerator.generate(Map.of(
                        ID, id,
                        ABS_BASE_BRANCH, absBaseBranch,
                        ABS_BRANCH, "0000")));
        KafkaHelper.sendMessage(BATM_IN_SIORUS_DEVICES, incomingMessage);

        var deviceData = DBHelper.Devices.getDeviceDataByDeviceId(id, long_timeout).get(0);
        softAssertEquals(deviceData.get("abs_base_branch"), absBaseBranch,
                "Поле 'abs_base_branch' неверно записалось в бд");
        softAssertEquals(deviceData.get("abs_branch_main"), "00000",
                "Поле 'abs_branch_main' неверно записалось в бд");
        softAssertEquals(deviceData.get("location_type"), "Y",
                "Поле 'location_type' неверно записалось в бд");

        var kafkaResponse = (BatmClearingInBatmDevicesAtmRootMessage) KafkaHelper.getLastRecord(
                BATM_CLEARING_IN_BATM_DEVICES_ATM,
                id,
                timeout);

        softAssertEquals(
                kafkaResponse,
                BatmClearingInBatmDevicesAtmRootMessageGenerator.generate(id),
                "В топик было помещено неверное сообщение.");
        softAssertAll();
    }

    @Test(description = "Проверка создания УС не 511 проект")
    @TmsLink("BATM-312")
    public void testBATM312() {
        String id = RandomStringUtils.insecure().nextNumeric(8);
        var absBaseBranches = DBHelper.Devices.getAbsBaseBranchCodeReference(false);
        String incomingMessage = serializeToJsonStringIgnoringNullableFields(
                BatmInSiorusDevicesMessageGenerator.generate(Map.of(
                        ID, id,
                        ABS_BASE_BRANCH, CommonHelper.getRandomFromList(absBaseBranches),
                        ABS_BRANCH, "0000")));
        KafkaHelper.sendMessage(BATM_IN_SIORUS_DEVICES, incomingMessage);

        var deviceData = DBHelper.Devices.getDeviceDataByDeviceId(id, long_timeout).get(0);
        softAssertFalse(deviceData.get("abs_base_branch").toString().matches("3779|0000"),
                "Поле 'abs_base_branch' неверно записалось в бд");
        softAssertFalse(deviceData.get("abs_branch_main").toString().matches("0+"),
                "Поле 'abs_branch_main' неверно записалось в бд");
        softAssertEquals(deviceData.get("location_type"), "N",
                "Поле 'location_type' неверно записалось в бд");

        var kafkaResponse = (BatmClearingInBatmDevicesAtmRootMessage) KafkaHelper.getLastRecord(
                BATM_CLEARING_IN_BATM_DEVICES_ATM,
                id,
                timeout);

        softAssertEquals(
                kafkaResponse,
                BatmClearingInBatmDevicesAtmRootMessageGenerator.generate(id, "N"),
                "В топик было помещено неверное сообщение.");
        softAssertAll();
    }

    @Test(description = "Передача данных о счетах устройств из batm_devices в batm_acc")
    @TmsLink("BATM-347")
    public void testBATM347() {
        String id = RandomStringUtils.insecure().nextNumeric(8);
        var msg = BatmInSiorusDevicesMessageGenerator.generate(Map.of(ID, id));
        KafkaHelper.sendMessage(BATM_IN_SIORUS_DEVICES,
                serializeToJsonStringIgnoringNullableFields(msg));

        msg.setHeader(generateSIORHeader());
        KafkaHelper.sendMessage(BATM_IN_SIORUS_DEVICES,
                serializeToJsonStringIgnoringNullableFields(msg));

        softAssertTrue(DBHelper.Devices.getDeviceHistoryDataByDeviceID(id).get("code_status").toString().matches("[017]"),
                "Поле 'code_status' неверно записалось в БД");

        var kafkaResponse = (BatmAccInBatmDevicesAccRootMessage) KafkaHelper.getLastRecord(BATM_ACC_IN_BATM_DEVICES_ACC, id, long_timeout);

        checkToBatmAccMessage(msg, kafkaResponse);
        softAssertAll();
    }

    @Test(description = "Проверка передачи данных по УС из batm_devices в batm_acc при изменении счетов")
    @TmsLink("BATM-348")
    public void testBATM348() {
        String id = RandomStringUtils.insecure().nextNumeric(8);
        var msg = BatmInSiorusDevicesMessageGenerator.generate(Map.of(ID, id));
        KafkaHelper.sendMessage(BATM_IN_SIORUS_DEVICES,
                serializeToJsonStringIgnoringNullableFields(msg));

        msg.setHeader(generateSIORHeader());
        msg.getBody().getAccounts().getAccount().get(0).setNumber(AccountsHelper.generateRandomAccountNumberWithDelimiter("-"));
        DBHelper.Devices.setIsInAccStatus(id, true);
        KafkaHelper.sendMessage(BATM_IN_SIORUS_DEVICES,
                serializeToJsonStringIgnoringNullableFields(msg));

        softAssertTrue(DBHelper.Devices.getDeviceHistoryDataByDeviceID(id).get("code_status").toString().matches("[017]"),
                "Поле 'code_status' неверно записалось в БД");

        var kafkaResponse = (BatmAccInBatmDevicesAccRootMessage) KafkaHelper.getLastRecord(BATM_ACC_IN_BATM_DEVICES_ACC, id, long_timeout);

        checkToBatmAccMessage(msg, kafkaResponse);
        softAssertAll();
    }

    @Test(description = "Проверка передачи данных по УС из batm_devices в batm_acc при добавлении счетов")
    @TmsLink("BATM-418")
    public void testBATM418() {
        String id = RandomStringUtils.insecure().nextNumeric(8);
        var msg = BatmInSiorusDevicesMessageGenerator.generate(Map.of(
                ID, id,
                ACCOUNTS, new Accounts(new ArrayList<>(List.of(
                        AccountItem.builder()
                                .currency("978")
                                .number(AccountsHelper.generateRandomAccountNumberWithDelimiter("-", "978"))
                                .build(),
                        AccountItem.builder()
                                .currency("840")
                                .number(AccountsHelper.generateRandomAccountNumberWithDelimiter("-", "840"))
                                .build()
                )))));
        KafkaHelper.sendMessage(BATM_IN_SIORUS_DEVICES,
                serializeToJsonStringIgnoringNullableFields(msg));

        msg.setHeader(generateSIORHeader());
        var accs = msg.getBody().getAccounts().getAccount();
        accs.add(
                AccountItem.builder()
                        .currency("643")
                        .number(AccountsHelper.generateRandomAccountNumberWithDelimiter("-"))
                        .build()
        );
        DBHelper.Devices.setIsInAccStatus(id, true);
        KafkaHelper.sendMessage(BATM_IN_SIORUS_DEVICES,
                serializeToJsonStringIgnoringNullableFields(msg));

        softAssertTrue(DBHelper.Devices.getDeviceHistoryDataByDeviceID(id).get("code_status").toString().matches("[017]"),
                "Поле 'code_status' неверно записалось в БД");

        var kafkaResponse = (BatmAccInBatmDevicesAccRootMessage) KafkaHelper.getLastRecord(BATM_ACC_IN_BATM_DEVICES_ACC, id, long_timeout);
        accs.remove(0);
        accs.remove(0);

        checkToBatmAccMessage(msg, kafkaResponse);
        softAssertAll();
    }

    @Test(description = "Проверка перемещения АТМ/АДМ")
    @TmsLink("BATM-434")
    public void testBATM434() {
        String id = RandomStringUtils.insecure().nextNumeric(8);
        var msg = BatmInSiorusDevicesMessageGenerator.generate(Map.of(
                ID, id));
        KafkaHelper.sendMessage(BATM_IN_SIORUS_DEVICES,
                serializeToJsonStringIgnoringNullableFields(msg));
        var deviceData = DBHelper.Devices.getDeviceDataByDeviceId(id);
        var deviceAccsData = DBHelper.Devices.getDeviceAccountsDataByDeviceID(id);

        msg.setHeader(generateSIORHeader());
        msg.getBody().getAccounts().getAccount().get(0).setNumber(AccountsHelper.generateRandomAccountNumberWithDelimiter("-"));
        msg.getBody().setAbsBaseBranch(BatmInSiorusDevicesMessageGenerator.getRandomAbsBaseBranchNumber(msg.getBody().getAbsBaseBranch()));
        msg.getBody().setAddress("QA Automation address");
        KafkaHelper.sendMessage(BATM_IN_SIORUS_DEVICES,
                serializeToJsonStringIgnoringNullableFields(msg));

        softAssertNotEquals(
                DBHelper.Devices.getDeviceDataByDeviceId(id),
                deviceData,
                "Запись в таблице 'devices' должна была обновиться");
        softAssertNotEquals(
                DBHelper.Devices.getDeviceAccountsDataByDeviceID(id),
                deviceAccsData,
                "Запись в таблице 'devices_accounts' должна была обновиться");
        softAssertAll();
    }

    @Test(description = "Проверка создания АТМ/АДМ")
    @TmsLink("BATM-435")
    public void testBATM435() {
        String id = RandomStringUtils.insecure().nextNumeric(8);
        var msg = BatmInSiorusDevicesMessageGenerator.generate(Map.of(
                ID, id));
        KafkaHelper.sendMessage(BATM_IN_SIORUS_DEVICES,
                serializeToJsonStringIgnoringNullableFields(msg));

        msg.setHeader(generateSIORHeader());
        KafkaHelper.sendMessage(BATM_IN_SIORUS_DEVICES,
                serializeToJsonStringIgnoringNullableFields(msg));

        var kafkaResponse = (BatmAccInBatmDevicesAccRootMessage) KafkaHelper.getLastRecord(BATM_ACC_IN_BATM_DEVICES_ACC, id, long_timeout);

        checkToBatmAccMessage(msg, kafkaResponse);
        softAssertAll();
    }

    @Test(description = "Замена счетов в договоре")
    @TmsLink("BATM-436")
    public void testBATM436() {
        String id = RandomStringUtils.insecure().nextNumeric(8);
        var msg = BatmInSiorusDevicesMessageGenerator.generate(Map.of(
                ID, id));
        KafkaHelper.sendMessage(BATM_IN_SIORUS_DEVICES,
                serializeToJsonStringIgnoringNullableFields(msg));
        var deviceAccsData = DBHelper.Devices.getDeviceAccountsDataByDeviceID(id);

        msg.setHeader(generateSIORHeader());
        msg.getBody().getAccounts().getAccount().get(0).setNumber(AccountsHelper.generateRandomAccountNumberWithDelimiter("-"));
        KafkaHelper.sendMessage(BATM_IN_SIORUS_DEVICES,
                serializeToJsonStringIgnoringNullableFields(msg));

        softAssertNotEquals(
                DBHelper.Devices.getDeviceAccountsDataByDeviceID(id),
                deviceAccsData,
                "Запись в таблице 'devices_accounts' должна была обновиться");
        softAssertAll();
    }

    private void checkToBatmAccMessage(BatmInSiorusDevicesRootMessage siorMessage, BatmAccInBatmDevicesAccRootMessage actualToBatmAccMsg) {
        var body = siorMessage.getBody();
        List<AccountsItem> accs = new ArrayList<>();
        for (var oldAcc : body.getAccounts().getAccount()) {
            accs.add(AccountsItem.builder()
                    .currency(oldAcc.getCurrency())
                    .number(oldAcc.getNumber().replaceAll("-", ""))
                    .build());
        }
        var exp = BatmAccInBatmDevicesAccRootMessage.builder()
                .atmStatus("operating")
                .atmId(body.getId())
                .absBaseBranch(DBHelper.Devices.getAbsBranchMainByAbsBaseBranch(body.getAbsBaseBranch()))
                .accounts(accs)
                .atmType("atm")
                .build();
        actualToBatmAccMsg.setMessageId(null);

        softAssertEquals(
                actualToBatmAccMsg.toJson(),
                exp.toJson(),
                "В batm acc было передано неверное сообщение");
    }
}
