package ru.vtb.tools;

import ru.vtb.core.common.EncryptionSettings;

/**
 * Шифрование/расшифровка значений для properties (тот же алгоритм и ключ, что у EnvConstants.getEncryptedProperty).
 * <p>
 * Запуск: {@code java -Dekey=<ключ> -cp auto-testing-1.0-fat-tests.jar ru.vtb.tools.SecretTool encrypt|decrypt <строка>}
 * <p>
 * Ключ берётся из -Dekey, строка — из аргумента. EnvConstants не используется, поэтому -Dcenv не нужен.
 * Результат печатается в stdout; при ошибке бросается исключение с понятным текстом (код возврата JVM — 1).
 */
public final class SecretTool {

    public static final String KEY_PROPERTY = "ekey";
    private static final String USAGE = "Использование: java -D" + KEY_PROPERTY
            + "=<ключ> -cp <fat-jar> ru.vtb.tools.SecretTool encrypt|decrypt <строка>";

    private SecretTool() {
    }

    public static void main(String[] args) {
        if (args.length != 2) {
            throw new IllegalArgumentException("Ожидается 2 аргумента, передано %d. %s".formatted(args.length, USAGE));
        }
        System.out.println(run(args[0], args[1], System.getProperty(KEY_PROPERTY)));
    }

    /**
     * @param command encrypt или decrypt
     * @param value   строка для шифрования / зашифрованная строка
     * @param key     ключ шифрования (значение -Dekey)
     * @return результат шифрования/расшифровки
     */
    public static String run(String command, String value, String key) {
        if (key == null || key.isEmpty()) {
            throw new IllegalArgumentException("Не задан ключ шифрования: передайте -D%s=<ключ>. %s".formatted(KEY_PROPERTY, USAGE));
        }
        if (value == null || value.isEmpty()) {
            throw new IllegalArgumentException("Не задана строка для обработки. " + USAGE);
        }
        EncryptionSettings encryption = new EncryptionSettings(key);
        String result = switch (command) {
            case "encrypt" -> encryption.encryptString(value);
            case "decrypt" -> encryption.decryptString(value);
            default -> throw new IllegalArgumentException("Неизвестная команда '%s'. %s".formatted(command, USAGE));
        };
        if (result == null) {
            // EncryptionSettings при ошибке пишет причину в лог и возвращает null
            throw new IllegalStateException("Не удалось выполнить '%s': см. причину в логе выше (неверный ключ или строка)"
                    .formatted(command));
        }
        return result;
    }
}
