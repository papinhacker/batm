package ru.vtb.selftest;

import org.testng.annotations.Test;
import ru.vtb.core.common.listeners.TestNGListener;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNull;

/**
 * Регрессия: без -DthreadCount static-блок TestNGListener падал с NPE (ExceptionInInitializerError).
 */
public class TestNGListenerThreadCountTest {

    @Test(groups = {"selftest"}, description = "Класс загружается без -DthreadCount (selftest его не задаёт)")
    public void classInitializesWithoutThreadCountProperty() throws ClassNotFoundException {
        assertNull(System.getProperty("threadCount"), "selftest должен запускаться без -DthreadCount");
        // До исправления здесь было ExceptionInInitializerError (NPE в static-блоке)
        Class.forName(TestNGListener.class.getName(), true, getClass().getClassLoader());
    }

    @Test(groups = {"selftest"}, description = "Нет значения -> 1 поток")
    public void missingValueGivesOneThread() {
        assertEquals(TestNGListener.parseThreadCount(null), 1);
        assertEquals(TestNGListener.parseThreadCount(""), 1);
        assertEquals(TestNGListener.parseThreadCount("   "), 1);
    }

    @Test(groups = {"selftest"}, description = "Число -> это число потоков (пробелы по краям допускаются)")
    public void numericValueIsUsed() {
        assertEquals(TestNGListener.parseThreadCount("1"), 1);
        assertEquals(TestNGListener.parseThreadCount("4"), 4);
        assertEquals(TestNGListener.parseThreadCount(" 8 "), 8);
        assertEquals(TestNGListener.parseThreadCount("0"), 0, "0 — как и раньше: параллельность не включается");
    }

    @Test(groups = {"selftest"}, description = "Нечисловое или слишком большое значение -> предупреждение и 1 поток")
    public void invalidValueGivesOneThread() {
        assertEquals(TestNGListener.parseThreadCount("four"), 1);
        assertEquals(TestNGListener.parseThreadCount("-2"), 1);
        assertEquals(TestNGListener.parseThreadCount("2.5"), 1);
        assertEquals(TestNGListener.parseThreadCount("99999999999"), 1, "переполнение int не должно ронять загрузку");
    }
}
