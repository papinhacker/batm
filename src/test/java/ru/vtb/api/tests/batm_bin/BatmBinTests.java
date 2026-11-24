package ru.vtb.api.tests.batm_bin;

import io.qameta.allure.TmsLink;
import io.restassured.path.json.JsonPath;
import org.testng.annotations.Test;
import ru.vtb.api.state.BaseAPIState;
import ru.vtb.core.utils.BinHelper;
import ru.vtb.core.utils.CommonHelper;
import ru.vtb.core.utils.DBHelper;
import ru.vtb.core.utils.KafkaHelper;
import ru.vtb.data_providers.BatmBinDP;

import static ru.vtb.core.data.static_data.KafkaTopic.SOLAR_OUT_RUN_JOB;
import static ru.vtb.core.data.static_data.S3Constants.BATM_OUTPUT_BIN;
import static ru.vtb.core.data.static_data.S3Constants.IF_BATM_WAY_IN;

public class BatmBinTests extends BaseAPIState {

    @Test(description = "Проверка чтения бакета микросервисом",
            dataProvider = "BatmBinFilenames", dataProviderClass = BatmBinDP.class)
    @TmsLink("BATM-277")
    public void testBATM277(String binMask) {
        String filename = BinHelper.generateBinFilename(binMask);

        s3Service.putObject(IF_BATM_WAY_IN, BATM_OUTPUT_BIN, BinHelper.getBinFileReference(filename));

//        softAssertFalse(DatabaseHelper.getBinFileProcessingStatusByFilename(filename).isEmpty(),
//                "Данные об обработке файла '%s' не записались в БД микросервиса".formatted(filename));

        String datePattern = "yyyy-MM-dd";
        softAssertEquals(CommonHelper.parseDateFromPatternToAnotherPattern("yyyy-MM-dd HH:mm:ss.SSS",
                        datePattern,
                        DBHelper.BatmBin.getDateOfDeletedFileFromS3ByFilename(filename)),
                CommonHelper.getCurrentDateInFormat(datePattern),
                "В БД микросервиса не появилась запись об удаление файла '%s'");

        softAssertFalse(s3Service.getObjectsNames(IF_BATM_WAY_IN).contains(filename),
                "Файл '%s' не был удалён из S3");

        softAssertEquals(DBHelper.getProcessedBinFileMetadataByBinFilename(filename),
                "SC",
                "Файл '%s' имеет неправильный статус обработки в БД Solar".formatted(filename));

        softAssertAll();
    }

    @Test(description = "Проверка взаимодействия  batm_bin  и Solar SUCCESS",
            dataProvider = "BatmBinFilenames", dataProviderClass = BatmBinDP.class)
    @TmsLink("BATM-279")
    public void testBATM279(String binMask) {
        String filename = BinHelper.generateBinFilename(binMask);

        s3Service.putObject(IF_BATM_WAY_IN, BATM_OUTPUT_BIN, BinHelper.getBinFileReference(filename));

        String status = JsonPath.from((String) KafkaHelper.getLastRecord(SOLAR_OUT_RUN_JOB, filename, long_timeout))
                .getString("body.job.status");

        softAssertEquals(status, "SUCCESS", "Событие в кафке имеет неверный статус");

        softAssertEquals(DBHelper.BatmBin.getBinFileProcessingStatusByFilename(filename),
                "SUCCESS",
                "Статус обработки файла '%s' не был обновлён в БД микросервиса".formatted(filename));

        softAssertAll();
    }
}
