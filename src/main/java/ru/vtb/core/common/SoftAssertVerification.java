package ru.vtb.core.common;

import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.asserts.SoftAssert;

import java.util.Objects;

public class SoftAssertVerification {

    protected SoftAssert softAssert;

    @BeforeMethod(alwaysRun = true)
    public void softAssertVerificationBeforeMethod() {
        this.softAssert = new SoftAssert();
    }

    @AfterMethod(alwaysRun = true)
    public void softAssertVerificationAfterMethod() {
        this.softAssert = null;
    }

    public void softAssertTrue(boolean condition, String message) {
        this.softAssert.assertTrue(condition, message);
        if (!condition) {
            Log.assertFail(message);
        }
    }

    public void softAssertFalse(boolean condition, String message) {
        this.softAssert.assertFalse(condition, message);
        if (condition) {
            Log.assertFail(message);
        }
    }

    public void softAssertNull(Object object, String message) {
        this.softAssert.assertNull(object, message);
        if (object != null) {
            Log.assertFail(message);
        }
    }

    public void softAssertNotNull(Object object, String message) {
        this.softAssert.assertNotNull(object, message);
        if (Objects.isNull(object)) {
            Log.assertFail(message);
        }
    }

    public void softAssertEquals(Object actual, Object expected, String message) {
        this.softAssert.assertEquals(actual, expected, message);
        if (!Objects.equals(actual, expected)) {
            Log.assertFail("%s.%nDifference:%nActual   : '%s'%nExpected : '%s'".formatted(message, actual, expected));
        }
    }

    public void softAssertNotEquals(Object actual, Object expected, String message) {
        this.softAssert.assertNotEquals(actual, expected, message);
        if (Objects.equals(actual, expected)) {
            Log.assertFail("%s.%nObjects must be not equals:%nActual   : '%s'%nExpected : '%s'".formatted(message, actual, expected));
        }
    }

    public void softAssertAll() {
        this.softAssert.assertAll();
    }
}
