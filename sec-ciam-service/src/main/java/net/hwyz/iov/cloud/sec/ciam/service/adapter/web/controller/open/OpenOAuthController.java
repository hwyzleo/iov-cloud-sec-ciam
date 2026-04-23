package net.hwyz.iov.cloud.sec.ciam.service.adapter.web.controller.open;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import net.hwyz.iov.cloud.framework.common.bean.ApiResponse;
import net.hwyz.iov.cloud.sec.ciam.service.common.exception.CiamErrorCode;
import net.hwyz.iov.cloud.sec.ciam.service.adapter.web.controller.open.vo.*;
import net.hwyz.iov.cloud.sec.ciam.service.application.service.OAuthAppService;
import net.hwyz.iov.cloud.sec.ciam.service.domain.service.DeviceAuthorizationResponse;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * OAuth 2.0 控制器 — 授权码+PKCE、令牌交换、Client Credentials、设备授权、刷新令牌、令牌撤销。
 */
@RestController
@RequestMapping("/api/open/oauth/v1")
@RequiredArgsConstructor
public class OpenOAuthController {

    private final OAuthAppService oAuthAppService;

    /** 签发授权码（Authorization Code + PKCE） */
    @PostMapping("/authorize")
    public ApiResponse<Map<String, String>> authorize(@RequestBody @Valid AuthorizeRequest request) {
        String code = oAuthAppService.createAuthorizationCode(
                request.getClientId(), request.getUserId(), request.getSessionId(),
                request.getRedirectUri(), request.getScope(),
                request.getCodeChallenge(), request.getChallengeMethod());
        Map<String, String> data = new LinkedHashMap<>();
        data.put("code", code);
        data.put("redirectUri", request.getRedirectUri());
        return ApiResponse.ok(data);
    }

    /** 令牌交换（授权码 → Access Token + Refresh Token） */
    @PostMapping("/token")
    public ApiResponse<Map<String, Object>> token(@RequestBody @Valid TokenRequest request) {
        return switch (request.getGrantType()) {
            case "authorization_code" -> ApiResponse.ok(oAuthAppService.handleAuthorizationCodeGrant(
                    request.getCode(), request.getClientId(), request.getClientSecret(),
                    request.getRedirectUri(), request.getCodeVerifier()));
            case "client_credentials" -> ApiResponse.ok(oAuthAppService.handleClientCredentialsGrant(
                    request.getClientId(), request.getClientSecret(), request.getScope()));
            case "refresh_token" -> ApiResponse.ok(oAuthAppService.handleRefreshTokenGrant(
                    request.getRefreshToken(), request.getClientId()));
            case "urn:ietf:params:oauth:grant-type:device_code" -> ApiResponse.ok(oAuthAppService.handleDeviceCodeGrant(
                    request.getDeviceCode(), request.getClientId()));
            default -> ApiResponse.fail(CiamErrorCode.INVALID_PARAM,
                    "不支持的授权类型: " + request.getGrantType());
        };
    }

    /** 发起设备授权 */
    @PostMapping("/device")
    public ApiResponse<DeviceAuthorizationResponse> deviceAuthorize(@RequestBody @Valid DeviceAuthorizeRequest request) {
        return ApiResponse.ok(oAuthAppService.initiateDeviceAuthorization(request.getClientId(), request.getScope()));
    }

    /** 用户确认设备授权 */
    @PostMapping("/device/approve")
    public ApiResponse<Void> approveDevice(@RequestBody @Valid ApproveDeviceRequest request) {
        oAuthAppService.approveDeviceAuthorization(request.getUserCode(), request.getUserId());
        return ApiResponse.ok();
    }

    /** 令牌撤销 */
    @PostMapping("/revoke")
    public ApiResponse<Void> revokeToken(@RequestBody @Valid RevokeTokenRequest request) {
        oAuthAppService.revokeToken(request.getToken());
        return ApiResponse.ok();
    }

}
