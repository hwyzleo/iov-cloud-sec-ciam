package net.hwyz.iov.cloud.sec.ciam.service.domain.adapter;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 第三方登录返回的用户信息。
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ThirdPartyUserInfo {

    /** 第三方主体标识（openId / subject） */
    private String subject;

    /** 联合标识（微信 unionId 等，可为 null） */
    private String unionId;

    /** 昵称 */
    private String nickname;

    /** 头像地址 */
    private String avatarUrl;

    /** 邮箱（Apple / Google 场景可能返回） */
    private String email;
}
