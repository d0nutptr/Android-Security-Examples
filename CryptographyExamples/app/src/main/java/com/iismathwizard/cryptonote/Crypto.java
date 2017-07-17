package com.iismathwizard.cryptonote;

import android.security.keystore.KeyGenParameterSpec;
import android.security.keystore.KeyProperties;
import android.util.Base64;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.security.KeyStore;
import java.security.SecureRandom;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.PBEKeySpec;

public class Crypto {
    private static int HASH_SALT_SIZE = 32; //32 bytes
    private static int HASH_ITERATIONS = 8192; //2^13 should be strong enough for this application
    private static int HASH_OUTPUT_SIZE = 32; //32 bytes since we're using HmacSHA256 underneath

    private static String alias = "cryptonote_key";

    public static String generateUserHash(String password) throws GeneralSecurityException {
        byte[] salt = new byte[HASH_SALT_SIZE];
        (new SecureRandom()).nextBytes(salt);

        byte[] hash = generateUserHash(password, salt);

        byte[] output = new byte[HASH_SALT_SIZE + HASH_OUTPUT_SIZE];

        System.arraycopy(salt, 0, output, 0, salt.length);
        System.arraycopy(hash, 0, output, salt.length, HASH_OUTPUT_SIZE);

        return Base64.encodeToString(output, Base64.DEFAULT);
    }

    public static byte[] generateUserHash(String password, byte[] salt) throws GeneralSecurityException {
        /*
            https://security.stackexchange.com/a/47188/79148
            We use PBKDF2 because android doesn't natively support bcrypt, scrypt, or argon2i.
            Additionally, keep in mind that this will only provide 20 bytes of security instead
            of 32 bytes because we're using HmacSHA1. Android doesn't support HmacSHA256.
            You can use SpongyCastle/BouncyCastle to include support for PBKDF2-HmacSHA256.
        */
        SecretKeyFactory factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA1");
        PBEKeySpec keySpec = new PBEKeySpec(password.toCharArray(), salt, HASH_ITERATIONS, HASH_OUTPUT_SIZE * 8);
        SecretKey hash = factory.generateSecret(keySpec);

        return hash.getEncoded();
    }

    public static boolean compareUserHash(String password, String hashString) throws GeneralSecurityException {
        byte[] storedHash = Base64.decode(hashString, Base64.DEFAULT);
        byte[] salt = new byte[HASH_SALT_SIZE];
        byte[] hashPortion = new byte[HASH_OUTPUT_SIZE];

        System.arraycopy(storedHash, 0, salt, 0, HASH_SALT_SIZE);
        System.arraycopy(storedHash, HASH_SALT_SIZE, hashPortion, 0, HASH_OUTPUT_SIZE);

        byte[] providedHash = generateUserHash(password, salt);

        return timeSafeCompare(hashPortion, providedHash);
    }

    public static boolean timeSafeCompare(byte[] arr1, byte[] arr2){
        if(arr1.length != arr2.length) {
            return false;
        }

        int xor = 0;

        for(int i = 0; i < arr1.length; i ++) {
            xor |= arr1[i] ^ arr2[i];
        }

        return xor == 0;
    }

    public static SecretKey getEncryptionKey() throws GeneralSecurityException, IOException {
        KeyStore keyStore = KeyStore.getInstance("AndroidKeyStore");
        keyStore.load(null, null);
        return (SecretKey) keyStore.getKey(alias, null);
    }

    public static void createEncryptionKey() throws GeneralSecurityException, IOException {
        KeyGenerator generator = KeyGenerator.getInstance("AES", "AndroidKeyStore");
        generator.init(new KeyGenParameterSpec.Builder(alias, KeyProperties.PURPOSE_ENCRYPT | KeyProperties.PURPOSE_DECRYPT)
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setKeySize(256)
                .build(), new SecureRandom());
        generator.generateKey();
    }

    public static String encrypt(String plaintext) {
        SecretKey encryptionKey = null;
        byte[] cipherTextPayload = null;

        try {
            encryptionKey = getEncryptionKey();

            byte[] plaintextBytes = plaintext.getBytes("UTF-8");

            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.ENCRYPT_MODE, encryptionKey, new SecureRandom());
            byte[] cipherText = cipher.doFinal(plaintextBytes);

            //should be 12 bytes
            byte[] iv = cipher.getIV();

            cipherTextPayload = new byte[iv.length + cipherText.length];

            System.arraycopy(iv, 0, cipherTextPayload, 0, iv.length);
            System.arraycopy(cipherText, 0, cipherTextPayload, iv.length, cipherText.length);

        } catch (GeneralSecurityException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return Base64.encodeToString(cipherTextPayload, Base64.DEFAULT);
    }

    public static String decrypt(String cipherTextPayload)  {
        byte[] plaintext = null;
        SecretKey encryptionKey = null;
        try {
            encryptionKey = getEncryptionKey();

            byte[] cipherTextPayloadBytes = Base64.decode(cipherTextPayload, Base64.DEFAULT);
            byte[] iv = new byte[12];
            byte[] cipherText = new byte[cipherTextPayloadBytes.length - iv.length];

            System.arraycopy(cipherTextPayloadBytes, 0, iv, 0, iv.length);
            System.arraycopy(cipherTextPayloadBytes, iv.length, cipherText, 0, cipherText.length);

            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.DECRYPT_MODE, encryptionKey, new GCMParameterSpec(128, iv));
            plaintext = cipher.doFinal(cipherText);
        } catch (GeneralSecurityException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return new String(plaintext);
    }
}
