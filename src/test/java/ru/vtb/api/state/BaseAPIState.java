package ru.vtb.api.state;

import io.qameta.allure.Allure;
import io.qameta.allure.restassured.AllureRestAssured;
import io.restassured.RestAssured;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.BeforeSuite;
import org.testng.annotations.Listeners;
import ru.vtb.api.services.AgreementService;
import ru.vtb.api.services.DeactivateAccessorService;
import ru.vtb.api.services.GetAccountListService;
import ru.vtb.api.services.SaveAccountService;
import ru.vtb.core.common.EnvConstants;
import ru.vtb.core.common.SoftAssertVerification;
import ru.vtb.core.common.listeners.TestNGListener;
import ru.vtb.core.utils.S3Service;

import java.util.Map;

import static ru.vtb.core.common.EnvConstants.FILE_SEPARATOR;
import static ru.vtb.core.common.EnvConstants.TEMP_FILES_FOLDER;

@Listeners(TestNGListener.class)
public class BaseAPIState extends SoftAssertVerification {

    protected static final String TEMP_FILES_PATH = TEMP_FILES_FOLDER + FILE_SEPARATOR;
    protected static final String DOWNLOADED_FILES_PATH = TEMP_FILES_PATH + "downloadedFiles" + FILE_SEPARATOR;

    protected static final Object monitor = new Object();
    protected long timeout = EnvConstants.TIMEOUT;
    protected long short_timeout = timeout / 3;
    protected long long_timeout = timeout * 7;
    protected long orcTimeout = 310;
    protected AgreementService agreementService;
    protected DeactivateAccessorService deactivateAccessorService;
    protected GetAccountListService getAccountListService;
    protected SaveAccountService saveAccountService;

    protected S3Service s3Service;

    /**
     * Глобальная настройка RestAssured — один раз на сьют, до всех тестов (в т.ч. при parallel=tests).
     * RestAssured.filters(...) добавляет фильтр в глобальный список, поэтому фильтр Allure добавляется,
     * только если его там ещё нет: повторный вызов (несколько сьютов в одной JVM) дублей не создаёт.
     */
    @BeforeSuite(alwaysRun = true)
    public void configureRestAssured() {
        RestAssured.baseURI = EnvConstants.BASE_URI + "/solar-bridge-ws/ext";
        if (RestAssured.filters().stream().noneMatch(AllureRestAssured.class::isInstance)) {
            RestAssured.filters(/*new RequestLoggingFilter(), new ResponseLoggingFilter(),*/ new AllureRestAssured());
        }
    }

    @BeforeMethod
    public void setUp() {
        agreementService = new AgreementService();
        deactivateAccessorService = new DeactivateAccessorService();
        getAccountListService = new GetAccountListService();
        saveAccountService = new SaveAccountService();
        s3Service = new S3Service();
    }

    @AfterMethod
    public void tearDown() {
        s3Service.cleanup();
    }

    protected void setAllureInfo(Object... info) {
        if (info.length < 1) {
            throw new IllegalArgumentException("В методе 'setAllureInfo' должен быть указан хотя бы один параметр");
        }
        Allure.epic("API");
        Allure.feature((String) info[0]);
        if (info.length > 1) {
            Allure.story((String) info[1]);
        }
    }
}
