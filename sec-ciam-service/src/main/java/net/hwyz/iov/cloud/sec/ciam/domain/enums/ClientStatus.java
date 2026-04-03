package net.hwyz.iov.cloud.sec.ciam.domain.enums;

import lombok.Getter;

import java.util.Arrays;

/**
 * OAuth 客户端状态枚举。
 * <p>
 * 对应表 {@code ciam_oauth_client.client_status}（TINYINT）。
 */
@Getter
public enum ClientStatus implements CodeEnum {

    DISABLED(0, "停用"),
    ENABLED(1, "启用");

    private final int code;
    private final String description;

    ClientStatus(int code, String description) {
        this.code = code;
        this.description = description;
    }

    public static ClientStatus fromCode(int code) {
        return Arrays.stream(values())
                .filter(e -> e.code == code)
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("未知的客户端状态编码: " + code));
    }
}
