package ru.vtb.selftest.kafka;

import org.apache.kafka.clients.CommonClientConfigs;
import org.apache.kafka.common.config.SslConfigs;
import org.testng.annotations.Test;
import ru.vtb.core.utils.kafka.KafkaClusterConfig;

import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Map;
import java.util.Properties;

import static org.testng.Assert.*;
import static ru.vtb.core.utils.kafka.KafkaClusterConfig.*;

/**
 * Конфигурация Kafka-кластеров из properties (docs/kafka-design.md, раздел 1) — без EnvConstants и брокера.
 */
public class KafkaClusterConfigTest {

    private static final Path RESOURCES = Path.of("/res");

    /** Источник из Map; «расшифровка» помечает значение, чтобы проверить, что расшифровываются только пароли. */
    private static Source source(Map<String, String> values) {
        return new Source() {
            @Override
            public String get(String key) {
                return values.get(key);
            }

            @Override
            public String decrypt(String value) {
                return "dec(" + value + ")";
            }
        };
    }

    private static final Map<String, String> TWO_CLUSTERS = Map.ofEntries(
            Map.entry("kafka.batm.bootstrap.servers", "batm1:9092,batm2:9092"),
            Map.entry("kafka.batm.ssl.truststore.type", "PKCS12"),
            Map.entry("kafka.batm.ssl.truststore.password", "enc-trust"),
            Map.entry("kafka.batm.ssl.keystore.type", "PKCS12"),
            Map.entry("kafka.batm.ssl.keystore.password", "enc-key"),
            Map.entry("kafka.uasp.bootstrap.servers", "uasp1:9093"),
            Map.entry("kafka.uasp.topic.prefix", "ift_"));

    @Test(groups = {"selftest"}, description = "Два кластера: у каждого свой bootstrap, без предупреждений")
    public void parsesTwoClusters() {
        KafkaClusterConfig batm = resolve("batm", source(TWO_CLUSTERS), RESOURCES, "test");
        KafkaClusterConfig uasp = resolve("uasp", source(TWO_CLUSTERS), RESOURCES, "test");
        KafkaClusterConfig ods = resolve("ods", source(TWO_CLUSTERS), RESOURCES, "test");

        assertTrue(batm.isEnabled());
        assertEquals(batm.getBootstrapServers(), "batm1:9092,batm2:9092");
        assertEquals(batm.getTopicPrefix(), "");
        assertTrue(batm.getWarnings().isEmpty(), batm.getWarnings().toString());

        assertTrue(uasp.isEnabled());
        assertEquals(uasp.getBootstrapServers(), "uasp1:9093");
        assertEquals(uasp.getTopicPrefix(), "ift_");

        assertFalse(ods.isEnabled(), "кластер без bootstrap выключен");
        IllegalStateException e = expectThrows(IllegalStateException.class, ods::toClientProperties);
        assertTrue(e.getMessage().contains("kafka.ods.bootstrap.servers"), e.getMessage());
    }

    @Test(groups = {"selftest"}, description = "SSL основного кластера: пароли расшифрованы, location по умолчанию от cenv")
    public void mainClusterSsl() {
        KafkaClusterConfig batm = resolve("batm", source(TWO_CLUSTERS), RESOURCES, "test");
        assertEquals(batm.getSsl(), Map.of(
                TRUSTSTORE_TYPE, "PKCS12",
                TRUSTSTORE_PASSWORD, "dec(enc-trust)",
                TRUSTSTORE_LOCATION, RESOURCES.resolve("kafka/test-batm-truststore.p12").toString(),
                KEYSTORE_TYPE, "PKCS12",
                KEYSTORE_PASSWORD, "dec(enc-key)",
                KEYSTORE_LOCATION, RESOURCES.resolve("kafka/test-batm-keystore.p12").toString()));

        Properties client = batm.toClientProperties();
        assertEquals(client.get(CommonClientConfigs.BOOTSTRAP_SERVERS_CONFIG), "batm1:9092,batm2:9092");
        assertEquals(client.get(CommonClientConfigs.SECURITY_PROTOCOL_CONFIG), "SSL");
        assertEquals(client.get(SslConfigs.SSL_TRUSTSTORE_PASSWORD_CONFIG), "dec(enc-trust)");
        assertEquals(client.get(SslConfigs.SSL_KEYSTORE_LOCATION_CONFIG), RESOURCES.resolve("kafka/test-batm-keystore.p12").toString());
    }

    @Test(groups = {"selftest"}, description = "Кластер без своих ssl.* наследует все SSL-настройки основного")
    public void sslIsInheritedFromMainCluster() {
        KafkaClusterConfig batm = resolve("batm", source(TWO_CLUSTERS), RESOURCES, "test");
        KafkaClusterConfig uasp = resolve("uasp", source(TWO_CLUSTERS), RESOURCES, "test");
        assertEquals(uasp.getSsl(), batm.getSsl());
    }

