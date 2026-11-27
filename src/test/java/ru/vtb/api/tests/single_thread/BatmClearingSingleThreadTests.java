package ru.vtb.api.tests.single_thread;

import io.qameta.allure.TmsLink;
import org.joda.time.DateTime;
import org.testng.Assert;
import org.testng.annotations.Ignore;
import org.testng.annotations.Test;
import ru.vtb.api.state.BaseAPIState;
import ru.vtb.core.utils.CommonHelper;
import ru.vtb.core.utils.DBHelper;
import ru.vtb.core.utils.KafkaHelper;

import java.text.SimpleDateFormat;

import static ru.vtb.core.data.static_data.KafkaTopic.SUTP_EVENTS;

public class BatmClearingSingleThreadTests extends BaseAPIState {

    // НЕРАБОЧИЙ: сообщение SUTP_EVENTS ищется только по банковской дате — поле, идентифицирующее
    // сообщение этого теста, не определено (см. docs/kafka-design.md)
    @Ignore
    @Test(description = "Проверка автоматической отправки уведомления в СУТП")
    @TmsLink("BATM-283")
    public void testBATM283() {
        synchronized (monitor) {
            try {
                String currentDateMinusOneDay = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS")
                        .format(new DateTime().minusDays(1).toDate());
                DBHelper.Clearing.updateSendingMessageZodDate(currentDateMinusOneDay);
                String currentTimePlusOneMinute = new SimpleDateFormat("HH:mm:ss")
                        .format(new DateTime().minusHours(3).plusSeconds(10).toDate()); //локальное время сервиса по Гринвичу
                DBHelper.Clearing.updateTimeStartInManagementZod(currentTimePlusOneMinute);

                String currentDate = CommonHelper.getCurrentDateInFormat("yyyy-MM-dd");
                KafkaHelper.getLastRecord(SUTP_EVENTS, DBHelper.TransAgr.getCurrentBankDate(), 220);

                Assert.assertEquals(
                        CommonHelper.parseDateFromPatternToAnotherPattern(
                                "yyyy-MM-dd HH:mm:ss.SSS",
                                "yyyy-MM-dd",
                                DBHelper.Clearing.getSendingMessageZodDate()),
                        currentDate,
                        "В поле 'sending_message_zod' таблицы 'zod_management' записалась неверная дата");
            } finally {
                DBHelper.Clearing.updateTimeStartInManagementZod("23:30:00");
            }
        }
    }
}
