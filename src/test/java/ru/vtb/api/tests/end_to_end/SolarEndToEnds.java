package ru.vtb.api.tests.end_to_end;

import io.qameta.allure.TmsLink;
import io.restassured.path.json.JsonPath;
import org.apache.commons.lang3.RandomStringUtils;
import org.testng.annotations.Test;
import ru.vtb.api.common.assertions.Conditions;
import ru.vtb.api.models.batm.generators.SaveAgreementGenerator;
import ru.vtb.api.state.BaseAPIState;
import ru.vtb.core.common.Log;
import ru.vtb.core.utils.CommonHelper;
import ru.vtb.core.utils.DBHelper;

import java.util.Map;

import static ru.vtb.core.common.EnvConstants.FILE_SEPARATOR;
import static ru.vtb.core.data.static_data.MapsKeysConstants.*;

public class SolarEndToEnds extends BaseAPIState {

    @Test(description = "Solar Air full e2e API test")
    @TmsLink("BATM-286")
    @TmsLink("BATM-287")
    @TmsLink("BATM-288")
    @TmsLink("BATM-289")
    @TmsLink("BATM-290")
    @TmsLink("BATM-291")
    public void testSolarE2e() {
        Log.step("Проверка JSON SaveAgreement новый договор");
        String agreementNumber = SaveAgreementGenerator.generateRandomAgreementNumber();
        String contractNumber = RandomStringUtils.insecure().nextNumeric(8);
        String accountNumber = SaveAgreementGenerator.generateRandomAccountNumber();

        String agreementId = agreementService.post(Map.of(
                        AGREEMENT_NUMBER, agreementNumber,
                        CONTRACT_NUMBER, contractNumber,
                        ACCOUNT_NUMBER, accountNumber))
                .should(Conditions.responseTypeIsSuccess())
                .should(Conditions.hasJsonSchema("solarAir" + FILE_SEPARATOR + "saveAgreement" + FILE_SEPARATOR + "createNewAgreementResponse.json"))
                .asString("body.agreement.id");

        //database asserts
        softAssertFalse(DBHelper.getAgreementDataByAgreementId(agreementId).isEmpty(),
                "Договор не был создан в бд Solar");
        softAssertNull(DBHelper.getAgreementAndTerminalRelationshipDataByAgreementId(agreementId).get("state"),
                "Связь договора и УС неврно сформировалась в бд Solar");
        softAssertNull(DBHelper.getAgreementAndAccountRelationshipDataByAgreementId(agreementId).get(0).get("closing_date"),
                "Запись о 'closing_date' должна отстутствовать");

        Log.step("Проверка JSON GetAccountList");
        String getAccountListResponse = getAccountListService.post(agreementNumber)
                .should(Conditions.responseTypeIsSuccess())
                .should(Conditions.responseTypeIsSLR0001())
                .should(Conditions.hasJsonSchema("solarAir" + FILE_SEPARATOR + "getAccountList" + FILE_SEPARATOR + "getAccountListResponse.json"))
                .asResponse().asString();
        String accountId = JsonPath.from(getAccountListResponse).getString("body.accounts.account[0].id");
        String accountTypeId = JsonPath.from(getAccountListResponse).getString("body.accounts.account[0].accountType.id");
        softAssertEquals(
                JsonPath.from(getAccountListResponse).getString("body.accounts.account[0].agreement.id"),
                agreementId,
                "agreement id должны совпадать");

        Log.step("Проверка JSON SaveAccount закрытие счета");
        String resp = saveAccountService.closeAccount(accountId, accountTypeId, agreementId, accountNumber)
                .should(Conditions.responseTypeIsSuccess())
                .should(Conditions.responseTypeIsSLR0001())
                .should(Conditions.hasJsonSchema("solarAir" + FILE_SEPARATOR + "saveAccount" + FILE_SEPARATOR + "closeAccountResponse.json"))
                .asString("body.account.accountNumber");
        softAssertEquals(resp, accountNumber, "Номера счетов должны совпадать");

        //database asserts
        CommonHelper.sleep(3000L);
        String currentDate = CommonHelper.getCurrentDateInFormat("yyyy-MM-dd");
        softAssertEquals(
                DBHelper.getAgreementAndAccountRelationshipDataByAccountNumber(accountNumber).get("closing_date").toString(),
                currentDate,
                "Поле 'closing_date' должно иметь значение текущей даты");

        Log.step("Проверка JSON SaveAccount добавление нового счета");
        String newAccountNumber = SaveAgreementGenerator.generateRandomAccountNumber();
        saveAccountService.createNewAccount(agreementId, newAccountNumber)
                .should(Conditions.responseTypeIsSuccess())
                .should(Conditions.responseTypeIsSLR0001())
                .should(Conditions.hasJsonSchema("solarAir" + FILE_SEPARATOR + "saveAccount" + FILE_SEPARATOR + "createNewAccountResponse.json"));

        //database asserts
        CommonHelper.sleep(3000L);
        softAssertNull(
                DBHelper.getAgreementAndAccountRelationshipDataByAccountNumberAndAgreementId(newAccountNumber, agreementId).get("closing_date"),
                "Поле 'closing_date' должно иметь значение null");

        Log.step("Проверка JSON деактивация УС");
        deactivateAccessorService.deactivate(contractNumber)
                .should(Conditions.responseTypeIsSuccess())
                .should(Conditions.responseTypeIsSLR0001())
                .should(Conditions.hasJsonSchema("solarAir" + FILE_SEPARATOR + "deactivateAccessor" + FILE_SEPARATOR + "deactivateTerminalResponse.json"));

        //database asserts
        softAssertEquals(
                DBHelper.getAgreementAndTerminalRelationshipDataByAgreementId(agreementId).get("state"),
                "I",
                "Поле 'state' имеет неверное значение");

        Log.step("Проверка JSON закрытие договора");
        agreementService.close(agreementNumber)
                .should(Conditions.responseTypeIsSuccess())
                .should(Conditions.responseTypeIsSLR0001())
                .should(Conditions.hasJsonSchema("solarAir" + FILE_SEPARATOR + "saveAgreement" + FILE_SEPARATOR + "closeAgreementResponse.json"));

        //database asserts
        softAssertEquals(DBHelper.getClassifierDataByAgreementId(agreementId).get("valid_from").toString().split(" ")[0],
                currentDate,
                "Поле 'valid_from' должно иметь значение текущей даты");

        softAssertAll();
    }
}
