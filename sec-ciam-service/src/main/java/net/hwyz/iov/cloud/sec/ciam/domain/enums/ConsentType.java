package net.hwyz.iov.cloud.sec.ciam.domain.enums;

import lombok.Getter;

import java.util.Arrays;

/**
 * 同意类型枚举。
 * <p>
 * 对应表 {@code ciam_user_consent.consent_type}（VARCHAR(32)）。
 */
@Getter
public enum ConsentType implements LabelEnum {

    USER_AGREEMENT("user_agreement", "用户协议"),
    PRIVACY_POLICY("privacy_policy", "隐私政策"),
    MARKETING("marketing", "营销同意");

    private final String value;
    private final String description;

    ConsentType(String value, String description) {
        this.value = value;
        this.description = description;
    }

    public static ConsentType fromValue(String value) {
        return Arrays.stream(values())
                .filter(e -> e.value.equals(value))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("未知的同意类型: " + value));
    }
}
