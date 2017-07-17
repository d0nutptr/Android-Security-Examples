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
    private static String hash_encryption_alias = "cryptonote_hash_key";

    /**
     * Creates a hash in base64 of the user's password using pbkdf2 since we have no nice alternatives like bcrypt.
     * @param password user's password
     * @return a bas64 representation of the pbkdf2 hash
     * @throws GeneralSecurityException
     */
    public static String generateUserHash(String password) throws GeneralSecurityException {
        byte[] salt = new byte[HASH_SALT_SIZE];
        (new SecureRandom()).nextBytes(salt);

        byte[] hash = generateUserHash(password, salt);

        byte[] output = new byte[HASH_SALT_SIZE + HASH_OUTPUT_SIZE];

        System.arraycopy(salt, 0, output, 0, salt.length);
        System.arraycopy(hash, 0, output, salt.length, HASH_OUTPUT_SIZE);

        return Base64.encodeToString(output, Base64.DEFAULT);
    }

    /**
     * Creates a hash in bytes of the user's password.
     * @param password the user's password
     * @param salt the salt to use when creating the hash
     * @return the pbkdf2 hash of the user's password in bytes
     * @throws GeneralSecurityException
     */
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

    /**
     * Encrypts the user's hash using AEAD.
     * @param hash The hash you want encrypted
     * @return The base64 encrypted hash.
     * @throws GeneralSecurityException
     * @throws IOException
     */
    public static String encryptHash(String hash) throws GeneralSecurityException, IOException {
        KeyStore keyStore = KeyStore.getInstance("AndroidKeyStore");
        keyStore.load(null, null);

        SecretKey encryptionKey = null;

        if(!keyStore.isKeyEntry(hash_encryption_alias)) {
            KeyGenerator generator = KeyGenerator.getInstance("AES", "AndroidKeyStore");
            generator.init(new KeyGenParameterSpec.Builder(hash_encryption_alias, KeyProperties.PURPOSE_ENCRYPT | KeyProperties.PURPOSE_DECRYPT)
                    .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                    .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                    .setKeySize(256)
                    .build(), new SecureRandom());
            generator.generateKey();
        }

        encryptionKey = (SecretKey)keyStore.getKey(hash_encryption_alias, null);

        return encrypt(hash, encryptionKey);
    }

    /**
     * Decrypts the user's hash.
     * @param encryptedHash The encrypted hash string
     * @return the decrypted hash string
     * @throws GeneralSecurityException
     * @throws IOException
     */
    public static String decryptHash(String encryptedHash) throws GeneralSecurityException, IOException  {
        KeyStore keyStore = KeyStore.getInstance("AndroidKeyStore");
        keyStore.load(null, null);

        SecretKey encryptionKey = null;

        if(keyStore.isKeyEntry(hash_encryption_alias)) {
            encryptionKey = (SecretKey)keyStore.getKey(hash_encryption_alias, null);
            return decrypt(encryptedHash, encryptionKey);
        }

        throw new IOException();
    }

    /**
     * Checks if the password matches the stored hash.
     * @param password The user's password
     * @param hashString The decrypted hash string
     * @return True if they match, false otherwise
     * @throws GeneralSecurityException
     */
    public static boolean compareUserHash(String password, String hashString) throws GeneralSecurityException {
        byte[] storedHash = Base64.decode(hashString, Base64.DEFAULT);
        byte[] salt = new byte[HASH_SALT_SIZE];
        byte[] hashPortion = new byte[HASH_OUTPUT_SIZE];

        System.arraycopy(storedHash, 0, salt, 0, HASH_SALT_SIZE);
        System.arraycopy(storedHash, HASH_SALT_SIZE, hashPortion, 0, HASH_OUTPUT_SIZE);

        byte[] providedHash = generateUserHash(password, salt);

        return timeSafeCompare(hashPortion, providedHash);
    }

    /**
     * Does a time safe compare between byte arrays
     * @param arr1 byte array 1
     * @param arr2 byte array 2
     * @return true if they match, false otherwise
     */
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

    /**
     * Gets the user's encryption key
     * @return The encryption key for the user.
     * @throws GeneralSecurityException
     * @throws IOException
     */
    public static SecretKey getEncryptionKey() throws GeneralSecurityException, IOException {
        KeyStore keyStore = KeyStore.getInstance("AndroidKeyStore");
        keyStore.load(null, null);
        return (SecretKey) keyStore.getKey(alias, null);
    }

    /**
     * Creates the encryption key for the user.
     * @throws GeneralSecurityException
     * @throws IOException
     */
    public static void createEncryptionKey() throws GeneralSecurityException, IOException {
        KeyGenerator generator = KeyGenerator.getInstance("AES", "AndroidKeyStore");
        generator.init(new KeyGenParameterSpec.Builder(alias, KeyProperties.PURPOSE_ENCRYPT | KeyProperties.PURPOSE_DECRYPT)
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setKeySize(256)
                .build(), new SecureRandom());
        generator.generateKey();
    }

    /**
     * Encrypts the plaintext with the provided encryption key.
     * @param plaintext the plaintext to encrypt
     * @param encryptionKey the key to encrypt with
     * @return a base64 string of the encrypted data.
     */
    public static String encrypt(String plaintext, SecretKey encryptionKey) {
        byte[] cipherTextPayload = null;

        try {

            byte[] plaintextBytes = plaintext.getBytes("UTF-8");

            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.ENCRYPT_MODE, encryptionKey, new SecureRandom());
            byte[] cipherText = cipher.doFinal(plaintextBytes);

            //should be 12 bytes
            byte[] iv = cipher.getIV();

            cipherTextPayload = new byte[iv.length + cipherText.length];

            System.arraycopy(iv, 0, cipherTextPayload, 0, iv.length);
            System.arraycopy(cipherText, 0, cipherTextPayload, iv.length, cipherText.length);

        } catch (GeneralSecurityException | IOException e) {
            return null;
        }

        return Base64.encodeToString(cipherTextPayload, Base64.DEFAULT);
    }

    /**
     * Encrypts the provided plaintext with the user's encryption key
     * @param plaintext the plaintext to encrypt
     * @return the base64 string of the encrypted data
     */
    public static String encrypt(String plaintext){
        SecretKey encryptionKey = null;
        try {
            encryptionKey = getEncryptionKey();
        } catch (GeneralSecurityException | IOException e) {
            return null;
        }

        return encrypt(plaintext, encryptionKey);
    }

    /**
     * Decrypts the provided ciphertext with the provided key
     * @param cipherTextPayload the provided ciphertext to decrypt
     * @param encryptionKey the encryption key to use
     * @return the plaintext message
     */
    public static String decrypt(String cipherTextPayload, SecretKey encryptionKey) {
        byte[] plaintext = null;
        try {
            byte[] cipherTextPayloadBytes = Base64.decode(cipherTextPayload, Base64.DEFAULT);
            byte[] iv = new byte[12];
            byte[] cipherText = new byte[cipherTextPayloadBytes.length - iv.length];

            System.arraycopy(cipherTextPayloadBytes, 0, iv, 0, iv.length);
            System.arraycopy(cipherTextPayloadBytes, iv.length, cipherText, 0, cipherText.length);

            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.DECRYPT_MODE, encryptionKey, new GCMParameterSpec(128, iv));
            plaintext = cipher.doFinal(cipherText);
        } catch (GeneralSecurityException e) {
            return null;
        }

        return new String(plaintext);
    }

    /**
     * Decrypts the provided ciphertext with the user's encryption key
     * @param cipherTextPayload the ciphertext to decrypt
     * @return the plaintext message
     */
    public static String decrypt(String cipherTextPayload) {
        SecretKey encryptionKey = null;
        try {
            encryptionKey = getEncryptionKey();
        } catch (GeneralSecurityException | IOException e) {
            return null;
        }

        return decrypt(cipherTextPayload, encryptionKey);
    }
}
