package net.hwyz.iov.cloud.sec.ciam.service.domain.adapter;

/**
 * Google 登录适配器接口。
 * <p>
 * 验证 Google ID token 并返回用户信息。
 */
public interface GoogleLoginAdapter {

    /**
     * 验证 Google ID token。
     *
     * @param idToken Google 签发的 ID token
     * @return 第三方用户信息（subject 和 email）
     */
    ThirdPartyUserInfo verifyIdToken(String idToken);
}
