package net.hwyz.iov.cloud.sec.ciam.service.domain.service;

import lombok.Getter;

import java.time.Instant;

/**
 * Refresh Token 轮换结果 DTO（SEC-CIAM-DSN-CR-003 RD-003-7，§4.4/§5.2）。
 * <p>
 * 包含新签发的 Refresh Token 原始值以及**从持久会话恢复**的完整认证上下文，
 * 供上层据此签发新的 Access Token。刷新不再从请求重新接受 device/clientType/scope。
 */
@Getter
public class RefreshTokenRotationResult {

    /** 新签发的 Refresh Token 原始值（仅此次返回） */
    private final String newRefreshToken;
    /** 用户业务唯一标识 */
    private final String userId;
    /** 会话业务唯一标识 */
    private final String sessionId;
    /** OAuth 客户端标识 */
    private final String clientId;
    /** 终端类型（手机为 MOBILE；范围外流程可为 null） */
    private final String clientType;
    /** 设备标识（手机链路必填） */
    private final String deviceId;
    /** 登录时冻结的授权范围 */
    private final String scope;
    /** 绝对到期边界（继承父令牌/会话，不延长） */
    private final Instant expireTime;

    public RefreshTokenRotationResult(String newRefreshToken,
                                      String userId,
                                      String sessionId,
                                      String clientId,
                                      String clientType,
                                      String deviceId,
                                      String scope,
                                      Instant expireTime) {
        this.newRefreshToken = newRefreshToken;
        this.userId = userId;
        this.sessionId = sessionId;
        this.clientId = clientId;
        this.clientType = clientType;
        this.deviceId = deviceId;
        this.scope = scope;
        this.expireTime = expireTime;
    }
}
