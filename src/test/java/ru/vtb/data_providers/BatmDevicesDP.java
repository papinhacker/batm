package ru.vtb.data_providers;

import org.testng.annotations.DataProvider;

public class BatmDevicesDP {

    @DataProvider(name = "BatmDevicesWrongAtmExplorationStatuses")
    public static Object[][] wrongAtmExplorationStatuses() {
        return new Object[][]{
                {"errorneous"}, {"evidence"},
                {"moving"}, {"no_migration"}, {"prepared"},
                {"prepared_in_bank"}, {"prepared_in_mk"}, {"ready_to_migrate"},
                {"repairing"}, {"temporarywithdrawn"}, {"testing"},
                {"to_migrate"}, {"to_rotation"}, {"warehousing"}};
    }
}
