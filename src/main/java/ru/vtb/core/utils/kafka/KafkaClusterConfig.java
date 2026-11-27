package ru.vtb.core.utils.kafka;

import org.apache.kafka.clients.CommonClientConfigs;
import org.apache.kafka.common.config.SslConfigs;
import org.apache.kafka.common.security.auth.SecurityProtocol;
import ru.vtb.core.common.EnvConstants;
import ru.vtb.core.common.Log;
import ru.vtb.core.data.static_data.KafkaCluster;

import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Настройки Kafka-кластера из properties (docs/kafka-design.md, раздел 1).
 * <pre>
 * kafka.&lt;имя&gt;.bootstrap.servers           — обязателен; пусто — кластер выключен
 * kafka.&lt;имя&gt;.ssl.truststore.type|password|location
 * kafka.&lt;имя&gt;.ssl.keystore.type|password|location — необязательны; нет — берутся у основного кластера (batm)
 * kafka.&lt;имя&gt;.topic.prefix                  — необязателен; префикс имён топиков кластера на контуре
 * </pre>
 * Основной кластер (batm) на переходный период читает и старые ключи без префикса ({@code bootstrap.servers},
 * {@code ssl.*}) с предупреждением; если заданы и новый, и старый ключ с разными значениями — ошибка.
 * Location — путь относительно src/main/resources; по умолчанию {@code kafka/<cenv>-batm-truststore.p12}
 * и {@code kafka/<cenv>-batm-keystore.p12}, как было.
 */
public final class KafkaClusterConfig {

    /** Источник свойств: значение ключа (или null) и расшифровка паролей. */
    public interface Source {
        String get(String key);

        String decrypt(String value);
    }

    public static final String MAIN_CLUSTER = "batm";
    public static final String BOOTSTRAP_SERVERS = "bootstrap.servers";
    public static final String TRUSTSTORE_TYPE = "ssl.truststore.type";
    public static final String TRUSTSTORE_PASSWORD = "ssl.truststore.password";
    public static final String TRUSTSTORE_LOCATION = "ssl.truststore.location";
    public static final String KEYSTORE_TYPE = "ssl.keystore.type";
    public static final String KEYSTORE_PASSWORD = "ssl.keystore.password";
    public static final String KEYSTORE_LOCATION = "ssl.keystore.location";
    public static final String TOPIC_PREFIX = "topic.prefix";

    /** Ключи основного кластера, у которых есть старое имя без префикса. */
    static final Set<String> LEGACY_KEYS = Set.of(BOOTSTRAP_SERVERS, TRUSTSTORE_TYPE, TRUSTSTORE_PASSWORD,
            KEYSTORE_TYPE, KEYSTORE_PASSWORD);
    private static final List<String> SSL_KEYS = List.of(TRUSTSTORE_TYPE, TRUSTSTORE_PASSWORD, TRUSTSTORE_LOCATION,
            KEYSTORE_TYPE, KEYSTORE_PASSWORD, KEYSTORE_LOCATION);
    private static final Set<String> PASSWORD_KEYS = Set.of(TRUSTSTORE_PASSWORD, KEYSTORE_PASSWORD);

    private static final Map<KafkaCluster, KafkaClusterConfig> CACHE = new ConcurrentHashMap<>();

    private final String cluster;
    private final String bootstrapServers;
    private final Map<String, String> ssl;
    private final String topicPrefix;
    private final List<String> warnings;

    private KafkaClusterConfig(String cluster, String bootstrapServers, Map<String, String> ssl, String topicPrefix,
                               List<String> warnings) {
        this.cluster = cluster;
        this.bootstrapServers = bootstrapServers;
        this.ssl = Collections.unmodifiableMap(ssl);
        this.topicPrefix = topicPrefix;
        this.warnings = List.copyOf(warnings);
    }

    /**
     * Настройки кластера из properties текущего окружения (-Dcenv). Предупреждения о старых ключах пишутся в лог один раз.
     */
    public static KafkaClusterConfig of(KafkaCluster cluster) {
        return CACHE.computeIfAbsent(cluster, c -> {
            KafkaClusterConfig config = resolve(c.getConfigName(), new Source() {
                @Override
                public String get(String key) {
                    return EnvConstants.getProperty(key);
                }

                @Override
                public String decrypt(String value) {
                    return EnvConstants.decrypt(value);
                }
            }, Path.of(EnvConstants.MAIN_RESOURCES_FOLDER), EnvConstants.getEnvironmentName());
            config.getWarnings().forEach(Log::warning);
            return config;
        });
    }

