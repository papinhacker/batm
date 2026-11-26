package ru.vtb.selftest;

import org.apache.commons.lang3.RandomStringUtils;
import org.testng.annotations.Test;
import ru.vtb.tools.SecretTool;

import static org.testng.Assert.*;

/**
 * SecretTool: encrypt -> decrypt возвращает исходную строку; ошибки ввода дают понятный текст.
 * Ключ и строка генерируются в тесте (DES — ключ из 8 байт).
 */
public class SecretToolTest {

    private static final String KEY = RandomStringUtils.insecure().nextAlphanumeric(8);

    @Test(groups = {"selftest"}, description = "encrypt -> decrypt возвращает исходную строку")
    public void roundTrip() {
        String value = "value-" + RandomStringUtils.insecure().nextAlphanumeric(20);
        String encrypted = SecretTool.run("encrypt", value, KEY);
        assertNotEquals(encrypted, value);
        assertEquals(SecretTool.run("decrypt", encrypted, KEY), value);
    }

    @Test(groups = {"selftest"}, description = "Без ключа — понятная ошибка с подсказкой про -Dekey")
    public void missingKeyFails() {
        IllegalArgumentException e = expectThrows(IllegalArgumentException.class, () -> SecretTool.run("encrypt", "x", null));
        assertTrue(e.getMessage().contains("-Dekey=<ключ>"), e.getMessage());
        expectThrows(IllegalArgumentException.class, () -> SecretTool.run("encrypt", "x", ""));
    }

    @Test(groups = {"selftest"}, description = "Неизвестная команда — понятная ошибка")
    public void unknownCommandFails() {
        IllegalArgumentException e = expectThrows(IllegalArgumentException.class, () -> SecretTool.run("hash", "x", KEY));
        assertTrue(e.getMessage().startsWith("Неизвестная команда 'hash'"), e.getMessage());
    }

    @Test(groups = {"selftest"}, description = "Неверное число аргументов main — понятная ошибка")
    public void wrongArgumentCountFails() {
        IllegalArgumentException e = expectThrows(IllegalArgumentException.class, () -> SecretTool.main(new String[]{"encrypt"}));
        assertTrue(e.getMessage().startsWith("Ожидается 2 аргумента, передано 1"), e.getMessage());
        expectThrows(IllegalArgumentException.class, () -> SecretTool.main(new String[0]));
    }

    @Test(groups = {"selftest"}, description = "Нерасшифровываемая строка — IllegalStateException, а не null")
    public void decryptGarbageFails() {
        // Base64 от 5 байт: длина не кратна блоку DES (8) -> IllegalBlockSizeException внутри EncryptionSettings
        String notABlock = java.util.Base64.getEncoder().encodeToString(new byte[5]);
        expectThrows(IllegalStateException.class, () -> SecretTool.run("decrypt", notABlock, KEY));
    }
}
