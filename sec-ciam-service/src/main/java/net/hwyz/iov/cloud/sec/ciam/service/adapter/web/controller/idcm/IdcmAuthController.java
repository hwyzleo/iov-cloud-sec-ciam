package net.hwyz.iov.cloud.sec.ciam.service.adapter.web.controller.idcm;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import net.hwyz.iov.cloud.framework.common.bean.ApiResponse;
import net.hwyz.iov.cloud.sec.ciam.service.adapter.web.controller.idcm.vo.PollDeviceTokenRequest;
import net.hwyz.iov.cloud.sec.ciam.service.adapter.web.controller.idcm.vo.VehicleDeviceAuthorizeRequest;
import net.hwyz.iov.cloud.sec.ciam.service.application.service.VehicleAuthAppService;
import net.hwyz.iov.cloud.sec.ciam.service.domain.service.DeviceAuthorizationResponse;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 车机端认证控制器。
 * 提供扫码登录、Device Authorization Grant、车机会话管理等能力。
 */
@RestController
@RequestMapping("/api/idcm/auth/v1")
@RequiredArgsConstructor
public class IdcmAuthController {

    private final VehicleAuthAppService vehicleAuthAppService;

    @PostMapping("/device/authorize")
    public ApiResponse<DeviceAuthorizationResponse> deviceAuthorize(
            @RequestBody @Valid VehicleDeviceAuthorizeRequest request) {
        return ApiResponse.ok(vehicleAuthAppService.initiateDeviceAuthorization(
                request.getClientId(), request.getScope()));
    }

    @PostMapping("/device/token")
    public ApiResponse<Map<String, Object>> pollDeviceToken(
            @RequestBody @Valid PollDeviceTokenRequest request) {
        return ApiResponse.ok(vehicleAuthAppService.pollDeviceToken(
                request.getDeviceCode(), request.getClientId()));
    }
}
