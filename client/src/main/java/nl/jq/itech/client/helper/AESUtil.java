package nl.jq.itech.client.helper;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.IvParameterSpec;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;

public class AESUtil {
    /**
     * Generate a SecretKey used for synchronous encryption
     *
     * @return SecretKey
     * @throws NoSuchAlgorithmException AES is not known
     */
    public static SecretKey generateKey() throws NoSuchAlgorithmException {
        KeyGenerator keyGenerator = KeyGenerator.getInstance("AES");
        keyGenerator.init(128);
        return keyGenerator.generateKey();
    }

    /**
     * Encrypt a string into a base64 encoded encrypted message with a SecretKey
     *
     * @param input text to encrypt
     * @param key   key to encrypt with
     * @return Base64[SecretKey[message]]
     * @throws Exception exception
     */
    public static String encrypt(String input, SecretKey key) throws Exception {
        Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
        cipher.init(Cipher.ENCRYPT_MODE, key, generateIv());
        byte[] cipherText = cipher.doFinal(input.getBytes());
        return Base64.getEncoder().encodeToString(cipherText);
    }

    /**
     * Decrypt an encrypted string encoded in base64 with a SecretKey
     *
     * @param cipherTextBase64 base64 to decrypt
     * @param key              key to decrypt with
     * @return plaintext
     * @throws Exception exception
     */
    public static String decrypt(String cipherTextBase64, SecretKey key) throws Exception {
        Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
        cipher.init(Cipher.DECRYPT_MODE, key, generateIv());
        byte[] plainText = cipher.doFinal(Base64.getDecoder().decode(cipherTextBase64));
        return new String(plainText);
    }

    /**
     * Generate an IV
     * @return iv
     */
    public static IvParameterSpec generateIv() {
        return new IvParameterSpec(new byte[16]);
    }
}
