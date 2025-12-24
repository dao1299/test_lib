package com.vtnet.netat.core.secret;

import com.vtnet.netat.core.logging.NetatLogger;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.spec.KeySpec;
import java.util.Base64;

/**
 * Giải mã dữ liệu nhạy cảm đã được mã hóa bằng AES-256-GCM.
 *
 * <p>Class này CHỈ chứa logic DECRYPT. Phần encrypt do tool bên ngoài xử lý.</p>
 *
 * <p>Format ciphertext: Base64(salt[16] + iv[12] + encrypted_data)</p>
 *
 * <p>Thuật toán:</p>
 * <ul>
 *   <li>Encryption: AES-256-GCM</li>
 *   <li>Key Derivation: PBKDF2WithHmacSHA256 (65536 iterations)</li>
 *   <li>Authentication: GCM 128-bit tag</li>
 * </ul>
 *
 * <p>Ví dụ sử dụng:</p>
 * <pre>
 * // Cách 1: Decrypt với master key tự cung cấp
 * String plainText = SecretDecryptor.decrypt(encryptedText, masterKey);
 *
 * // Cách 2: Decrypt với master key tự động lấy từ ENV/.env
 * String plainText = SecretDecryptor.decrypt(encryptedText);
 * </pre>
 */
public final class SecretDecryptor {

//    private static final Logger log = LoggerFactory.getLogger(SecretDecryptor.class);

    private static final NetatLogger log = NetatLogger.getInstance(SecretDecryptor.class);
    // ==================== CONSTANTS ====================
    // Phải KHỚP với tool encrypt bên ngoài

    private static final String ALGORITHM = "AES/GCM/NoPadding";
    private static final String KEY_ALGORITHM = "AES";
    private static final String KEY_DERIVATION = "PBKDF2WithHmacSHA256";

    private static final int KEY_LENGTH = 256;        // bits
    private static final int SALT_LENGTH = 16;        // bytes
    private static final int IV_LENGTH = 12;          // bytes (GCM recommended)
    private static final int TAG_LENGTH = 128;        // bits (GCM auth tag)
    private static final int ITERATIONS = 65536;      // PBKDF2 iterations

    private static final int MIN_CIPHERTEXT_LENGTH = SALT_LENGTH + IV_LENGTH + 16;

    private SecretDecryptor() {}


    public static String decrypt(String ciphertext) {
        String masterKey = MasterKeyProvider.getMasterKey();
        return decrypt(ciphertext, masterKey);
    }

    public static String decrypt(String ciphertext, String masterKey) {
        validateInputs(ciphertext, masterKey);

        try {
            byte[] decoded = Base64.getDecoder().decode(ciphertext);

            if (decoded.length < MIN_CIPHERTEXT_LENGTH) {
                throw new SecretDecryptionException("Invalid ciphertext: too short");
            }

            // Extract components: salt + iv + encrypted_data
            ByteBuffer buffer = ByteBuffer.wrap(decoded);

            byte[] salt = new byte[SALT_LENGTH];
            byte[] iv = new byte[IV_LENGTH];
            byte[] encrypted = new byte[decoded.length - SALT_LENGTH - IV_LENGTH];

            buffer.get(salt);
            buffer.get(iv);
            buffer.get(encrypted);

            // Derive key từ master password
            SecretKey key = deriveKey(masterKey, salt);

            // Decrypt
            Cipher cipher = Cipher.getInstance(ALGORITHM);
            GCMParameterSpec gcmSpec = new GCMParameterSpec(TAG_LENGTH, iv);
            cipher.init(Cipher.DECRYPT_MODE, key, gcmSpec);

            byte[] plaintext = cipher.doFinal(encrypted);

            return new String(plaintext, StandardCharsets.UTF_8);

        } catch (IllegalArgumentException e) {
            log.error("Invalid Base64 ciphertext", e);
            throw new SecretDecryptionException("Invalid ciphertext format: not valid Base64", e);
        } catch (SecretDecryptionException e) {
            throw e;
        } catch (Exception e) {
            log.error("Decryption failed", e);
            throw new SecretDecryptionException(
                    "Failed to decrypt secret. Verify master key is correct.", e);
        }
    }

    public static boolean isEncrypted(String value) {
        if (value == null || value.isEmpty()) {
            return false;
        }

        try {
            byte[] decoded = Base64.getDecoder().decode(value);
            return decoded.length >= MIN_CIPHERTEXT_LENGTH;
        } catch (IllegalArgumentException e) {
            return false;
        }
    }


    public static boolean verifyMasterKey(String ciphertext, String masterKey) {
        try {
            decrypt(ciphertext, masterKey);
            return true;
        } catch (SecretDecryptionException e) {
            return false;
        }
    }


    public static String decryptIfEncrypted(String value) {
        if (!isEncrypted(value)) {
            return value;
        }

        try {
            return decrypt(value);
        } catch (Exception e) {
            log.debug("Value looks encrypted but decryption failed, returning original");
            return value;
        }
    }


    public static String decryptIfEncrypted(String value, String masterKey) {
        if (!isEncrypted(value)) {
            return value;
        }

        try {
            return decrypt(value, masterKey);
        } catch (Exception e) {
            log.debug("Value looks encrypted but decryption failed, returning original");
            return value;
        }
    }

    private static void validateInputs(String ciphertext, String masterKey) {
        if (ciphertext == null || ciphertext.trim().isEmpty()) {
            throw new IllegalArgumentException("Ciphertext cannot be null or empty");
        }
        if (masterKey == null || masterKey.isEmpty()) {
            throw new IllegalArgumentException("Master key cannot be null or empty");
        }
    }


    private static SecretKey deriveKey(String masterKey, byte[] salt) throws Exception {
        KeySpec spec = new PBEKeySpec(
                masterKey.toCharArray(),
                salt,
                ITERATIONS,
                KEY_LENGTH
        );

        SecretKeyFactory factory = SecretKeyFactory.getInstance(KEY_DERIVATION);
        byte[] keyBytes = factory.generateSecret(spec).getEncoded();

        return new SecretKeySpec(keyBytes, KEY_ALGORITHM);
    }

    public static class SecretDecryptionException extends RuntimeException {

        public SecretDecryptionException(String message) {
            super(message);
        }

        public SecretDecryptionException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}