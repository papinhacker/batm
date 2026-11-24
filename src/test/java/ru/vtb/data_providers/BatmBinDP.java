package ru.vtb.data_providers;

import org.testng.annotations.DataProvider;

public class BatmBinDP {

    @DataProvider(name = "BatmBinFilenames")
    public static Object[][] binNames() {
        return new Object[][]{{"Binm"}, {"Binv"}, {"Bavi"}, {"Bavu"}};//, {"Bava"}};
    }
}
