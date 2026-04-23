package net.hwyz.iov.cloud.sec.ciam.service.application.service;

import lombok.RequiredArgsConstructor;
import net.hwyz.iov.cloud.sec.ciam.service.domain.service.DeviceAuthorizationResponse;
import net.hwyz.iov.cloud.sec.ciam.service.domain.service.DeviceAuthorizationService;
import net.hwyz.iov.cloud.sec.ciam.service.domain.service.JwtTokenService;
import org.springframework.stereotype.Service;

import java.util.Map;

/**
 * 车机认证应用服务 — 封装车机扫码登录与设备授权轮询逻辑。
 */
@Service
@RequiredArgsConstructor
public class VehicleAuthAppService {

    private final DeviceAuthorizationService deviceAuthorizationService;
    private final JwtTokenService jwtTokenService;

    /** 发起设备授权 */
    public DeviceAuthorizationResponse initiateDeviceAuthorization(String clientId, String scope) {
        return deviceAuthorizationService.initiateDeviceAuthorization(clientId, scope);
    }

    /** 轮询设备令牌 */
    public Map<String, Object> pollDeviceToken(String deviceCode, String clientId) {
        var result = deviceAuthorizationService.pollDeviceAuthorization(deviceCode, clientId);
        String accessToken = jwtTokenService.generateAccessToken(
                result.getUserId(), clientId, result.getScope(), null, 1800);
        return Map.of(
                "access_token", accessToken,
                "token_type", "Bearer",
                "expires_in", 1800
        );
    }
}
