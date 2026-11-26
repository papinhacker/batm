package ru.vtb.selftest;

import org.joda.time.DateTime;
import org.joda.time.DateTimeUtils;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;
import ru.vtb.core.data.static_data.B2LMask;
import ru.vtb.core.utils.B2LHelper;

import java.util.HashSet;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.testng.Assert.*;

/**
 * Характеризационные тесты: фиксируют ТЕКУЩЕЕ поведение B2LHelper.generateBase2LikeFileName / substringFileMask.
 */
public class B2LHelperCharacterizationTest {

    private static final int ITERATIONS = 2000;

    @AfterMethod(alwaysRun = true)
    public void resetJodaClock() {
        DateTimeUtils.setCurrentMillisSystem();
    }

    @DataProvider
    public Object[][] masks() {
        B2LMask[] values = B2LMask.values();
        Object[][] data = new Object[values.length][];
        for (int i = 0; i < values.length; i++) {
            data[i] = new Object[]{values[i]};
        }
        return data;
    }

    @Test(groups = {"selftest"}, dataProvider = "masks",
            description = "Имя файла = маска + префикс 01-05 или 21-25 + '.' + трёхзначный день года")
    public void generatedNameHasMaskPrefixAndDayOfYear(B2LMask mask) {
        Pattern pattern = Pattern.compile(Pattern.quote(mask.getName()) + "(\\d{2})\\.(\\d{3})");
        int todayDayOfYear = new DateTime().getDayOfYear();
        Set<String> prefixes = new HashSet<>();
        for (int i = 0; i < ITERATIONS; i++) {
            String name = B2LHelper.generateBase2LikeFileName(mask);
            Matcher m = pattern.matcher(name);
            assertTrue(m.matches(), "Неожиданный формат имени файла: " + name);
            String prefix = m.group(1);
            assertTrue(prefix.matches("0[1-5]|2[1-5]"), "Префикс должен быть 01-05 или 21-25: " + name);
            assertNotEquals(prefix, "06", "Префикс 06 не генерируется: " + name);
            assertNotEquals(prefix, "26", "Префикс 26 не генерируется: " + name);
            int day = Integer.parseInt(m.group(2));
            // День года всегда строго раньше сегодняшнего и в пределах текущего года.
            assertTrue(day >= 1 && day < todayDayOfYear,
                    "День года %d должен быть в [1, %d): %s".formatted(day, todayDayOfYear, name));
            prefixes.add(prefix);
        }
        // Обе группы префиксов реально выпадают (выбор группы 50/50, внутри группы равномерно).
        assertTrue(prefixes.stream().anyMatch(p -> p.startsWith("0")), "Не выпал ни один префикс 01-05: " + prefixes);
        assertTrue(prefixes.stream().anyMatch(p -> p.startsWith("2")), "Не выпал ни один префикс 21-25: " + prefixes);
        assertEquals(prefixes, Set.of("01", "02", "03", "04", "05", "21", "22", "23", "24", "25"),
                "За %d итераций должны выпасть все 10 префиксов".formatted(ITERATIONS));
    }

    @Test(groups = {"selftest"}, description = "2 января единственный возможный день — 001")
    public void secondOfJanuaryAlwaysGivesDay001() {
        DateTimeUtils.setCurrentMillisFixed(new DateTime(2027, 1, 2, 12, 0).getMillis());
        for (int i = 0; i < 100; i++) {
            assertTrue(B2LHelper.generateBase2LikeFileName(B2LMask.D_038cto).endsWith(".001"));
        }
    }

    // KNOWN-BUG: 1 января CommonHelper.getNumericDayOfYearBeforeCurrentDate вызывает
    // ThreadLocalRandom.nextInt(1, dayOfYear) = nextInt(1, 1) -> IllegalArgumentException,
    // т.е. генерация имени файла без postfix падает весь день 1 января.
    @Test(groups = {"selftest"}, description = "1 января генерация имени без postfix падает (KNOWN-BUG)")
    public void firstOfJanuaryThrows() {
        DateTimeUtils.setCurrentMillisFixed(new DateTime(2027, 1, 1, 12, 0).getMillis());
        IllegalArgumentException e = expectThrows(IllegalArgumentException.class,
                () -> B2LHelper.generateBase2LikeFileName(B2LMask.D_038cto));
        assertEquals(e.getMessage(), "bound must be greater than origin");
    }

