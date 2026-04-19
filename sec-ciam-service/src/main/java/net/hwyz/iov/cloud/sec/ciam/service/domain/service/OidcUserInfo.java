package net.hwyz.iov.cloud.sec.ciam.service.domain.service;

import lombok.Builder;
import lombok.Getter;

/**
 * OIDC UserInfo 响应 DTO。
 * <p>
 * 遵循 OpenID Connect Core 1.0 标准声明命名，
 * {@code sub} 使用平台全局唯一用户 ID。
 */
@Getter
@Builder
public class OidcUserInfo {

    /** 用户唯一标识（OIDC sub claim） */
    private final String sub;

    /** 昵称 */
    private final String name;

    /** 头像地址 */
    private final String picture;

    /** 性别：male / female / unknown */
    private final String gender;

    /** 生日（ISO 8601 日期格式，如 2000-01-15） */
    private final String birthdate;

    /** 邮箱 */
    private final String email;

    /** 手机号 */
    private final String phoneNumber;
}
