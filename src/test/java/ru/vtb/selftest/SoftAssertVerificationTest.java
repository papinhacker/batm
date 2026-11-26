package ru.vtb.selftest;

import org.testng.annotations.Test;
import ru.vtb.core.common.SoftAssertVerification;

import static org.testng.Assert.assertTrue;
import static org.testng.Assert.expectThrows;

/**
 * Регрессия: softAssertEquals / softAssertNotEquals падали с NPE при actual == null.
 * Наследуемся от SoftAssertVerification, чтобы его @BeforeMethod/@AfterMethod отрабатывали как в боевых тестах.
 */
public class SoftAssertVerificationTest extends SoftAssertVerification {

    @Test(groups = {"selftest"}, description = "softAssertEquals(null, null) проходит без NPE")
    public void equalsBothNullPasses() {
        softAssertEquals(null, null, "оба null");
        softAssertAll();
    }

    @Test(groups = {"selftest"}, description = "softAssertEquals(null, значение) без NPE, проверка проваливается в softAssertAll")
    public void equalsNullActualFailsOnAssertAll() {
        softAssertEquals(null, true, "Поле 'sent_to_dp'");
        AssertionError e = expectThrows(AssertionError.class, this::softAssertAll);
        assertTrue(e.getMessage().contains("Поле 'sent_to_dp'"), e.getMessage());
    }

    @Test(groups = {"selftest"}, description = "softAssertEquals(значение, null) проваливается в softAssertAll")
    public void equalsNullExpectedFailsOnAssertAll() {
        softAssertEquals("x", null, "expected null");
        expectThrows(AssertionError.class, this::softAssertAll);
    }

    @Test(groups = {"selftest"}, description = "softAssertEquals с равными значениями проходит")
    public void equalsEqualValuesPasses() {
        softAssertEquals(true, true, "boolean");
        softAssertEquals("SERVICE", "SERVICE", "string");
        softAssertAll();
    }

    @Test(groups = {"selftest"}, description = "softAssertNotEquals(null, значение) проходит без NPE")
    public void notEqualsNullActualPasses() {
        softAssertNotEquals(null, "x", "null и 'x' различаются");
        softAssertAll();
    }

    @Test(groups = {"selftest"}, description = "softAssertNotEquals(null, null) без NPE, проверка проваливается в softAssertAll")
    public void notEqualsBothNullFailsOnAssertAll() {
        softAssertNotEquals(null, null, "оба null");
        expectThrows(AssertionError.class, this::softAssertAll);
    }
}
