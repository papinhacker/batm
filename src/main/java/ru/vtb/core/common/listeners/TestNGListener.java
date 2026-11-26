package ru.vtb.core.common.listeners;

import io.qameta.allure.Allure;
import org.testng.*;
import org.testng.annotations.ITestAnnotation;
import org.testng.xml.XmlSuite;
import ru.vtb.core.common.Log;
import ru.vtb.core.common.RetryAnalyzer;
import ru.vtb.core.data.static_data.KafkaCluster;
import ru.vtb.core.data.static_data.KafkaTopic;
import ru.vtb.core.utils.KafkaClient;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.List;

public class TestNGListener implements ITestListener, ISuiteListener, IInvokedMethodListener, IReporter, IAnnotationTransformer {

    protected static int threadCount;

    static {
        threadCount = parseThreadCount(System.getProperty("threadCount"));
    }

    /**
     * @param value значение свойства -DthreadCount
     * @return число потоков; 1, если свойство не задано, или (с предупреждением в лог) если значение не число
     */
    public static int parseThreadCount(String value) {
        if (value == null || value.isBlank()) {
            return 1;
        }
        String trimmed = value.trim();
        // не больше 9 цифр — Integer.parseInt не переполнится
        if (trimmed.matches("\\d{1,9}")) {
            return Integer.parseInt(trimmed);
        }
        Log.warning("Некорректное значение -DthreadCount='%s': ожидается целое число, тесты будут запущены в 1 поток"
                .formatted(value));
        return 1;
    }

    @Override
    public void onStart(ISuite suite) {
        String suiteName = suite.getName();
        Log.info("Log level is '%s'".formatted(System.getProperty("loglevel")));
        Log.info("Начинаем запуск набора тестов: " + suiteName);
        System.setProperty("suitename", suiteName);
        XmlSuite xmlSuite = suite.getXmlSuite();
        xmlSuite.setName(suiteName);
        if (threadCount > 1 && !suiteName.toLowerCase().contains("singleThread".toLowerCase())) {
            xmlSuite.setParallel(XmlSuite.ParallelMode.TESTS);
            xmlSuite.setThreadCount(threadCount);
            xmlSuite.setPreserveOrder(true);
        }
        Log.info("Тесты будут запущенны в '%s' поток(а/ов)".formatted(String.valueOf(xmlSuite.getThreadCount())));
        initKafkaConsumers();
    }

    @Override
    public void onTestFailure(ITestResult result) {
        Allure.addAttachment(
                "Test console log",
                "text/plain",
                String.join("\n", Reporter.getOutput(result)));
    }

    @Override
    public void transform(ITestAnnotation annotation, Class testClass, Constructor testConstructor, Method testMethod) {
        annotation.setRetryAnalyzer(RetryAnalyzer.class);
    }

    private void initKafkaConsumers() {
        for (KafkaCluster cluster : KafkaCluster.values()) {
            List<String> topics = KafkaTopic.getTopicsNames(cluster);
            if (topics.isEmpty()) {
                continue;
            }
            if (cluster.isConfigured()) {
                KafkaClient.startReadingTopics(cluster, topics);
            } else {
                Log.warning("Kafka-кластер '%s' не настроен (пустое свойство '%s'), топики %s читаться не будут"
                        .formatted(cluster, cluster.getBootstrapServersPropertyKey(), topics));
            }
        }
    }
}