    @Test(groups = {"selftest"}, description = "Свой ssl.* кластера перекрывает унаследованный, остальное наследуется")
    public void ownSslOverridesInherited() {
        Map<String, String> values = new java.util.HashMap<>(TWO_CLUSTERS);
        values.put("kafka.uasp.ssl.truststore.location", "kafka/test-uasp-truststore.p12");
        values.put("kafka.uasp.ssl.truststore.password", "enc-uasp");
        KafkaClusterConfig uasp = resolve("uasp", source(values), RESOURCES, "test");

        assertEquals(uasp.getSsl().get(TRUSTSTORE_LOCATION), RESOURCES.resolve("kafka/test-uasp-truststore.p12").toString());
        assertEquals(uasp.getSsl().get(TRUSTSTORE_PASSWORD), "dec(enc-uasp)");
        // не заданные у uasp — от batm
        assertEquals(uasp.getSsl().get(TRUSTSTORE_TYPE), "PKCS12");
        assertEquals(uasp.getSsl().get(KEYSTORE_PASSWORD), "dec(enc-key)");
        assertEquals(uasp.getSsl().get(KEYSTORE_LOCATION), RESOURCES.resolve("kafka/test-batm-keystore.p12").toString());
    }

    @Test(groups = {"selftest"}, description = "Старые ключи без префикса работают с предупреждением (переходный период)")
    public void legacyKeysWorkWithWarning() {
        Map<String, String> legacy = Map.of(
                "bootstrap.servers", "old:9092",
                "ssl.truststore.type", "PKCS12",
                "ssl.truststore.password", "enc-trust",
                "ssl.keystore.type", "PKCS12",
                "ssl.keystore.password", "enc-key",
                "kafka.uasp.bootstrap.servers", "uasp1:9093");
        KafkaClusterConfig batm = resolve("batm", source(legacy), RESOURCES, "test");
        assertEquals(batm.getBootstrapServers(), "old:9092");
        assertEquals(batm.getSsl().get(KEYSTORE_PASSWORD), "dec(enc-key)");
        assertEquals(batm.getWarnings().size(), 5, batm.getWarnings().toString());
        assertTrue(batm.getWarnings().get(0).contains("'bootstrap.servers'")
                && batm.getWarnings().get(0).contains("'kafka.batm.bootstrap.servers'"), batm.getWarnings().get(0));

        // другие кластеры наследуют SSL и со старых ключей, предупреждения — только у основного
        KafkaClusterConfig uasp = resolve("uasp", source(legacy), RESOURCES, "test");
        assertEquals(uasp.getSsl(), batm.getSsl());
        assertTrue(uasp.getWarnings().isEmpty());
    }

    @Test(groups = {"selftest"}, description = "Новый и старый ключ с одинаковым значением — без ошибки, берётся новый")
    public void sameValueInBothKeysIsAllowed() {
        Map<String, String> values = new java.util.HashMap<>(TWO_CLUSTERS);
        values.put("bootstrap.servers", "batm1:9092,batm2:9092");
        KafkaClusterConfig batm = resolve("batm", source(values), RESOURCES, "test");
        assertEquals(batm.getBootstrapServers(), "batm1:9092,batm2:9092");
        assertTrue(batm.getWarnings().isEmpty());
    }

    @Test(groups = {"selftest"}, description = "Новый и старый ключ с разными значениями — ошибка с именами ключей")
    public void conflictingKeysFail() {
        Map<String, String> values = new java.util.HashMap<>(TWO_CLUSTERS);
        values.put("ssl.keystore.password", "other");
        IllegalStateException e = expectThrows(IllegalStateException.class,
                () -> resolve("batm", source(values), RESOURCES, "test"));
        assertTrue(e.getMessage().contains("'kafka.batm.ssl.keystore.password'") && e.getMessage().contains("'ssl.keystore.password'"),
                e.getMessage());
    }

    @Test(groups = {"selftest"}, description = "Репозиторный test.properties уже на новых ключах: batm настроен без предупреждений")
    public void repositoryTestPropertiesUseNewKeys() throws IOException {
        Properties properties = new Properties();
        try (FileReader reader = new FileReader("src/main/resources/properties/test.properties")) {
            properties.load(reader);
        }
        Source fileSource = new Source() {
            @Override
            public String get(String key) {
                return properties.getProperty(key);
            }

            @Override
            public String decrypt(String value) {
                return value;
            }
        };
        KafkaClusterConfig batm = resolve("batm", fileSource, RESOURCES, "test");
        assertTrue(batm.isEnabled());
        assertTrue(batm.getWarnings().isEmpty(), batm.getWarnings().toString());
        for (String legacyKey : new String[]{"bootstrap.servers", "ssl.truststore.type", "ssl.truststore.password",
                "ssl.keystore.type", "ssl.keystore.password"}) {
            assertNull(properties.getProperty(legacyKey), "в test.properties остался старый ключ " + legacyKey);
        }
        assertFalse(resolve("uasp", fileSource, RESOURCES, "test").isEnabled(), "UASP на ift пока не настроен");
    }
}
