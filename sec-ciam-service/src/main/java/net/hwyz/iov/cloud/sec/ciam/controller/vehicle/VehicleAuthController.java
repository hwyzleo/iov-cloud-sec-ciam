package net.hwyz.iov.cloud.sec.ciam.controller.vehicle;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import net.hwyz.iov.cloud.framework.common.bean.ApiResponse;
import net.hwyz.iov.cloud.sec.ciam.controller.vehicle.dto.PollDeviceTokenRequest;
import net.hwyz.iov.cloud.sec.ciam.controller.vehicle.dto.VehicleDeviceAuthorizeRequest;
import net.hwyz.iov.cloud.sec.ciam.domain.service.DeviceAuthorizationResponse;
import net.hwyz.iov.cloud.sec.ciam.domain.service.DeviceAuthorizationService;
import net.hwyz.iov.cloud.sec.ciam.domain.service.JwtTokenService;
import net.hwyz.iov.cloud.sec.ciam.domain.service.RefreshTokenDomainService;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 车机端认证控制器。
 * 提供扫码登录、Device Authorization Grant、车机会话管理等能力。
 */
@RestController
@RequestMapping("/api/vehicle/v1/auth")
@RequiredArgsConstructor
public class VehicleAuthController {

    private final DeviceAuthorizationService deviceAuthorizationService;
    private final RefreshTokenDomainService refreshTokenDomainService;
    private final JwtTokenService jwtTokenService;

    @PostMapping("/device/authorize")
    public ApiResponse<DeviceAuthorizationResponse> deviceAuthorize(
            @RequestBody @Valid VehicleDeviceAuthorizeRequest request) {
        DeviceAuthorizationResponse response = deviceAuthorizationService.initiateDeviceAuthorization(
                request.getClientId(), request.getScope());
        return ApiResponse.ok(response);
    }

    @PostMapping("/device/token")
    public ApiResponse<Map<String, Object>> pollDeviceToken(
            @RequestBody @Valid PollDeviceTokenRequest request) {
        var result = deviceAuthorizationService.pollDeviceAuthorization(
                request.getDeviceCode(), request.getClientId());
        String accessToken = jwtTokenService.generateAccessToken(
                result.getUserId(), request.getClientId(), result.getScope(), null, 1800);
        return ApiResponse.ok(Map.of(
                "access_token", accessToken,
                "token_type", "Bearer",
                "expires_in", 1800
        ));
    }
}
