package net.hwyz.iov.cloud.sec.ciam.domain.enums;

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

    private final String value;
    private final String description;

    ChallengeScene(String value, String description) {
        this.value = value;
        this.description = description;
    }

    public static ChallengeScene fromValue(String value) {
        return Arrays.stream(values())
                .filter(e -> e.value.equals(value))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("未知的挑战场景: " + value));
    }
}
