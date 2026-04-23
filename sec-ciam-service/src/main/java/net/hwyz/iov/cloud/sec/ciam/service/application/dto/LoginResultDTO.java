package net.hwyz.iov.cloud.sec.ciam.service.application.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import net.hwyz.iov.cloud.sec.ciam.service.domain.gateway.CaptchaChallenge;

/**
 * 登录结果 DTO。
 * <p>
 * 封装登录/注册的返回信息，支持密码登录挑战场景。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LoginResultDto {

    /** 用户业务唯一标识 */
    private String userId;

    /** 是否为新注册用户 */
    private boolean newUser;

    /** Access Token */
    private String accessToken;

    /** Refresh Token */
    private String refreshToken;

    /** Access Token 有效期（秒） */
    private Integer accessTokenTtl;

    /** 会话 ID（占位，后续由会话模块填充） */
    private String sessionId;

    /** 是否需要图形验证码挑战（密码错误 ≥3 次时触发） */
    private boolean challengeRequired;

    /** 图形验证码挑战信息（challengeRequired=true 时返回） */
    private CaptchaChallenge captchaChallenge;

    /** 是否需要回退到短信验证码登录（本机手机号认证不可用时返回 true） */
    private boolean fallbackRequired;
}
