package net.hwyz.iov.cloud.sec.ciam.service.domain.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 注册来源枚举
 */
@Getter
@AllArgsConstructor
public enum RegisterSource {

    // 移动端注册来源
    MOBILE("mobile", "手机号注册"),
    EMAIL("email", "邮箱注册"),
    WECHAT("wechat", "微信注册"),
    APPLE("apple", "Apple注册"),
    GOOGLE("google", "Google注册"),
    WECHAT_MINI_PROGRAM("wechat_mini_program", "微信小程序注册"),
    WECHAT_H5("wechat_h5", "微信H5注册"),
    APP_IOS("app_ios", "iOS App注册"),
    APP_ANDROID("app_android", "Android App注册"),

    // 后台管理注册来源
    ADMIN_MOBILE("admin_mobile", "后台手机号创建"),
    ADMIN_EMAIL("admin_email", "后台邮箱创建"),

    // 本机手机号注册来源
    LOCAL_MOBILE("local_mobile", "本机手机号注册");

    private final String code;
    private final String description;

    public static RegisterSource fromCode(String code) {
        for (RegisterSource source : values()) {
            if (source.getCode().equals(code)) {
                return source;
            }
        }
        return null;
    }

    public static RegisterSource fromIdentityType(IdentityType identityType) {
        if (identityType == null) {
            return null;
        }
        return switch (identityType) {
            case MOBILE -> MOBILE;
            case EMAIL -> EMAIL;
            case WECHAT -> WECHAT;
            case APPLE -> APPLE;
            case GOOGLE -> GOOGLE;
            case LOCAL_MOBILE -> LOCAL_MOBILE;
        };
    }
}
