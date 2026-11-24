package ru.vtb.core.models.common;

import ru.vtb.core.utils.DBHelper;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

public class CommonModelHelper {
    private CommonModelHelper() {
    }

    public static String generateRandomMessageId() {
        return UUID.randomUUID().toString();
    }

    public static String getMessageDate() {
        ZonedDateTime now = ZonedDateTime.now(ZoneId.systemDefault());
        return now.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);
    }

    public static String getDeprecatedMessageDate() {
        ZonedDateTime now = ZonedDateTime.now(ZoneId.systemDefault());
        return now.format(DateTimeFormatter.ISO_DATE_TIME);
    }

    public static String getMessageOperDate() {
        ZonedDateTime now = ZonedDateTime.of(
                LocalDate.parse(DBHelper.TransAgr.getCurrentBankDate(),
                        DateTimeFormatter.ISO_LOCAL_DATE), LocalTime.now(),
                ZoneId.systemDefault());
        return now.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);
    }
}
