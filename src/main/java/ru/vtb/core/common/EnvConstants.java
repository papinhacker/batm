package ru.vtb.core.common;

import org.apache.commons.lang3.StringUtils;

import java.io.FileReader;
import java.io.IOException;
import java.nio.file.FileSystems;
import java.util.Properties;

public class EnvConstants {

    /* **************************
     ****** Working fields ******
     ***************************/

    private static final String ENV = getEnvironment();
    private static final String ENCRYPTION_KEY = System.getProperty("ekey");

    /* common constants */
    public static final String FILE_SEPARATOR = FileSystems.getDefault().getSeparator();
    public static final String PROJECT_DIRECTORY = System.getProperty("user.dir");
    public static final String MAIN_RESOURCES_FOLDER = PROJECT_DIRECTORY + FILE_SEPARATOR
            + "src" + FILE_SEPARATOR
            + "main" + FILE_SEPARATOR
            + "resources";
    public static final String TEST_RESOURCES_FOLDER = PROJECT_DIRECTORY + FILE_SEPARATOR
            + "src" + FILE_SEPARATOR
            + "test" + FILE_SEPARATOR
            + "resources";
    public static final String TEMP_FILES_FOLDER = TEST_RESOURCES_FOLDER + FILE_SEPARATOR + "tempFiles";
    public static final String SCHEMAS_FOLDER = TEST_RESOURCES_FOLDER + FILE_SEPARATOR + "schemas";

    private static final Properties PROPERTIES = getPropsForEnv();

    /* **************************
     ******** Constants *********
     ***************************/

    public static final Long TIMEOUT = Long.parseLong(System.getProperty("timeout", "10"));

    /* base urls data */
    public static final String BASE_URI = getProperty("base.page.uri");
    public static final String BO_JSON_API_ENDPOINT = "/bo-json-api";

    /* database data */
    public static final String DB_URI = getProperty("db.uri");
    public static final String DB_DEFAULT_PASSWORD = getEncryptedProperty("db.password");
    public static final String DB_BATM_BIN_PASSWORD = getEncryptedProperty("db.batm.bin.password");
    public static final String DB_BATM_OUTBOX_PASSWORD = getEncryptedProperty("db.batm.outbox.password");
    public static final String DB_BATM_ACC_ROUTE_PASSWORD = getEncryptedProperty("db.batm.acc.route.password");
    public static final String DB_BATM_ACC_PASSWORD = getEncryptedProperty("db.batm.acc.password");
    public static final String DB_BATM_TRANSACTIONS_AGR_PASSWORD = getEncryptedProperty("db.batm.transactions.agr.password");
    public static final String DB_BATM_TRANSACTIONS_AGR_SRV_PASSWORD = getEncryptedProperty("db.batm.transactions.agr.srv.password");
    public static final String DB_BATM_OPERATIONS_SIOR_PASSWORD = getEncryptedProperty("db.batm.operations.sior.password");
    public static final String DB_BATM_SIOR_PRODUCT_ACC_PASSWORD = getEncryptedProperty("db.batm.sior,product.acc.password");

    /* s3 data */
    public static final String S3_URI = getProperty("s3.uri");
    public static final String S3_PORT = getProperty("s3.port");
    public static final String S3_ACCESS_KEY = getEncryptedProperty("s3.access.key");
    public static final String S3_SECRET_KEY = getEncryptedProperty("s3.secret.key");
    public static final String S3_REGION = getProperty("s3.region");
    public static final String S3_PROTOCOL = getProperty("s3.protocol");

    /* kafka options: настройки кластеров читает ru.vtb.core.utils.kafka.KafkaClusterConfig (ключи kafka.<имя>.*) */

    /* smtp data */
    public static final String SMTP_LOGIN = getProperty("smtp.login");
    public static final String SMTP_PASSWORD = getEncryptedProperty("smtp.password");
    public static final String SMTP_HOST = getProperty("smtp.host");

    private static String getEnvironment() {
        String cEnv = System.getProperty("cenv");
        if (StringUtils.isEmpty(cEnv)) {
            throw new RuntimeException("Unable to recognize environment name");
        }
        Log.info(String.format("About to initializing '%s' environment properties", cEnv.toUpperCase()));
        return cEnv.toLowerCase();
    }

    private static Properties getPropsForEnv() {
        Properties properties;

        try (FileReader reader = new FileReader(MAIN_RESOURCES_FOLDER + FILE_SEPARATOR
                + "properties" + FILE_SEPARATOR
                + String.format("%s.properties", EnvConstants.ENV))) {
            properties = new Properties();
            properties.load(reader);
        } catch (IOException e) {
            String err = String.format("Unable to read properties file for '%s' environment", EnvConstants.ENV);
            Log.error(err);
            throw new RuntimeException(err);
        }
        return properties;
    }

    /**
     * @return имя окружения из -Dcenv (в нижнем регистре)
     */
    public static String getEnvironmentName() {
        return ENV;
    }

    public static String getProperty(String key) {
        return PROPERTIES.getProperty(key);
    }

    public static String getEncryptedProperty(String key) {
        return decrypt(getProperty(key));
    }

    /**
     * Расшифровывает значение ключом -Dekey; без ключа или для null возвращает значение как есть.
     */
    public static String decrypt(String value) {
        if (value == null || StringUtils.isEmpty(ENCRYPTION_KEY)) {
            return value;
        }
        return new EncryptionSettings(ENCRYPTION_KEY).decryptString(value);
    }

}
