package net.hwyz.iov.cloud.sec.ciam.domain.adapter;

/**
 * Apple 登录适配器接口。
 * <p>
 * 验证 Apple identity token 并返回用户信息。
 */
public interface AppleLoginAdapter {

    /**
     * 验证 Apple identity token。
     *
     * @param identityToken Apple 签发的 identity token
     * @return 第三方用户信息（subject 为 Apple 用户标识）
     */
    ThirdPartyUserInfo verifyIdentityToken(String identityToken);
}
