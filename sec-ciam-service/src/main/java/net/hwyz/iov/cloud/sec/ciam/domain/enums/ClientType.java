package net.hwyz.iov.cloud.sec.ciam.domain.enums;

import lombok.Getter;

import java.util.Arrays;

/**
 * 客户端类型枚举（终端类型）。
 * <p>
 * 对应表 {@code ciam_session.client_type}、{@code ciam_device.client_type} 等（VARCHAR(32)）。
 */
@Getter
public enum ClientType implements LabelEnum {

    APP("app", "手机App"),
    MINI_PROGRAM("mini_program", "小程序"),
    WEB("web", "官网"),
    VEHICLE("vehicle", "车机"),
    ADMIN("admin", "运营后台");

    private final String value;
    private final String description;

    ClientType(String value, String description) {
        this.value = value;
        this.description = description;
    }

    public static ClientType fromValue(String value) {
        return Arrays.stream(values())
                .filter(e -> e.value.equals(value))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("未知的客户端类型: " + value));
    }
}
