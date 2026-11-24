package ru.vtb.core.common;

import org.testng.IRetryAnalyzer;
import org.testng.ITestResult;

public class RetryAnalyzer implements IRetryAnalyzer {
    int counter = 0;
    int retryLimit = 0;

    @Override
    public boolean retry(ITestResult iTestResult) {
        retryLimit = Integer.parseInt(System.getProperty("retryOnFailCount", "0"));
        if (counter < retryLimit) {
            ++counter;
            return true;
        }
        return false;
    }
}
