package net.hwyz.iov.cloud.sec.ciam.service.common.security;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Objects;

/**
 * 令牌/验证码指纹生成器。
 * <p>
 * 对授权码、Refresh Token、验证码等敏感凭据生成 SHA-256 指纹，
 * 用于 {@code code_hash}、{@code token_fingerprint}、{@code verify_code_hash} 等列。
 * <p>
 * 原始值不落库，仅存储指纹。
 */
public final class TokenDigest {

    private static final String SHA_256 = "SHA-256";

    private TokenDigest() {
    }

    /**
     * 生成 SHA-256 指纹（小写十六进制，64 字符）。
     *
     * @param rawValue 原始令牌/验证码值
     * @return 64 字符十六进制指纹
     */
    public static String fingerprint(String rawValue) {
        Objects.requireNonNull(rawValue, "rawValue must not be null");
        try {
            MessageDigest digest = MessageDigest.getInstance(SHA_256);
            byte[] hashBytes = digest.digest(rawValue.getBytes(StandardCharsets.UTF_8));
            return bytesToHex(hashBytes);
        } catch (Exception e) {
            throw new SecurityException("Fingerprint generation failed", e);
        }
    }

    /**
     * 校验原始值是否与已存储的指纹匹配。
     *
     * @param rawValue    原始值
     * @param storedHash  已存储的指纹
     * @return 匹配返回 true
     */
    public static boolean matches(String rawValue, String storedHash) {
        Objects.requireNonNull(rawValue, "rawValue must not be null");
        Objects.requireNonNull(storedHash, "storedHash must not be null");
        return fingerprint(rawValue).equals(storedHash);
    }

    private static String bytesToHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder(bytes.length * 2);
        for (byte b : bytes) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }
}
