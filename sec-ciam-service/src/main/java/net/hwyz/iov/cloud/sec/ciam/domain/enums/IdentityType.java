package net.hwyz.iov.cloud.sec.ciam.domain.enums;

import lombok.Getter;

import java.util.Arrays;

/**
 * 登录标识类型枚举。
 * <p>
 * 对应表 {@code ciam_user_identity.identity_type}（VARCHAR(32)）。
 */
@Getter
public enum IdentityType implements LabelEnum {

    MOBILE("mobile", "手机号"),
    EMAIL("email", "邮箱"),
    WECHAT("wechat", "微信"),
    APPLE("apple", "Apple"),
    GOOGLE("google", "Google"),
    LOCAL_MOBILE("local_mobile", "本机手机号");

    private final String value;
    private final String description;

    IdentityType(String value, String description) {
        this.value = value;
        this.description = description;
    }

    public static IdentityType fromValue(String value) {
        return Arrays.stream(values())
                .filter(e -> e.value.equals(value))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("未知的标识类型: " + value));
    }
}
