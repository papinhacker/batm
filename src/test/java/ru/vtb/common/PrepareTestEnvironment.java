package ru.vtb.common;

import org.testng.annotations.Test;
import ru.vtb.core.utils.CommonHelper;
import ru.vtb.core.utils.DBHelper;

public class PrepareTestEnvironment {

    @Test(description = "Setting up bank date to current date")
    public void updateOperday() {
        DBHelper.TransAgr.setCurrentBankDate(CommonHelper.getCurrentDateInFormat("yyyy-MM-dd"));
    }

    @Test(description = "update zod_management set time_start = '23:30:00'")
    public void updateZodStartTime() {
        DBHelper.Clearing.updateTimeStartInManagementZod("23:30:00");
    }

    @Test(description = "truncate table transactions in route S3")
    public void truncateTransactionsInRouteS3() {
        DBHelper.BatmAccRouteS3.truncateTransactions();
    }
}
