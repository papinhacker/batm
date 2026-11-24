package ru.vtb.data_providers;

import org.testng.annotations.DataProvider;

import java.util.Map;

import static ru.vtb.core.data.static_data.Bin.*;
import static ru.vtb.core.data.static_data.MapsKeysConstants.*;
import static ru.vtb.core.data.static_data.MapsValuesConstants.*;

public class BatmDpDP {

    @DataProvider(name = "BatmDpBATM300")
    public static Object[][] BATM300() {
        return new Object[][]{
                {Map.of(BIN, MIR_VTB.genRandAccNum(),
                        OPERATION_TYPE, ONUS_CARD,
                        OPERATION_TYPE_DESCRIPTION, WITHDRAWAL,
                        PURPOSE, "A1ATwc/ Операции выдачи по банковским картам ВТБ (ПАО) в банкоматах ВТБ (ПАО)",
                        INNER_TYPE_CODE, "WI.001",
                        DEBIT_ACCOUNT, TECHNICAL_ACCOUNT)},
                {Map.of(BIN, MASTERCARD_VTB.genRandAccNum(),
                        OPERATION_TYPE, ONUS_CARD,
                        OPERATION_TYPE_DESCRIPTION, WITHDRAWAL,
                        PURPOSE, "A1ATmc/ Операции выдачи по банковским картам ВТБ (ПАО) в банкоматах ВТБ (ПАО)",
                        INNER_TYPE_CODE, "WI.001",
                        DEBIT_ACCOUNT, TECHNICAL_ACCOUNT)},
                {Map.of(BIN, VISA_VTB.genRandAccNum(),
                        OPERATION_TYPE, ONUS_CARD,
                        OPERATION_TYPE_DESCRIPTION, WITHDRAWAL,
                        PURPOSE, "A1ATvc/ Операции выдачи по банковским картам ВТБ (ПАО) в банкоматах ВТБ (ПАО)",
                        INNER_TYPE_CODE, "WI.001",
                        DEBIT_ACCOUNT, TECHNICAL_ACCOUNT)}
        };
    }

    @DataProvider(name = "BatmDpBATM301")
    public static Object[][] BATM301() {
        return new Object[][]{
                {Map.of(BIN, MIR_VTB.genRandAccNum(), //mirVtb
                        OPERATION_TYPE, ONUS_CARD,
                        OPERATION_TYPE_DESCRIPTION, WITHDRAWAL_REVERSAL,
                        PURPOSE, "a1ATwc/Отмена операции выдачи по банковским картам ВТБ (ПАО) в банкоматах ВТБ (ПАО)",
                        INNER_TYPE_CODE, "CAWI.001",
                        CREDIT_ACCOUNT, TECHNICAL_ACCOUNT)},
                {Map.of(BIN, MASTERCARD_VTB.genRandAccNum(), //masterCardVtb
                        OPERATION_TYPE, ONUS_CARD,
                        OPERATION_TYPE_DESCRIPTION, WITHDRAWAL_REVERSAL,
                        PURPOSE, "a1ATmc/Отмена операции выдачи по банковским картам ВТБ (ПАО) в банкоматах ВТБ (ПАО)",
                        INNER_TYPE_CODE, "CAWI.001",
                        CREDIT_ACCOUNT, TECHNICAL_ACCOUNT)},
                {Map.of(BIN, VISA_VTB.genRandAccNum(), //visaVtb
                        OPERATION_TYPE, ONUS_CARD,
                        OPERATION_TYPE_DESCRIPTION, WITHDRAWAL_REVERSAL,
                        PURPOSE, "a1ATvc/Отмена операции выдачи по банковским картам ВТБ (ПАО) в банкоматах ВТБ (ПАО)",
                        INNER_TYPE_CODE, "CAWI.001",
                        CREDIT_ACCOUNT, TECHNICAL_ACCOUNT)}
        };
    }

    @DataProvider(name = "BatmDpBATM302")
    public static Object[][] BATM302() {
        return new Object[][]{
                {Map.of(BIN, MIR_VTB.genRandAccNum(), //mirVtb
                        OPERATION_TYPE, ONUS_CARD,
                        OPERATION_TYPE_DESCRIPTION, CASH_IN,
                        PURPOSE, "Y1ATwc/Пополнение б/к через Cash in",
                        INNER_TYPE_CODE, "DE.001",
                        CREDIT_ACCOUNT, TECHNICAL_ACCOUNT)},
                {Map.of(BIN, MASTERCARD_VTB.genRandAccNum(), //masterCardVtb
                        OPERATION_TYPE, ONUS_CARD,
                        OPERATION_TYPE_DESCRIPTION, CASH_IN,
                        PURPOSE, "Y1ATmc/Пополнение б/к через Cash in",
                        INNER_TYPE_CODE, "DE.001",
                        CREDIT_ACCOUNT, TECHNICAL_ACCOUNT)},
                {Map.of(BIN, VISA_VTB.genRandAccNum(), //visaVtb
                        OPERATION_TYPE, ONUS_CARD,
                        OPERATION_TYPE_DESCRIPTION, CASH_IN,
                        PURPOSE, "Y1ATvc/Пополнение б/к через Cash in",
                        INNER_TYPE_CODE, "DE.001",
                        CREDIT_ACCOUNT, TECHNICAL_ACCOUNT)}
        };
    }
}
