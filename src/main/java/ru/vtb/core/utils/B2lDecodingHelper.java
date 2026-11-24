package ru.vtb.core.utils;

import org.apache.commons.compress.archivers.zip.ZipArchiveInputStream;
import ru.vtb.core.common.Log;

import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.OAEPParameterSpec;
import javax.crypto.spec.PSource;
import javax.crypto.spec.SecretKeySpec;
import java.io.*;
import java.nio.file.Path;
import java.security.*;
import java.security.cert.CertificateFactory;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.MGF1ParameterSpec;
import java.security.spec.PSSParameterSpec;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.stream.IntStream;
import java.util.zip.GZIPInputStream;
import java.util.zip.ZipEntry;

import static ru.vtb.core.common.EnvConstants.*;

public class B2lDecodingHelper {

    public static File decodeFile(String encodedFilePath, String resultPath) {
        InputStream zipStream = FileHelper.fileToInputStream(encodedFilePath);
        var map = extractFilesFromZip(zipStream);
        var b2lFileBytes = map.get(getFileNameWithoutExt(Path.of(encodedFilePath).getFileName().toString()) + ".enc");
        verifySignature(b2lFileBytes, map.get("signature.sig"));
        var decryptSessionKey = decryptRsaSessionKey(map.get("session.key.enc"));
        var decryptedFile = decryptAes(b2lFileBytes, hexToBytes(decryptSessionKey), map.get("vector.vec"));
        return FileHelper.saveInputStreamToFile(getDecompressData(decryptedFile), resultPath);
    }

    private static Map<String, byte[]> extractFilesFromZip(InputStream zip) {
        Map<String, byte[]> extractingFiles = new HashMap<>();

        try (ZipArchiveInputStream zipStream = new ZipArchiveInputStream(zip, null, false, true)) {
            ZipEntry zipEntry;

            while ((zipEntry = zipStream.getNextEntry()) != null) {
                if (!zipEntry.isDirectory()) {
                    String fileName = zipEntry.getName();
                    ByteArrayOutputStream arrayOutputStream = new ByteArrayOutputStream();
                    int bytes;
                    byte[] buffer = new byte[8192];
                    while ((bytes = zipStream.read(buffer)) > 0) {
                        arrayOutputStream.write(buffer, 0, bytes);
                    }

                    extractingFiles.put(fileName, arrayOutputStream.toByteArray());
                }
            }
        } catch (IOException e) {
            Log.error("Error while extracting files from zip.Cause:{}" + e.getMessage());
            //TODO: в дальнейшем нужно обработку кастомного ексепшена подвязать к логу и выпилить отсюда, оставить только в конструкторе
            throw new RuntimeException("Error extracting files from archive");
        }

        return extractingFiles;
    }

    private static void verifySignature(byte[] b2l, byte[] sig) {

        boolean isValid;

        try {
            var signature = Signature.getInstance("RSASSA-PSS");
            var publicKey = (RSAPublicKey) getPublicKeyFromCertificate();
            var salt = publicKey.getModulus().bitLength() / 8 - 34; //34 = 32байта(длина SHA256) + 2байта(резерв сдвиг по RFC8017 9.1)
            var parameterSpec = new PSSParameterSpec("SHA-256", "MGF1", MGF1ParameterSpec.SHA256, salt, 1);
            signature.setParameter(parameterSpec);
            signature.initVerify(publicKey);
            signature.update(b2l);
            isValid = signature.verify(sig);
        } catch (Exception e) {
            Log.error("Error verifying signature. Cause: {}" + e.getMessage());
            throw new RuntimeException("Error verifying signature");
        }

        if (!isValid) {
            throw new RuntimeException("Invalid signature verify" + "Signature invalid");
        }

        Log.info("Success verifying signature");
    }

    private static PublicKey getPublicKeyFromCertificate() {
        try (InputStream inputStream = new FileInputStream(CLEARING_SIGNATURE_FILE)) {
            CertificateFactory factory = CertificateFactory.getInstance("X.509");
            return factory.generateCertificate(inputStream).getPublicKey();
        } catch (Exception e) {
            Log.error("Error getting public key from cert. Cause: {}" + e.getMessage());
            throw new RuntimeException("Error getting public key from cert " + new File(CLEARING_SIGNATURE_FILE).getName() + e.getMessage());
        }
    }

