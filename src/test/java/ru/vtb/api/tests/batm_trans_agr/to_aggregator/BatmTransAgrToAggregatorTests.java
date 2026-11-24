package ru.vtb.api.tests.batm_trans_agr.to_aggregator;

import io.qameta.allure.TmsLink;
import org.testng.annotations.Test;
import ru.vtb.api.tests.batm_trans_agr.BatmTransAgrTests;
import ru.vtb.core.data.static_data.dto.TransactionDto;
import ru.vtb.core.utils.B2LHelper;
import ru.vtb.core.utils.CommonHelper;
import ru.vtb.core.utils.DBHelper;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static ru.vtb.core.data.static_data.B2LMask.*;
import static ru.vtb.core.data.static_data.Bin.*;
import static ru.vtb.core.data.static_data.MapsKeysConstants.ACCOUNT_NUMBER;
import static ru.vtb.core.data.static_data.MapsKeysConstants.TERMINAL_ID;
import static ru.vtb.core.data.static_data.dto.TransactionDto.CORP_TO_JSON_M_F_TERMINALS;

public class BatmTransAgrToAggregatorTests extends BatmTransAgrTests {

    private static final String TO_AGGREGATOR_ATM_ID_M_T = CommonHelper.getRandomFromList(DBHelper.TransAgr.getTerminalIdsToAggregator(true));
    private static final String TO_AGGREGATOR_ATM_ID_M_F = CommonHelper.getRandomFromList(DBHelper.TransAgr.getTerminalIdsToAggregator(false));

    @Test(description = "Проверка Маршрутизации проводок в витрину 298cto без миграции")
    @TmsLink("BATM-337")
    public void testBATM337() {
        String b2lFilename = B2LHelper.generateBase2LikeFileName(_298cto);
        List<TransactionDto> trans = new ArrayList<>();
        String terminalId = TO_AGGREGATOR_ATM_ID_M_F;
        trans.addAll(B2LHelper.getBundleOfWithdrawalAndWithdrawalReversedAndCashInTransactions(Map.of(
                ACCOUNT_NUMBER, VISA_VTB.genRandAccNum(),
                TERMINAL_ID, terminalId)));
        trans.addAll(B2LHelper.getBundleOfWithdrawalAndWithdrawalReversedAndCashInTransactions(Map.of(
                ACCOUNT_NUMBER, MIR_VTB.genRandAccNum(),
                TERMINAL_ID, terminalId)));
        trans.addAll(B2LHelper.getBundleOfWithdrawalAndWithdrawalReversedAndCashInTransactions(Map.of(
                ACCOUNT_NUMBER, MASTERCARD_VTB.genRandAccNum(),
                TERMINAL_ID, terminalId)));

        testTransAgrToAggregator(b2lFilename, trans);
    }

    @Test(description = "Проверка Маршрутизации проводок в витрину  298cto Мигрированные нзр")
    @TmsLink("BATM-341")
    public void testBATM341() {
        String b2lFilename = B2LHelper.generateBase2LikeFileName(_298cto);
        List<TransactionDto> trans = new ArrayList<>();
        String terminalId = TO_AGGREGATOR_ATM_ID_M_T;
        trans.addAll(B2LHelper.getBundleOfWithdrawalAndWithdrawalReversedAndCashInTransactions(Map.of(
                ACCOUNT_NUMBER, VISA_VTB.genRandAccNum(),
                TERMINAL_ID, terminalId)));
        trans.addAll(B2LHelper.getBundleOfWithdrawalAndWithdrawalReversedAndCashInTransactions(Map.of(
                ACCOUNT_NUMBER, MIR_VTB.genRandAccNum(),
                TERMINAL_ID, terminalId)));
        trans.addAll(B2LHelper.getBundleOfWithdrawalAndWithdrawalReversedAndCashInTransactions(Map.of(
                ACCOUNT_NUMBER, MASTERCARD_VTB.genRandAccNum(),
                TERMINAL_ID, terminalId)));

        testTransAgrToAggregator(b2lFilename, trans);
    }

    @Test(description = "Проверка Маршрутизации проводок в витрину extVTB")
    @TmsLink("BATM-342")
    public void testBATM342() {
        String b2lFilename = B2LHelper.generateBase2LikeFileName(VTB24_038cto);
        List<TransactionDto> trans = new ArrayList<>();
        String terminalId = TO_AGGREGATOR_ATM_ID_M_T;
        trans.addAll(B2LHelper.getBundleOfWithdrawalAndWithdrawalReversedAndCashInTransactions(Map.of(
                ACCOUNT_NUMBER, VISA_VTB.genRandAccNum(),
                TERMINAL_ID, terminalId)));
        trans.addAll(B2LHelper.getBundleOfWithdrawalAndWithdrawalReversedAndCashInTransactions(Map.of(
                ACCOUNT_NUMBER, MIR_VTB.genRandAccNum(),
                TERMINAL_ID, terminalId)));
        trans.addAll(B2LHelper.getBundleOfWithdrawalAndWithdrawalReversedAndCashInTransactions(Map.of(
                ACCOUNT_NUMBER, MASTERCARD_VTB.genRandAccNum(),
                TERMINAL_ID, terminalId)));

        testTransAgrToAggregator(b2lFilename, trans);
    }

