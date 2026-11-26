package ru.vtb.core.common.test_runners;

import org.testng.TestNG;
import ru.vtb.core.utils.TestRunnerUtils;

import java.util.List;

public class TestsRunner {

    /**
     * Код возврата при отсутствии обязательного аргумента (путь к XML-сьюту).
     */
    static final int EXIT_CODE_USAGE = 2;

    public static void main(String[] args) {
        if (args.length == 0 || args[0] == null || args[0].isBlank()) {
            System.err.println("Не указан путь к XML-сьюту TestNG: первый аргумент обязателен. "
                    + "Пример: java -Dcenv=test -jar auto-testing-1.0-fat-tests.jar TestXML/API/batm_acc/BatmAcc.xml");
            System.exit(EXIT_CODE_USAGE);
        }
        TestRunnerUtils.unpackResources();

        TestNG testNG = new TestNG();
        testNG.setTestSuites(List.of(args[0]));
        //todo добавить предпрогон подготовки окружения
        testNG.run();
        // System.exit обязателен: потоки чтения Kafka не daemon и иначе держат JVM.
        // getStatus(): 0 — всё прошло; иначе битовая маска TestNG (1 — падения, 2 — пропуски, 8 — тестов не найдено).
        System.exit(testNG.getStatus());
    }
}
