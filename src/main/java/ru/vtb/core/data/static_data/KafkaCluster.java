package ru.vtb.core.data.static_data;

import org.apache.commons.lang3.StringUtils;
import ru.vtb.core.common.EnvConstants;

/**
 * Kafka-кластеры, с которыми работает проект.
 * <p>
 * SSL-настройки (truststore/keystore и пароли) у всех кластеров общие — берутся из основной кафки BATM.
 * Кластер считается настроенным, если для него задан bootstrap.servers.
 */
public enum KafkaCluster {

    BATM("bootstrap.servers"),
    UASP("kafka.uasp.bootstrap.servers"),
    ODS("kafka.ods.bootstrap.servers");

    private final String bootstrapServersPropertyKey;

    KafkaCluster(String bootstrapServersPropertyKey) {
        this.bootstrapServersPropertyKey = bootstrapServersPropertyKey;
    }

    public String getBootstrapServersPropertyKey() {
        return this.bootstrapServersPropertyKey;
    }

    public String getBootstrapServers() {
        return switch (this) {
            case BATM -> EnvConstants.BOOTSTRAP_SERVERS;
            case UASP -> EnvConstants.UASP_BOOTSTRAP_SERVERS;
            case ODS -> EnvConstants.ODS_BOOTSTRAP_SERVERS;
        };
    }

    public boolean isConfigured() {
        return StringUtils.isNotBlank(getBootstrapServers());
    }
}
