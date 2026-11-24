package ru.vtb.core.common;

import io.qameta.allure.Allure;
import org.testng.Reporter;

import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Класс для вывода логов, как в коносль, так и в аллюр, и в логи TestNG
 * <p>
 * Уровни логирования:
 * none = 0
 * error = 1
 * step = 10
 * warning = 25
 * debug = 30
 * trace = 100
 */
public class Log {

    private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("(z) HH.mm.ss:SSS");
    private static final int LOG_LEVEL;
    private static final ThreadLocal<Boolean> suppressed = ThreadLocal.withInitial(() -> false);

    static {
        String level = System.getProperty("loglevel");
        if (level == null || level.isEmpty()) {
            LOG_LEVEL = 25;
        } else {
            switch (level) {
                case "none" -> LOG_LEVEL = 0;
                case "error" -> LOG_LEVEL = 1;
                case "step" -> LOG_LEVEL = 10;
                case "debug" -> LOG_LEVEL = 30;
                case "trace" -> LOG_LEVEL = 100;
                default -> LOG_LEVEL = 25;
            }
        }
    }

    private Log() {
    }

    public static void error(Object message) {
        if (LOG_LEVEL >= 1) {
            Reporter.log("[ERROR] " + DATE_FORMAT.format(new Date())
                    + " - " + Thread.currentThread().getStackTrace()[2].getClassName()
                    + " [ERROR] " + message.toString(), true);
        }
    }

    public static void step(Object message) {
        if (LOG_LEVEL >= 10 && !suppressed.get()) {
            Reporter.log("[STEP] " + DATE_FORMAT.format(new Date())
                    + " - " + Thread.currentThread().getStackTrace()[2].getClassName()
                    + " [STEP] " + message.toString(), true);
        }
        Allure.step(message.toString());
    }

    public static void assertFail(Object message) {
        if (LOG_LEVEL >= 11) {
            Reporter.log("[ASSERTION FAILURE] " + DATE_FORMAT.format(new Date())
                    + " - " + Thread.currentThread().getStackTrace()[2].getClassName()
                    + " [ASSERTION FAILURE] " + message.toString(), true);
        }
    }

    public static void warning(Object message) {
        if (LOG_LEVEL >= 25) {
            Reporter.log("[WARNING] " + DATE_FORMAT.format(new Date())
                    + " - " + Thread.currentThread().getStackTrace()[2].getClassName()
                    + " [WARNING] " + message.toString(), true);
        }
    }

    public static void debug(Object message) {
        if (LOG_LEVEL >= 30) {
            Reporter.log("[DEBUG] " + DATE_FORMAT.format(new Date())
                    + " - " + Thread.currentThread().getStackTrace()[2].getClassName()
                    + " [DEBUG] " + message.toString(), true);
        }
    }

    public static void trace(Object message) {
        if (LOG_LEVEL >= 100) {
            Reporter.log("[TRACE] " + DATE_FORMAT.format(new Date())
                    + " - " + Thread.currentThread().getStackTrace()[2].getClassName()
                    + " [TRACE] " + message.toString(), true);
        }
    }

    public static void info(Object message) {
        if (LOG_LEVEL != 0 && !suppressed.get()) {
            Reporter.log("[INFO] " + DATE_FORMAT.format(new Date())
                    + " - " + Thread.currentThread().getStackTrace()[2].getClassName()
                    + " [INFO] " + message.toString(), true);
        }
    }

    public static void suppress() {
        suppressed.set(true);
    }

    public static void restore() {
        suppressed.set(false);
        suppressed.remove();
    }
}
