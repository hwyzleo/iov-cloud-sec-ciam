package net.hwyz.iov.cloud.sec.ciam.domain.enums;

import lombok.Getter;

import java.util.Arrays;

/**
 * MFA 挑战状态枚举。
 * <p>
 * 对应表 {@code ciam_mfa_challenge.challenge_status}（TINYINT）。
 */
@Getter
public enum ChallengeStatus implements CodeEnum {

    PENDING(0, "待验证"),
    PASSED(1, "通过"),
    FAILED(2, "失败"),
    EXPIRED(3, "过期"),
    CANCELLED(4, "取消");

    private final int code;
    private final String description;

    ChallengeStatus(int code, String description) {
        this.code = code;
        this.description = description;
    }

    public static ChallengeStatus fromCode(int code) {
        return Arrays.stream(values())
                .filter(e -> e.code == code)
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("未知的挑战状态编码: " + code));
    }
}
