package net.hwyz.iov.cloud.sec.ciam.service.domain.enums;

import lombok.Getter;

import java.util.Arrays;

/**
 * 同意状态枚举。
 * <p>
 * 对应表 {@code ciam_user_consent.consent_status}（TINYINT）。
 */
@Getter
public enum ConsentStatus implements CodeEnum {

    REVOKED(0, "撤回"),
    AGREED(1, "同意");

    private final int code;
    private final String description;

    ConsentStatus(int code, String description) {
        this.code = code;
        this.description = description;
    }

    public static ConsentStatus fromCode(int code) {
        return Arrays.stream(values())
                .filter(e -> e.code == code)
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("未知的同意状态编码: " + code));
    }
}
