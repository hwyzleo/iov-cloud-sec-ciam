package net.hwyz.iov.cloud.sec.ciam.service.domain.enums;

import lombok.Getter;

import java.util.Arrays;

/**
 * MFA 挑战场景枚举。
 * <p>
 * 对应表 {@code ciam_mfa_challenge.challenge_scene}（VARCHAR(32)）。
 */
@Getter
public enum ChallengeScene implements LabelEnum {

    NEW_DEVICE("new_device", "新设备登录"),
    GEO_CHANGE("geo_change", "异地登录"),
    HIGH_RISK("high_risk", "高风险操作");

    private final String code;
    private final String description;

    ChallengeScene(String code, String description) {
        this.code = code;
        this.description = description;
    }

    public static ChallengeScene fromCode(String code) {
        return Arrays.stream(values())
                .filter(e -> e.code.equals(code))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("未知的挑战场景: " + code));
    }
}
