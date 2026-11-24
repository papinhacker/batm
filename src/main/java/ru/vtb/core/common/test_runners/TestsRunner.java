package ru.vtb.core.common.test_runners;

import org.testng.TestNG;
import ru.vtb.core.utils.TestRunnerUtils;

import java.util.List;

public class TestsRunner {

    public static void main(String[] args) {
        TestRunnerUtils.unpackResources();

        TestNG testNG = new TestNG();
        testNG.setTestSuites(List.of(args[0]));
        //todo добавить предпрогон подготовки окружения
        testNG.run();
        System.exit(0);
    }
}
