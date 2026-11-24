package ru.vtb.core.utils;

import org.apache.commons.lang3.RandomStringUtils;

public class AccountsHelper {

    public static String generateRandomAccountNumberWithDelimiter(String delimiter, String... currency) {
        String buff = "810";
        if (currency.length > 0) {
            buff = currency[0];
        }
        return String.join(delimiter, "20208",
                buff,
                RandomStringUtils.insecure().nextNumeric(1),
                RandomStringUtils.insecure().nextNumeric(4),
                RandomStringUtils.insecure().nextNumeric(7));
    }

    public static String generateRandomAccountNumber(String... currency) {
        return generateRandomAccountNumberWithDelimiter("", currency);
    }
}
