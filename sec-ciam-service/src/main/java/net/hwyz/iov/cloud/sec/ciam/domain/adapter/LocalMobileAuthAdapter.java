package net.hwyz.iov.cloud.sec.ciam.domain.adapter;

/**
 * 本机手机号认证适配器接口。
 * <p>
 * 验证运营商本机号码认证 token，返回手机号。
 */
public interface LocalMobileAuthAdapter {

    /**
     * 验证本机号码认证 token。
     *
     * @param token 运营商认证 token
     * @return 手机号（验证成功时），验证失败时返回 null
     */
    String verifyToken(String token);
}
