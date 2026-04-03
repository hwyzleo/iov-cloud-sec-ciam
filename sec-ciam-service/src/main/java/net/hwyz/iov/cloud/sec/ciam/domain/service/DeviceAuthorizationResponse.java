package net.hwyz.iov.cloud.sec.ciam.domain.service;

import lombok.Getter;

/**
 * 设备授权响应。
 * <p>
 * 设备发起授权请求后返回设备码、用户码、验证地址、过期时间和轮询间隔，
 * 供设备端展示和轮询使用。
 */
@Getter
public class DeviceAuthorizationResponse {

    private final String deviceCode;
    private final String userCode;
    private final String verificationUri;
    private final int expiresIn;
    private final int interval;

    public DeviceAuthorizationResponse(String deviceCode, String userCode,
                                       String verificationUri, int expiresIn, int interval) {
        this.deviceCode = deviceCode;
        this.userCode = userCode;
        this.verificationUri = verificationUri;
        this.expiresIn = expiresIn;
        this.interval = interval;
    }
}
