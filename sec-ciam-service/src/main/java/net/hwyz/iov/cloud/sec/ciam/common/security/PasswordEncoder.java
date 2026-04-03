package net.hwyz.iov.cloud.sec.ciam.common.security;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import java.util.Objects;

/**
 * 密码编码器。
 * <p>
 * 基于 BCrypt 对密码进行强哈希存储，用于 {@code credential_hash} 列。
 * 不保存明文密码，不可逆。
 * <p>
 * 默认使用 BCrypt 强度 10（Spring Security 默认值）。
 */
public final class PasswordEncoder {

    /** BCrypt 哈希算法标识，写入 {@code hash_algorithm} 列 */
    public static final String ALGORITHM = "BCRYPT";

    private final BCryptPasswordEncoder delegate;

    public PasswordEncoder() {
        this.delegate = new BCryptPasswordEncoder();
    }

    /**
     * 指定 BCrypt 强度构造。
     *
     * @param strength BCrypt log rounds（4 ~ 31）
     */
    public PasswordEncoder(int strength) {
        this.delegate = new BCryptPasswordEncoder(strength);
    }

    /**
     * 对原始密码进行 BCrypt 哈希。
     *
     * @param rawPassword 原始密码
     * @return BCrypt 哈希值
     */
    public String encode(String rawPassword) {
        Objects.requireNonNull(rawPassword, "rawPassword must not be null");
        return delegate.encode(rawPassword);
    }

    /**
     * 校验原始密码是否与已存储的哈希匹配。
     *
     * @param rawPassword    原始密码
     * @param encodedPassword 已存储的 BCrypt 哈希
     * @return 匹配返回 true
     */
    public boolean matches(String rawPassword, String encodedPassword) {
        Objects.requireNonNull(rawPassword, "rawPassword must not be null");
        Objects.requireNonNull(encodedPassword, "encodedPassword must not be null");
        return delegate.matches(rawPassword, encodedPassword);
    }
}
