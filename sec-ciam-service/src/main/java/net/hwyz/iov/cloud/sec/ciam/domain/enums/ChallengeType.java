package net.hwyz.iov.cloud.sec.ciam.domain.enums;

import lombok.Getter;

import java.util.Arrays;

/**
 * MFA 挑战类型枚举。
 * <p>
 * 对应表 {@code ciam_mfa_challenge.challenge_type}（VARCHAR(32)）。
 */
@Getter
public enum ChallengeType implements LabelEnum {

    SMS("sms", "短信验证码"),
    EMAIL("email", "邮箱验证码");

    private final String code;
    private final String description;

    ChallengeType(String code, String description) {
        this.code = code;
        this.description = description;
    }

    public static ChallengeType fromCode(String code) {
        return Arrays.stream(values())
                .filter(e -> e.code.equals(code))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("未知的挑战类型: " + code));
    }
}
