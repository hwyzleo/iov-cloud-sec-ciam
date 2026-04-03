package net.hwyz.iov.cloud.sec.ciam.domain.enums;

import lombok.Getter;

import java.util.Arrays;

/**
 * OAuth 客户端类型枚举。
 * <p>
 * 对应表 {@code ciam_oauth_client.client_type}（VARCHAR(32)）。
 * 与终端类型 {@link ClientType} 区分，此枚举描述 OAuth 协议层面的客户端分类。
 */
@Getter
public enum OAuthClientType implements LabelEnum {

    PUBLIC("public", "公开客户端"),
    CONFIDENTIAL("confidential", "机密客户端"),
    INTERNAL("internal", "内部客户端");

    private final String value;
    private final String description;

    OAuthClientType(String value, String description) {
        this.value = value;
        this.description = description;
    }

    public static OAuthClientType fromValue(String value) {
        return Arrays.stream(values())
                .filter(e -> e.value.equals(value))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("未知的OAuth客户端类型: " + value));
    }
}
