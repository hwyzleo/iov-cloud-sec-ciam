package net.hwyz.iov.cloud.sec.ciam.domain.service;

import lombok.Getter;

/**
 * Refresh Token 轮换结果 DTO。
 * <p>
 * 包含新签发的 Refresh Token 原始值以及关联的用户、会话、授权范围信息，
 * 供上层服务据此签发新的 Access Token。
 */
@Getter
public class RefreshTokenRotationResult {

    /** 新签发的 Refresh Token 原始值（仅此次返回） */
    private final String newRefreshToken;
    /** 用户业务唯一标识 */
    private final String userId;
    /** 会话业务唯一标识 */
    private final String sessionId;
    /** 授权范围（继承自旧令牌关联的客户端配置） */
    private final String scope;

    public RefreshTokenRotationResult(String newRefreshToken,
                                      String userId,
                                      String sessionId,
                                      String scope) {
        this.newRefreshToken = newRefreshToken;
        this.userId = userId;
        this.sessionId = sessionId;
        this.scope = scope;
    }
}