    private static byte[] decryptRsaSessionKey(byte[] sessionKey) {
        try {
            var cipher = Cipher.getInstance("RSA/ECB/OAEPWithSHA-256AndMGF1Padding");
            var parameterSpec = new OAEPParameterSpec(
                    "SHA-256", "MGF1", MGF1ParameterSpec.SHA256, new PSource.PSpecified(new byte[0]));
            cipher.init(Cipher.DECRYPT_MODE, getPrivateKey(), parameterSpec);
            return cipher.doFinal(sessionKey);
        } catch (Exception e) {
            Log.error("Error decrypting session key.Cause: {}" + e.getMessage());
            throw new RuntimeException("Error decrypting RSA");
        }
    }

    private static PrivateKey getPrivateKey() throws KeyStoreException, UnrecoverableKeyException, NoSuchAlgorithmException {
        var keyStore = KeyStore.getInstance(CLEARING_SSL_KEYSTORE_TYPE);
        try (InputStream is = new FileInputStream(CLEARING_SSL_KEYSTORE_LOCATION)) {
            keyStore.load(is, SSL_KEYSTORE_PASSWORD.toCharArray());
        } catch (Exception e) {
            Log.error("Error loading keystore. Cause: {}" + e.getMessage());
            throw new RuntimeException("Error getting private key");
        }

        String alias = keyStore.aliases().nextElement();

        var key = keyStore.getKey(alias, SSL_KEYSTORE_PASSWORD.toCharArray());
        return (PrivateKey) key;

    }

    private static byte[] decryptAes(byte[] b2l, byte[] decryptionSessionKey, byte[] vector) {
        try {
            var cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
            var keySpec = new SecretKeySpec(decryptionSessionKey, "AES");
            var ivParameterSpec = new IvParameterSpec(hexToBytes(vector));

            cipher.init(Cipher.DECRYPT_MODE, keySpec, ivParameterSpec);
            return cipher.doFinal(b2l);
        } catch (Exception e) {
            Log.error("Error decrypting file by session key.Cause:{}" + e.getMessage());
            throw new RuntimeException("Error decryption AES with session key");
        }
    }

    private static String extractFileName(String path, String delimiter, String nullDefault) {
        String[] parts = Objects.toString(path, "").split(delimiter);
        return parts.length > 0
                ? parts[parts.length - 1]
                : nullDefault;
    }


    public static String getFileNameWithoutExt(String fileName) {
        if (Objects.isNull(fileName)) {
            return null;
        }

        int lastDotIndex = fileName.lastIndexOf(".");
        return lastDotIndex > 0
                ? fileName.substring(0, lastDotIndex)
                : null;
    }


    private static byte[] hexToBytes(byte[] hex) {
        return IntStream.range(0, hex.length / 2)
                .map(i -> {
                    int high = Character.digit((char) hex[i * 2], 16);
                    int low = Character.digit((char) hex[i * 2 + 1], 16);
                    return (high << 4) + low;
                })
                .collect(
                        () -> new ByteArrayOutputStream(hex.length / 2),
                        ByteArrayOutputStream::write,
                        ((byteArrayOutputStream, byteArrayOutputStream2) -> {
                            try {
                                byteArrayOutputStream2.writeTo(byteArrayOutputStream);
                            } catch (Exception e) {
                                Log.error("Error converting hex to bytes.Cause: {}" + e.getMessage());
                                throw new RuntimeException("Error while hex to bytes converting");
                            }
                        }))
                .toByteArray();
    }

    private static InputStream getDecompressData(byte[] compressed) {
        try {
            return new GZIPInputStream(new ByteArrayInputStream(compressed));
        } catch (IOException e) {
            Log.error("Error decompress file data.Cause:{}" + e.getMessage());
            throw new RuntimeException("Error while stream decompress data file");
        }
    }

}
