package ru.vtb.selftest.kafka;

import org.apache.kafka.clients.CommonClientConfigs;
import org.apache.kafka.clients.admin.Admin;
import org.apache.kafka.clients.admin.NewPartitions;
import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.awaitility.Awaitility;
import org.testcontainers.kafka.KafkaContainer;
import org.testcontainers.utility.DockerImageName;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import ru.vtb.core.utils.kafka.KafkaClusterRuntime;
import ru.vtb.core.utils.kafka.KafkaMessage;
import ru.vtb.core.utils.kafka.TopicStore;
import ru.vtb.selftest.support.DockerSupport;

import java.time.Clock;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static org.testng.Assert.*;

/**
 * KafkaClusterRuntime на настоящем брокере (Testcontainers, apache/kafka). Без Docker тесты пропускаются.
 */
public class KafkaClusterRuntimeContainerTest {

    private static final String IMAGE = "apache/kafka:3.9.1";
    private static final KafkaClusterRuntime.Settings SETTINGS =
            new KafkaClusterRuntime.Settings(10_000, Duration.ofMinutes(60), Duration.ofSeconds(2), Duration.ofMillis(200));

    private KafkaContainer kafka;

    @BeforeClass(alwaysRun = true)
    public void startKafka() {
        DockerSupport.requireDocker();
        kafka = new KafkaContainer(DockerImageName.parse(IMAGE));
        kafka.start();
    }

    @AfterClass(alwaysRun = true)
    public void stopKafka() {
        if (kafka != null) {
            kafka.stop();
        }
    }

    private Properties clientProperties() {
        Properties properties = new Properties();
        properties.put(CommonClientConfigs.BOOTSTRAP_SERVERS_CONFIG, kafka.getBootstrapServers());
        return properties;
    }

    private String createTopic(int partitions) throws Exception {
        String topic = "selftest-" + UUID.randomUUID();
        try (Admin admin = Admin.create(clientProperties())) {
            admin.createTopics(List.of(new NewTopic(topic, partitions, (short) 1))).all().get(30, TimeUnit.SECONDS);
        }
        return topic;
    }

    private static void send(KafkaClusterRuntime runtime, String topic, int partition, String value) throws Exception {
        runtime.producer().send(new ProducerRecord<>(topic, partition, "k" + partition, value)).get(30, TimeUnit.SECONDS);
    }

    private static Set<String> values(TopicStore store) {
        return store.messages().stream().map(KafkaMessage::value).collect(Collectors.toSet());
    }

    private static boolean consumerThreadAlive(String cluster) {
        return Thread.getAllStackTraces().keySet().stream()
                .anyMatch(t -> t.isAlive() && t.getName().equals("kafka-consumer-" + cluster));
    }

    @Test(groups = {"selftest"}, description = "seekToEnd: сообщения до старта не видны, после — видны во всех 3 партициях")
    public void readsOnlyMessagesAfterStart() throws Exception {
        String topic = createTopic(3);
        String cluster = "seek-" + topic.substring(9, 17);
        KafkaClusterRuntime runtime = new KafkaClusterRuntime(cluster, clientProperties(), List.of(topic), SETTINGS, Clock.systemUTC());
        try {
            for (int p = 0; p < 3; p++) {
                send(runtime, topic, p, "old-p" + p);
            }
            runtime.start();
            runtime.awaitReady(Duration.ofSeconds(60));
            for (int p = 0; p < 3; p++) {
                send(runtime, topic, p, "new-p" + p);
            }
            TopicStore store = runtime.store(topic);
            Awaitility.await().atMost(Duration.ofSeconds(30)).until(() -> store.size() >= 3);

            // старые сообщения стоят в партициях раньше новых: если бы чтение шло с начала, они уже были бы здесь
            assertEquals(values(store), Set.of("new-p0", "new-p1", "new-p2"));
            assertEquals(store.messages().stream().map(KafkaMessage::partition).collect(Collectors.toSet()), Set.of(0, 1, 2));
            assertTrue(store.messages().stream().allMatch(m -> m.offset() == 1), "у каждой партиции offset 1 — оба сообщения с равным offset хранятся");
            assertNull(runtime.getError());
        } finally {
            runtime.close();
        }
        assertFalse(consumerThreadAlive(cluster), "после close не должно оставаться потока consumer");
    }

    @Test(groups = {"selftest"}, description = "Новая партиция, появившаяся во время прогона, читается с начала")
    public void newPartitionIsReadFromBeginning() throws Exception {
        String topic = createTopic(3);
        String cluster = "newpart-" + topic.substring(9, 17);
        KafkaClusterRuntime runtime = new KafkaClusterRuntime(cluster, clientProperties(), List.of(topic), SETTINGS, Clock.systemUTC());
        try {
            runtime.start();
            runtime.awaitReady(Duration.ofSeconds(60));
            try (Admin admin = Admin.create(clientProperties())) {
                admin.createPartitions(Map.of(topic, NewPartitions.increaseTo(4))).all().get(30, TimeUnit.SECONDS);
            }
            // пишем в новую партицию сразу, до того как consumer её обнаружит
            send(runtime, topic, 3, "in-new-partition");
            TopicStore store = runtime.store(topic);
            Awaitility.await().atMost(Duration.ofSeconds(60)).until(() -> values(store).contains("in-new-partition"));
            assertEquals(store.find(m -> m.value().equals("in-new-partition"), TopicStore.Order.ANY).orElseThrow().partition(), 3);
        } finally {
            runtime.close();
        }
    }

    @Test(groups = {"selftest"}, description = "Несуществующий топик — понятная ошибка старта, без автосоздания")
    public void missingTopicFailsStartup() throws Exception {
        String topic = "selftest-missing-" + UUID.randomUUID();
        KafkaClusterRuntime runtime = new KafkaClusterRuntime("missing", clientProperties(), List.of(topic), SETTINGS, Clock.systemUTC());
        try {
            runtime.start();
            IllegalStateException e = expectThrows(IllegalStateException.class, () -> runtime.awaitReady(Duration.ofSeconds(90)));
            assertTrue(e.getMessage().contains(topic), e.getMessage());
        } finally {
            runtime.close();
        }
        try (Admin admin = Admin.create(clientProperties())) {
            assertFalse(admin.listTopics().names().get(30, TimeUnit.SECONDS).contains(topic), "топик не должен создаваться автоматически");
        }
    }
}
