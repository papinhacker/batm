package ru.vtb.core.common;

import javax.crypto.*;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.Key;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;

public class EncryptionSettings {

    private static final String CIPHER = "DES";
    private final Key passwordSecretKey;

    private Cipher eCipher;
    private Cipher dCipher;

    public EncryptionSettings(String key) {
        passwordSecretKey = getKeyForText(key);
        init();
    }

    private void init() {
        try {
            eCipher = Cipher.getInstance(CIPHER);
            dCipher = Cipher.getInstance(CIPHER);
            eCipher.init(Cipher.ENCRYPT_MODE, passwordSecretKey);
            dCipher.init(Cipher.DECRYPT_MODE, passwordSecretKey);
        } catch (NoSuchPaddingException | NoSuchAlgorithmException | InvalidKeyException e) {
            throw new RuntimeException("Не удалось проинициализировать класс шифрования по причине: " + e.getMessage());
        }
    }

    public String encryptString(String stringToEncrypt) {
        try {
            byte[] bytes = stringToEncrypt.getBytes(StandardCharsets.UTF_8);
            byte[] cipherTextBytes = eCipher.doFinal(bytes);
            return new String(Base64.getEncoder().encode(cipherTextBytes), StandardCharsets.UTF_8);
        } catch (IllegalBlockSizeException | BadPaddingException e) {
            Log.error("Не удалось зашифровать строку по причине: " + e.getMessage());
        }
        return null;
    }

    public String decryptString(String stringToDecrypt) {
        try {
            byte[] bytes = Base64.getDecoder().decode(stringToDecrypt);
            byte[] cipherTextBytes = dCipher.doFinal(bytes);
            return new String(cipherTextBytes, StandardCharsets.UTF_8);
        } catch (IllegalBlockSizeException | BadPaddingException e) {
            Log.error("Не удалось расшифроваать строку '" + stringToDecrypt +
                    "' по причине: " + e.getMessage());
        }
        return null;
    }

    private SecretKey getKeyForText(String secretText) {
        byte[] keyBytes = secretText.getBytes(StandardCharsets.UTF_8);
        return new SecretKeySpec(keyBytes, CIPHER);
    }
}
