package ru.vtb.core.data.static_data;

import ru.vtb.core.utils.kafka.KafkaClusterConfig;

/**
 * Kafka-кластеры, с которыми работает проект.
 * <p>
 * Кластер описывается только в properties (ключи {@code kafka.<имя>.*}, имя — {@link #getConfigName()}),
 * см. {@link KafkaClusterConfig}. Добавление кластера: строки в properties + константа здесь + топики в {@link KafkaTopic}.
 * Кластер считается настроенным, если для него задан bootstrap.servers.
 */
public enum KafkaCluster {

    BATM,
    UASP,
    ODS;

    /**
     * @return имя кластера в ключах properties: kafka.&lt;имя&gt;.*
     */
    public String getConfigName() {
        return name().toLowerCase();
    }

    public String getBootstrapServersPropertyKey() {
        return KafkaClusterConfig.key(getConfigName(), KafkaClusterConfig.BOOTSTRAP_SERVERS);
    }

    public String getBootstrapServers() {
        return KafkaClusterConfig.of(this).getBootstrapServers();
    }

    public boolean isConfigured() {
        return KafkaClusterConfig.of(this).isEnabled();
    }
}
