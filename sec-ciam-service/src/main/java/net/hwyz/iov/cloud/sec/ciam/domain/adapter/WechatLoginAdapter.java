package net.hwyz.iov.cloud.sec.ciam.domain.adapter;

/**
 * 微信登录适配器接口。
 * <p>
 * 通过微信授权码换取用户信息（openId、unionId、昵称、头像）。
 */
public interface WechatLoginAdapter {

    /**
     * 使用微信授权码获取用户信息。
     *
     * @param code 微信授权码
     * @return 第三方用户信息
     */
    ThirdPartyUserInfo getUserInfo(String code);
}
