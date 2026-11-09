package net.hwyz.iov.cloud.sec.ciam.service.domain.enums;

import lombok.Getter;

import java.util.Arrays;

/**
 * 客户端类型枚举（终端类型）。
 * <p>
 * 对应表 {@code ciam_session.client_type}、{@code ciam_device.client_type} 等（VARCHAR(32)）。
 * <p>
 * 注意：{@link #MOBILE} 与 SGW 令牌校验契约的大写 {@code MOBILE} 保持一致，
 * 历史终端类型仍使用小写编码。
 */
@Getter
public enum ClientType implements LabelEnum {

    APP("app", "手机App"),
    MINI_PROGRAM("mini_program", "小程序"),
    WEB("web", "官网"),
    VEHICLE("vehicle", "车机"),
    ADMIN("admin", "运营后台"),

    /**
     * 手机认证终端类型（SEC-CIAM-DSN-CR-003 §4.1/§6.2）。
     * <p>
     * 取值必须与 SGW 令牌校验契约（framework {@code ClientType.MOBILE}）一致，
     * 因此使用大写常量名而不是历史的小写终端编码；仅用于手机验证码登录链路，
     * 同时写入 {@code ciam_session.client_type} 与 JWT {@code client_type} 声明。
     */
    MOBILE("MOBILE", "手机App（手机认证）");

    private final String code;
    private final String description;

    ClientType(String code, String description) {
        this.code = code;
        this.description = description;
    }

    @Override
    public String getDescription() {
        return description;
    }

    public static ClientType fromCode(String code) {
        return Arrays.stream(values())
                .filter(e -> e.code.equals(code))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("未知的客户端类型: " + code));
    }
}
