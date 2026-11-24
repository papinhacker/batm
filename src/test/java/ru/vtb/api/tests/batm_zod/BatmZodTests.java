package ru.vtb.api.tests.batm_zod;

import io.qameta.allure.TmsLink;
import io.restassured.path.json.JsonPath;
import org.joda.time.DateTime;
import org.testng.annotations.Test;
import ru.vtb.api.state.BaseAPIState;
import ru.vtb.core.models.common.CommonModelHelper;
import ru.vtb.core.models.kafka.batm_zod_in_batm_acc_route_s3_load_result.BatmZodInBatmAccRouteS3LoadResultRootMessage;
import ru.vtb.core.models.kafka.batm_zod_out_batm_acc_route_s3_event.BatmZodOutBatmAccRouteS3EventRootMessage;
import ru.vtb.core.models.kafka.sutp_events.SutpEventsRootMessage;
import ru.vtb.core.models.kafka.sutp_to_batm.SutpToBatmRootMessage;
import ru.vtb.core.models.kafka.sutp_to_batm.generators.SutpToBatmRootMessageGenerator;
import ru.vtb.core.utils.CommonHelper;
import ru.vtb.core.utils.DBHelper;
import ru.vtb.core.utils.KafkaHelper;

import java.text.SimpleDateFormat;

import static ru.vtb.core.data.static_data.KafkaTopic.*;

public class BatmZodTests extends BaseAPIState {

    @Test(description = "Проверка обработки сообщения о выгрузке в Агрегатор")
    @TmsLink("BATM-257")
    public void testBATM257() {
        var sutpToBatmMsg = SutpToBatmRootMessageGenerator.generate();
        KafkaHelper.sendMessage(SUTP_TO_BATM, sutpToBatmMsg, false);
        String transparentId = sutpToBatmMsg.getRequestId();

        var sutpEventsMsg1 = (SutpEventsRootMessage)
                KafkaHelper.getLastRecord(SUTP_EVENTS, transparentId, long_timeout);

        var routeS3EventMsg = (BatmZodOutBatmAccRouteS3EventRootMessage)
                KafkaHelper.getLastRecord(BATM_ZOD_OUT_BATM_ACC_ROUTE_S3_EVENT, transparentId, long_timeout);
        softAssertNull(routeS3EventMsg.getEventDate(), "err1");

        var routeS3LoadResultMsg = (BatmZodInBatmAccRouteS3LoadResultRootMessage)
                KafkaHelper.getLastRecord(BATM_ZOD_IN_BATM_ACC_ROUTE_S3_LOAD_RESULT, transparentId, orcTimeout);

        var sutpEventsMsg2 = (SutpEventsRootMessage)
                KafkaHelper.getLastRecord(SUTP_EVENTS, transparentId, long_timeout);

        var dbData = DBHelper.Zod.getSutpRequestData(transparentId);
        softAssertEquals(
                dbData.get("status").toString(),
                "SUCCESS",
                "error"
        );

        softAssertAll();
    }

    @Test(description = "Проверка успешного ЗОД в batm_transactions_agr")
    @TmsLink("BATM-351")
    public void testBATM351() {
        String currDay = CommonHelper.getCurrentDateInFormat("yyyy-MM-dd");
        DBHelper.TransAgrSrv.setCurrentBankDate(currDay);
        var sutpToBatmMsg = SutpToBatmRootMessage.builder()
                .requestId(CommonModelHelper.generateRandomMessageId())
                .eventName("BATM-SwitchDay")
                .eventDate(CommonHelper.getCurrentDateInFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'"))
                .description("Переключение операционного дня")
                .operdayDate(currDay)
                .branchCode("00000")
                .systemCode("2093")
                .systemName("СУТП ЗОД")
                .build();
        KafkaHelper.sendMessage(SUTP_TO_BATM, sutpToBatmMsg, false);
        String transparentId = sutpToBatmMsg.getRequestId();

        KafkaHelper.getLastRecord(BATM_ZOD_OUT_BATM_TRANS_AGR_ZOD, transparentId, long_timeout);

        softAssertEquals(
                DBHelper.TransAgrSrv.getCurrentBankDate(),
                new SimpleDateFormat("yyyy-MM-dd").format(new DateTime().plusDays(1).toDate()),
                "error");

        softAssertEquals(
                JsonPath.from((String) KafkaHelper.getLastRecord(BATM_ZOD_IN_BATM_TRANS_AGR_ZOD_RESULT, transparentId, timeout)).getString("statusLoad"),
                "SUCCESS",
                "error2");

        softAssertEquals(
                DBHelper.Zod.getSutpRequestData(transparentId).get("status").toString(),
                "SUCCESS",
                "err3");

        softAssertAll();
    }

}
