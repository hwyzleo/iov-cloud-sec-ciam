package net.hwyz.iov.cloud.sec.ciam.service.domain.service;

import lombok.Getter;

/**
 * 设备授权轮询结果。
 * <p>
 * 用户确认授权后，设备端轮询获取的授权结果，包含用户标识、授权范围和客户端标识，
 * 供后续 Token 签发流程使用。
 */
@Getter
public class DeviceAuthorizationResult {

    private final String userId;
    private final String scope;
    private final String clientId;

    public DeviceAuthorizationResult(String userId, String scope, String clientId) {
        this.userId = userId;
        this.scope = scope;
        this.clientId = clientId;
    }
}
