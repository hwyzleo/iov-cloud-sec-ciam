package net.hwyz.iov.cloud.sec.ciam.domain.service;

import lombok.Getter;

/**
 * 授权码交换结果。
 * <p>
 * 授权码成功交换后返回用户标识、会话标识、授权范围和客户端标识，
 * 供后续 Token 签发流程使用。
 */
@Getter
public class AuthCodeExchangeResult {

    private final String userId;
    private final String sessionId;
    private final String scope;
    private final String clientId;

    public AuthCodeExchangeResult(String userId, String sessionId, String scope, String clientId) {
        this.userId = userId;
        this.sessionId = sessionId;
        this.scope = scope;
        this.clientId = clientId;
    }
}