    @Test(groups = {"selftest"}, description = "С postfix к маске дописывается ровно postfix, без '.ДДД'")
    public void postfixReplacesWholeSuffix() {
        assertEquals(B2LHelper.generateBase2LikeFileName(B2LMask.D_038cto, "01.330"), "D_038cto01.330");
        // Из-за приоритета тернарного оператора '.ДДД' дописывается только к случайному префиксу.
        assertEquals(B2LHelper.generateBase2LikeFileName(B2LMask._298cto, "03"), "298cto03");
        assertEquals(B2LHelper.generateBase2LikeFileName(B2LMask.CORP_038cto, ""), "CORP_038cto");
        // Используется только первый postfix, остальные игнорируются.
        assertEquals(B2LHelper.generateBase2LikeFileName(B2LMask._038cts, "21.100", "22.200"), "038cts21.100");
    }

    // KNOWN-BUG: postfix не проверяется — через него можно получить префикс 06/26 (и любой мусор),
    // хотя случайная генерация такие префиксы не выдаёт.
    @Test(groups = {"selftest"}, description = "postfix не валидируется (KNOWN-BUG)")
    public void postfixIsNotValidated() {
        assertEquals(B2LHelper.generateBase2LikeFileName(B2LMask.D_038cto, "06.001"), "D_038cto06.001");
        assertEquals(B2LHelper.generateBase2LikeFileName(B2LMask.D_038cto, "26.999"), "D_038cto26.999");
        assertEquals(B2LHelper.generateBase2LikeFileName(B2LMask.D_038cto, "xx"), "D_038ctoxx");
    }

    @Test(groups = {"selftest"}, dataProvider = "masks",
            description = "substringFileMask отрезает сгенерированный суффикс, recognize возвращает исходную маску")
    public void substringFileMaskRoundTrip(B2LMask mask) {
        for (int i = 0; i < 200; i++) {
            String name = B2LHelper.generateBase2LikeFileName(mask);
            assertEquals(B2LHelper.substringFileMask(name), mask.getName(), "Имя: " + name);
            assertEquals(B2LMask.recognize(name), mask, "Имя: " + name);
        }
    }

    @Test(groups = {"selftest"}, description = "substringFileMask удаляет только хвост вида 'ДД.ДДД' в конце строки")
    public void substringFileMaskExamples() {
        assertEquals(B2LHelper.substringFileMask("D_038cto05.123"), "D_038cto");
        assertEquals(B2LHelper.substringFileMask("CORP_038ctd21.001"), "CORP_038ctd");
        assertEquals(B2LHelper.substringFileMask("298cto25.366"), "298cto");
        // Префикс не проверяется: 06/26/99 отрезаются так же.
        assertEquals(B2LHelper.substringFileMask("D_038cto06.001"), "D_038cto");
        assertEquals(B2LHelper.substringFileMask("D_038cto26.001"), "D_038cto");
        assertEquals(B2LHelper.substringFileMask("D_038cto99.999"), "D_038cto");
        // Без суффикса и с суффиксом не того формата строка не меняется.
        assertEquals(B2LHelper.substringFileMask("D_038cto"), "D_038cto");
        assertEquals(B2LHelper.substringFileMask("D_038cto5.123"), "D_038cto5.123");
        assertEquals(B2LHelper.substringFileMask("D_038cto05.12"), "D_038cto05.12");
        assertEquals(B2LHelper.substringFileMask("D_038cto05.123.xml"), "D_038cto05.123.xml");
        // Отрезаются только последние 6 символов: лишние цифры перед ними остаются частью маски.
        assertEquals(B2LHelper.substringFileMask("D_038cto105.123"), "D_038cto1");
        assertEquals(B2LHelper.substringFileMask("05.123"), "");
    }

    @Test(groups = {"selftest"}, description = "recognize падает на имени без известной маски")
    public void recognizeUnknownMaskThrows() {
        RuntimeException e = expectThrows(RuntimeException.class, () -> B2LMask.recognize("base2like"));
        assertEquals(e.getMessage(), "Unable to recognize file mask of file - base2like");
    }
}