    @Test(description = "Проверка Маршрутизации проводок в витрину CORP_038cto без миграции")
    @TmsLink("BATM-437")
    public void testBATM437() {
        String b2lFilename = B2LHelper.generateBase2LikeFileName(CORP_038cto);
        List<TransactionDto> trans = new ArrayList<>();
        String terminalId = TO_AGGREGATOR_ATM_ID_M_F;
        trans.addAll(B2LHelper.getBundleOfWithdrawalAndWithdrawalReversedTransactions(Map.of(
                ACCOUNT_NUMBER, VISA_VTB.genRandAccNum(),
                TERMINAL_ID, terminalId)));
        trans.addAll(B2LHelper.getBundleOfWithdrawalAndWithdrawalReversedTransactions(Map.of(
                ACCOUNT_NUMBER, MIR_VTB.genRandAccNum(),
                TERMINAL_ID, terminalId)));
        trans.addAll(B2LHelper.getBundleOfWithdrawalAndWithdrawalReversedTransactions(Map.of(
                ACCOUNT_NUMBER, MASTERCARD_VTB.genRandAccNum(),
                TERMINAL_ID, terminalId)));

        testTransAgrToAggregator(b2lFilename, trans);
    }

    @Test(description = "Проверка Маршрутизации проводок в витрину  CORP_038cto Мигрированные нзр")
    @TmsLink("BATM-438")
    public void testBATM438() {
        String b2lFilename = B2LHelper.generateBase2LikeFileName(CORP_038cto);
        List<TransactionDto> trans = new ArrayList<>();
        String terminalId = TO_AGGREGATOR_ATM_ID_M_T;
        trans.addAll(B2LHelper.getBundleOfWithdrawalAndWithdrawalReversedTransactions(Map.of(
                ACCOUNT_NUMBER, VISA_VTB.genRandAccNum(),
                TERMINAL_ID, terminalId)));
        trans.addAll(B2LHelper.getBundleOfWithdrawalAndWithdrawalReversedTransactions(Map.of(
                ACCOUNT_NUMBER, MIR_VTB.genRandAccNum(),
                TERMINAL_ID, terminalId)));
        trans.addAll(B2LHelper.getBundleOfWithdrawalAndWithdrawalReversedTransactions(Map.of(
                ACCOUNT_NUMBER, MASTERCARD_VTB.genRandAccNum(),
                TERMINAL_ID, terminalId)));

        testTransAgrToAggregator(b2lFilename, trans);
    }

    @Test(description = "Проверка Маршрутизации проводок в витрину D_038cto")
    @TmsLink("BATM-439")
    public void testBATM439() {
        String b2lFilename = B2LHelper.generateBase2LikeFileName(D_038cto);
        List<TransactionDto> trans = new ArrayList<>();
        String terminalId = TO_AGGREGATOR_ATM_ID_M_T;
        trans.addAll(B2LHelper.getAllTransactionTypes(Map.of(
                ACCOUNT_NUMBER, VISA_VTB.genRandAccNum(),
                TERMINAL_ID, terminalId)));
        trans.addAll(B2LHelper.getAllTransactionTypes(Map.of(
                ACCOUNT_NUMBER, MIR_VTB.genRandAccNum(),
                TERMINAL_ID, terminalId)));
        trans.addAll(B2LHelper.getAllTransactionTypes(Map.of(
                ACCOUNT_NUMBER, MASTERCARD_VTB.genRandAccNum(),
                TERMINAL_ID, terminalId)));
        trans.addAll(B2LHelper.getAllTransactionTypes(Map.of(
                ACCOUNT_NUMBER, AMERICAN_EXPRESS.genRandAccNum(),
                TERMINAL_ID, terminalId)));
        trans.addAll(B2LHelper.getAllTransactionTypes(Map.of(
                ACCOUNT_NUMBER, UNION_PAY.genRandAccNum(),
                TERMINAL_ID, terminalId)));

        testTransAgrToAggregator(b2lFilename, trans);
    }

    @Test(description = "Проверка Маршрутизации проводок в витрину CORP_038ctd без миграции")
    @TmsLink("BATM-477")
    public void testBATM477() {
        String b2lFilename = B2LHelper.generateBase2LikeFileName(CORP_038ctd);
        List<TransactionDto> trans = new ArrayList<>();
        String terminalId = CommonHelper.getRandomFromList(DBHelper.TransAgr.get20208AtmIds(CORP_TO_JSON_M_F_TERMINALS));
        trans.add(B2LHelper.getNewCashInTransactionReference(Map.of(
                ACCOUNT_NUMBER, VISA_VTB.genRandAccNum(),
                TERMINAL_ID, terminalId)));
        trans.add(B2LHelper.getNewCashInTransactionReference(Map.of(
                ACCOUNT_NUMBER, MIR_VTB.genRandAccNum(),
                TERMINAL_ID, terminalId)));
        trans.add(B2LHelper.getNewCashInTransactionReference(Map.of(
                ACCOUNT_NUMBER, MASTERCARD_VTB.genRandAccNum(),
                TERMINAL_ID, terminalId)));

        testTransAgrToAggregator(b2lFilename, trans);
    }

    @Test(description = "Проверка Маршрутизации проводок в витрину CORP_038ctd Мигрированные нзр")
    @TmsLink("BATM-478")
    public void testBATM478() {
        String b2lFilename = B2LHelper.generateBase2LikeFileName(CORP_038ctd);
        List<TransactionDto> trans = new ArrayList<>();
        String terminalId = TO_AGGREGATOR_ATM_ID_M_T;
        trans.add(B2LHelper.getNewCashInTransactionReference(Map.of(
                ACCOUNT_NUMBER, VISA_VTB.genRandAccNum(),
                TERMINAL_ID, terminalId)));
        trans.add(B2LHelper.getNewCashInTransactionReference(Map.of(
                ACCOUNT_NUMBER, MIR_VTB.genRandAccNum(),
                TERMINAL_ID, terminalId)));
        trans.add(B2LHelper.getNewCashInTransactionReference(Map.of(
                ACCOUNT_NUMBER, MASTERCARD_VTB.genRandAccNum(),
                TERMINAL_ID, terminalId)));

        testTransAgrToAggregator(b2lFilename, trans);
    }
}
