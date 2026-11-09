package net.hwyz.iov.cloud.sec.ciam.service.domain.service;

import lombok.Builder;
import lombok.Getter;

import java.time.Instant;

/**
 * Access Token 签发上下文（SEC-CIAM-DSN-CR-003 RD-003-5，§4.1/§4.4）。
 * <p>
 * 手机登录/刷新链路使用完整上下文签发，确保 Token 携带与持久会话一致的
 * sub、client_id、client_type、device_id、session_id、scope。
 * <p>
 * {@code expireBound} 为绝对到期边界（会话 {@code expire_time}）：
 * {@code exp = min(now + ttlSeconds, expireBound)}，防止刷新无限续期（RD-003-7）。
 */
@Getter
@Builder
public class AccessTokenContext {

    /** 用户业务唯一标识（sub） */
    private final String userId;
    /** 设备标识（device_id） */
    private final String deviceId;
    /** OAuth 客户端标识（client_id） */
    private final String clientId;
    /** 终端类型（client_type，手机为 MOBILE） */
    private final String clientType;
    /** 授权范围（scope，空格分隔） */
    private final String scope;
    /** 会话业务唯一标识（session_id） */
    private final String sessionId;
    /** 相对有效期（秒） */
    private final int ttlSeconds;
    /** 绝对到期边界（可为 null，表示不额外收紧） */
    private final Instant expireBound;
}
