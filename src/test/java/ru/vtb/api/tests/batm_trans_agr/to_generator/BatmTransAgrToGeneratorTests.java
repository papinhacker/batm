package ru.vtb.api.tests.batm_trans_agr.to_generator;

import io.qameta.allure.TmsLink;
import org.testng.annotations.Test;
import ru.vtb.api.tests.batm_trans_agr.BatmTransAgrTests;
import ru.vtb.core.data.static_data.Bin;
import ru.vtb.core.data.static_data.dto.TransactionDto;
import ru.vtb.core.utils.B2LHelper;
import ru.vtb.core.utils.CommonHelper;
import ru.vtb.core.utils.DBHelper;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static ru.vtb.core.data.static_data.B2LMask.*;
import static ru.vtb.core.data.static_data.MapsKeysConstants.ACCOUNT_NUMBER;
import static ru.vtb.core.data.static_data.MapsKeysConstants.TERMINAL_ID;

public class BatmTransAgrToGeneratorTests extends BatmTransAgrTests {

    private static final String TO_GENERATOR_ATM_ID = CommonHelper.getRandomFromList(DBHelper.TransAgr.getTerminalIdsToGenerator());

    @Test(description = "Проверка Маршрутизации проводок в ROUTE-PP")
    @TmsLink("BATM-415")
    public void testBATM415() {
        String b2lFilename = B2LHelper.generateBase2LikeFileName(_298cto);
        List<TransactionDto> trans = new ArrayList<>();
        trans.addAll(B2LHelper.getBundleOfWithdrawalAndWithdrawalReversedAndCashInTransactions(Map.of(
                ACCOUNT_NUMBER, Bin.VISA_VTB.genRandAccNum(),
                TERMINAL_ID, TO_GENERATOR_ATM_ID)));
        trans.addAll(B2LHelper.getBundleOfWithdrawalAndWithdrawalReversedAndCashInTransactions(Map.of(
                ACCOUNT_NUMBER, Bin.MIR_VTB.genRandAccNum(),
                TERMINAL_ID, TO_GENERATOR_ATM_ID)));
        trans.addAll(B2LHelper.getBundleOfWithdrawalAndWithdrawalReversedAndCashInTransactions(Map.of(
                ACCOUNT_NUMBER, Bin.MASTERCARD_VTB.genRandAccNum(),
                TERMINAL_ID, TO_GENERATOR_ATM_ID)));

        testTransAgrToGenerator(b2lFilename, trans);
    }

    @Test(description = "Проверка Маршрутизации проводок CORP_CARD  cash-in в ROUTE-PP")
    @TmsLink("BATM-485")
    public void testBATM485() {
        String b2lFilename = B2LHelper.generateBase2LikeFileName(CORP_038ctd);
        List<TransactionDto> trans = new ArrayList<>();
        trans.add(B2LHelper.getNewCashInTransactionReference(Map.of(
                ACCOUNT_NUMBER, Bin.VISA_VTB.genRandAccNum(),
                TERMINAL_ID, TO_GENERATOR_ATM_ID)));
        trans.add(B2LHelper.getNewCashInTransactionReference(Map.of(
                ACCOUNT_NUMBER, Bin.MIR_VTB.genRandAccNum(),
                TERMINAL_ID, TO_GENERATOR_ATM_ID)));
        trans.add(B2LHelper.getNewCashInTransactionReference(Map.of(
                ACCOUNT_NUMBER, Bin.MASTERCARD_VTB.genRandAccNum(),
                TERMINAL_ID, TO_GENERATOR_ATM_ID)));

        testTransAgrToGenerator(b2lFilename, trans);
    }

    @Test(description = "Проверка Маршрутизации проводок CORP_CARD Withdrawal в ROUTE-PP")
    @TmsLink("BATM-486")
    public void testBATM486() {
        String b2lFilename = B2LHelper.generateBase2LikeFileName(CORP_038cto);
        List<TransactionDto> trans = new ArrayList<>();
        trans.addAll(B2LHelper.getBundleOfWithdrawalAndWithdrawalReversedTransactions(Map.of(
                ACCOUNT_NUMBER, Bin.VISA_VTB.genRandAccNum(),
                TERMINAL_ID, TO_GENERATOR_ATM_ID)));
        trans.addAll(B2LHelper.getBundleOfWithdrawalAndWithdrawalReversedTransactions(Map.of(
                ACCOUNT_NUMBER, Bin.MIR_VTB.genRandAccNum(),
                TERMINAL_ID, TO_GENERATOR_ATM_ID)));
        trans.addAll(B2LHelper.getBundleOfWithdrawalAndWithdrawalReversedTransactions(Map.of(
                ACCOUNT_NUMBER, Bin.MASTERCARD_VTB.genRandAccNum(),
                TERMINAL_ID, TO_GENERATOR_ATM_ID)));

        testTransAgrToGenerator(b2lFilename, trans);
    }
}