    /**
     * @param cluster      имя кластера в ключах (batm, uasp, ...)
     * @param source       источник свойств
     * @param resourcesDir каталог, относительно которого задаются location хранилищ
     * @param environment  имя окружения (для location по умолчанию)
     */
    public static KafkaClusterConfig resolve(String cluster, Source source, Path resourcesDir, String environment) {
        List<String> warnings = new ArrayList<>();
        boolean main = MAIN_CLUSTER.equals(cluster);
        String bootstrap = main
                ? mainValue(BOOTSTRAP_SERVERS, source, warnings)
                : blankToNull(source.get(key(cluster, BOOTSTRAP_SERVERS)));

        Map<String, String> ssl = new LinkedHashMap<>();
        for (String sslKey : SSL_KEYS) {
            String value = main ? null : blankToNull(source.get(key(cluster, sslKey)));
            if (value == null) {
                // основной кластер: свой ключ (или старый); остальные — наследуют от основного
                value = mainValue(sslKey, source, main ? warnings : new ArrayList<>());
            }
            if (value == null && sslKey.equals(TRUSTSTORE_LOCATION)) {
                value = "kafka/%s-batm-truststore.p12".formatted(environment);
            }
            if (value == null && sslKey.equals(KEYSTORE_LOCATION)) {
                value = "kafka/%s-batm-keystore.p12".formatted(environment);
            }
            if (value != null && (sslKey.equals(TRUSTSTORE_LOCATION) || sslKey.equals(KEYSTORE_LOCATION))) {
                value = resourcesDir.resolve(value).toString();
            }
            if (value != null && PASSWORD_KEYS.contains(sslKey)) {
                value = source.decrypt(value);
            }
            if (value != null) {
                ssl.put(sslKey, value);
            }
        }
        String prefix = Objects.requireNonNullElse(blankToNull(source.get(key(cluster, TOPIC_PREFIX))), "");
        return new KafkaClusterConfig(cluster, bootstrap, ssl, prefix, warnings);
    }

    /**
     * Значение ключа основного кластера: kafka.batm.X, а на переходный период — старый ключ X с предупреждением.
     */
    private static String mainValue(String suffix, Source source, List<String> warnings) {
        String newKey = key(MAIN_CLUSTER, suffix);
        String value = blankToNull(source.get(newKey));
        if (!LEGACY_KEYS.contains(suffix)) {
            return value;
        }
        String legacy = blankToNull(source.get(suffix));
        if (value != null && legacy != null && !value.equals(legacy)) {
            throw new IllegalStateException(("Заданы оба ключа '%s' и устаревший '%s' с разными значениями: "
                    + "удалите устаревший ключ (docs/preprod-migration.md)").formatted(newKey, suffix));
        }
        if (value == null && legacy != null) {
            warnings.add("Устаревший ключ '%s' в properties: переименуйте его в '%s' (docs/preprod-migration.md)"
                    .formatted(suffix, newKey));
            return legacy;
        }
        return value;
    }

    public static String key(String cluster, String suffix) {
        return "kafka.%s.%s".formatted(cluster, suffix);
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    public String getCluster() {
        return cluster;
    }

    public boolean isEnabled() {
        return bootstrapServers != null;
    }

    public String getBootstrapServers() {
        return bootstrapServers;
    }

    /** SSL-настройки после наследования: ключи ssl.truststore.*, ssl.keystore.*; пароли расшифрованы. */
    public Map<String, String> getSsl() {
        return ssl;
    }

    public String getTopicPrefix() {
        return topicPrefix;
    }

    /** Предупреждения о старых ключах, найденные при чтении. */
    public List<String> getWarnings() {
        return warnings;
    }

    /**
     * @return свойства клиента Kafka: bootstrap.servers, протокол SSL и SSL-настройки
     */
    public Properties toClientProperties() {
        if (!isEnabled()) {
            throw new IllegalStateException("Kafka-кластер '%s' не настроен: пустое свойство '%s'"
                    .formatted(cluster, key(cluster, BOOTSTRAP_SERVERS)));
        }
        Properties properties = new Properties();
        properties.put(CommonClientConfigs.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        properties.put(CommonClientConfigs.SECURITY_PROTOCOL_CONFIG, SecurityProtocol.SSL.name);
        put(properties, SslConfigs.SSL_TRUSTSTORE_TYPE_CONFIG, ssl.get(TRUSTSTORE_TYPE));
        put(properties, SslConfigs.SSL_TRUSTSTORE_PASSWORD_CONFIG, ssl.get(TRUSTSTORE_PASSWORD));
        put(properties, SslConfigs.SSL_TRUSTSTORE_LOCATION_CONFIG, ssl.get(TRUSTSTORE_LOCATION));
        put(properties, SslConfigs.SSL_KEYSTORE_TYPE_CONFIG, ssl.get(KEYSTORE_TYPE));
        put(properties, SslConfigs.SSL_KEYSTORE_PASSWORD_CONFIG, ssl.get(KEYSTORE_PASSWORD));
        put(properties, SslConfigs.SSL_KEYSTORE_LOCATION_CONFIG, ssl.get(KEYSTORE_LOCATION));
        return properties;
    }

    private static void put(Properties properties, String key, String value) {
        if (value != null) {
            properties.put(key, value);
        }
    }
}
