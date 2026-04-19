package net.hwyz.iov.cloud.sec.ciam.service.common.security;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.Objects;

/**
 * 敏感身份字段加密器。
 * <p>
 * 对手机号、邮箱、第三方主体标识等敏感字段提供：
 * <ul>
 *   <li>AES-256-GCM 加密/解密 — 存入 {@code identity_value} 列</li>
 *   <li>SHA-256 哈希 — 存入 {@code identity_hash} 列，用于唯一查重</li>
 * </ul>
 */
public final class FieldEncryptor {

    private static final String AES_ALGORITHM = "AES";
    private static final String AES_GCM_TRANSFORMATION = "AES/GCM/NoPadding";
    private static final int GCM_IV_LENGTH = 12;
    private static final int GCM_TAG_LENGTH = 128;
    private static final String SHA_256 = "SHA-256";

    private final SecretKeySpec secretKey;

    /**
     * 构造加密器。
     *
     * @param aesKeyBase64 Base64 编码的 AES-256 密钥（32 字节原始密钥）
     * @throws IllegalArgumentException 密钥为空或长度不合法
     */
    public FieldEncryptor(String aesKeyBase64) {
        Objects.requireNonNull(aesKeyBase64, "AES key must not be null");
        byte[] keyBytes = Base64.getDecoder().decode(aesKeyBase64);
        if (keyBytes.length != 32) {
            throw new IllegalArgumentException("AES-256 key must be 32 bytes, got " + keyBytes.length);
        }
        this.secretKey = new SecretKeySpec(keyBytes, AES_ALGORITHM);
    }

    /**
     * AES-256-GCM 加密。
     *
     * @param plaintext 明文
     * @return Base64 编码的密文（IV + ciphertext + tag）
     */
    public String encrypt(String plaintext) {
        Objects.requireNonNull(plaintext, "plaintext must not be null");
        try {
            byte[] iv = new byte[GCM_IV_LENGTH];
            new SecureRandom().nextBytes(iv);

            Cipher cipher = Cipher.getInstance(AES_GCM_TRANSFORMATION);
            cipher.init(Cipher.ENCRYPT_MODE, secretKey, new GCMParameterSpec(GCM_TAG_LENGTH, iv));

            byte[] ciphertext = cipher.doFinal(plaintext.getBytes(StandardCharsets.UTF_8));

            ByteBuffer buffer = ByteBuffer.allocate(iv.length + ciphertext.length);
            buffer.put(iv);
            buffer.put(ciphertext);
            return Base64.getEncoder().encodeToString(buffer.array());
        } catch (Exception e) {
            throw new SecurityException("Encryption failed", e);
        }
    }

    /**
     * AES-256-GCM 解密。
     *
     * @param ciphertextBase64 Base64 编码的密文
     * @return 明文
     */
    public String decrypt(String ciphertextBase64) {
        Objects.requireNonNull(ciphertextBase64, "ciphertext must not be null");
        try {
            byte[] decoded = Base64.getDecoder().decode(ciphertextBase64);
            ByteBuffer buffer = ByteBuffer.wrap(decoded);

            byte[] iv = new byte[GCM_IV_LENGTH];
            buffer.get(iv);
            byte[] ciphertext = new byte[buffer.remaining()];
            buffer.get(ciphertext);

            Cipher cipher = Cipher.getInstance(AES_GCM_TRANSFORMATION);
            cipher.init(Cipher.DECRYPT_MODE, secretKey, new GCMParameterSpec(GCM_TAG_LENGTH, iv));

            byte[] plainBytes = cipher.doFinal(ciphertext);
            return new String(plainBytes, StandardCharsets.UTF_8);
        } catch (Exception e) {
            throw new SecurityException("Decryption failed", e);
        }
    }

    /**
     * 生成 SHA-256 哈希（小写十六进制，64 字符），用于 {@code identity_hash} 列唯一查重。
     *
     * @param value 原始值
     * @return 64 字符十六进制哈希
     */
    public static String hash(String value) {
        Objects.requireNonNull(value, "value must not be null");
        try {
            MessageDigest digest = MessageDigest.getInstance(SHA_256);
            byte[] hashBytes = digest.digest(value.getBytes(StandardCharsets.UTF_8));
            return bytesToHex(hashBytes);
        } catch (Exception e) {
            throw new SecurityException("Hashing failed", e);
        }
    }

    private static String bytesToHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder(bytes.length * 2);
        for (byte b : bytes) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }
}
