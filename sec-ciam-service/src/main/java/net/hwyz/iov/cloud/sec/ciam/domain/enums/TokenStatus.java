package net.hwyz.iov.cloud.sec.ciam.domain.enums;

import lombok.Getter;

import java.util.Arrays;

/**
 * 刷新令牌状态枚举。
 * <p>
 * 对应表 {@code ciam_refresh_token.token_status}（TINYINT）。
 */
@Getter
public enum TokenStatus implements CodeEnum {

    ACTIVE(1, "有效"),
    ROTATED(2, "已轮换"),
    REVOKED(3, "已撤销"),
    EXPIRED(4, "已过期");

    private final int code;
    private final String description;

    TokenStatus(int code, String description) {
        this.code = code;
        this.description = description;
    }

    public static TokenStatus fromCode(int code) {
        return Arrays.stream(values())
                .filter(e -> e.code == code)
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("未知的令牌状态编码: " + code));
    }
}
