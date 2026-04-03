package net.hwyz.iov.cloud.sec.ciam.domain.service;

import lombok.Getter;

/**
 * 验证码类型枚举。
 */
@Getter
public enum VerificationCodeType {

    /** 短信验证码，有效期 5 分钟 */
    SMS(5 * 60),

    /** 邮箱验证码，有效期 30 分钟 */
    EMAIL(30 * 60);

    /** 验证码有效期（秒） */
    private final int ttlSeconds;

    VerificationCodeType(int ttlSeconds) {
        this.ttlSeconds = ttlSeconds;
    }
}
