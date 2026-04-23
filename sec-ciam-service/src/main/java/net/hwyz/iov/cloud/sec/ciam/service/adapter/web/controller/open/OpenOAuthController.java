package net.hwyz.iov.cloud.sec.ciam.service.adapter.web.controller.open;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import net.hwyz.iov.cloud.framework.common.bean.ApiResponse;
import net.hwyz.iov.cloud.sec.ciam.service.common.exception.CiamErrorCode;
import net.hwyz.iov.cloud.sec.ciam.service.adapter.web.controller.open.vo.*;
import net.hwyz.iov.cloud.sec.ciam.service.domain.service.AuthCodeExchangeResult;
import net.hwyz.iov.cloud.sec.ciam.service.domain.service.ClientCredentialsResult;
import net.hwyz.iov.cloud.sec.ciam.service.domain.service.DeviceAuthorizationResponse;
import net.hwyz.iov.cloud.sec.ciam.service.domain.service.DeviceAuthorizationResult;
import net.hwyz.iov.cloud.sec.ciam.service.domain.service.DeviceAuthorizationService;
import net.hwyz.iov.cloud.sec.ciam.service.domain.service.JwtTokenService;
import net.hwyz.iov.cloud.sec.ciam.service.domain.service.OAuthAuthorizationService;
import net.hwyz.iov.cloud.sec.ciam.service.domain.service.RefreshTokenDomainService;
import net.hwyz.iov.cloud.sec.ciam.service.domain.service.RefreshTokenRotationResult;
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

    private final OAuthAuthorizationService oAuthAuthorizationService;
    private final DeviceAuthorizationService deviceAuthorizationService;
    private final RefreshTokenDomainService refreshTokenDomainService;
    private final JwtTokenService jwtTokenService;

    /** 签发授权码（Authorization Code + PKCE） */
    @PostMapping("/authorize")
    public ApiResponse<Map<String, String>> authorize(@RequestBody @Valid AuthorizeRequest request) {
        String code = oAuthAuthorizationService.createAuthorizationCode(
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
            case "authorization_code" -> handleAuthorizationCodeGrant(
                    request.getCode(), request.getClientId(), request.getClientSecret(),
                    request.getRedirectUri(), request.getCodeVerifier());
            case "client_credentials" -> handleClientCredentialsGrant(
                    request.getClientId(), request.getClientSecret(), request.getScope());
            case "refresh_token" -> handleRefreshTokenGrant(
                    request.getRefreshToken(), request.getClientId());
            case "urn:ietf:params:oauth:grant-type:device_code" -> handleDeviceCodeGrant(
                    request.getDeviceCode(), request.getClientId());
            default -> ApiResponse.fail(CiamErrorCode.INVALID_PARAM,
                    "不支持的授权类型: " + request.getGrantType());
        };
    }

    /** 发起设备授权 */
    @PostMapping("/device")
    public ApiResponse<DeviceAuthorizationResponse> deviceAuthorize(@RequestBody @Valid DeviceAuthorizeRequest request) {
        DeviceAuthorizationResponse response = deviceAuthorizationService.initiateDeviceAuthorization(
                request.getClientId(), request.getScope());
        return ApiResponse.ok(response);
    }

    /** 用户确认设备授权 */
    @PostMapping("/device/approve")
    public ApiResponse<Void> approveDevice(@RequestBody @Valid ApproveDeviceRequest request) {
        deviceAuthorizationService.approveDeviceAuthorization(request.getUserCode(), request.getUserId());
        return ApiResponse.ok();
    }

    /** 令牌撤销 */
    @PostMapping("/revoke")
    public ApiResponse<Void> revokeToken(@RequestBody @Valid RevokeTokenRequest request) {
        refreshTokenDomainService.revokeRefreshToken(request.getToken());
        return ApiResponse.ok();
    }

    // ---- 内部方法 ----

    private ApiResponse<Map<String, Object>> handleAuthorizationCodeGrant(
            String code, String clientId, String clientSecret, String redirectUri, String codeVerifier) {
        AuthCodeExchangeResult exchangeResult = oAuthAuthorizationService.exchangeCode(
                code, clientId, clientSecret, redirectUri, codeVerifier);

        int accessTokenTtl = 1800; // 默认 30 分钟
        String accessToken = jwtTokenService.generateAccessToken(
                exchangeResult.getUserId(), exchangeResult.getClientId(),
                exchangeResult.getScope(), exchangeResult.getSessionId(), accessTokenTtl);

        String newRefreshToken = refreshTokenDomainService.issueRefreshToken(
                exchangeResult.getUserId(), exchangeResult.getSessionId(),
                exchangeResult.getClientId(), 2592000);

        return ApiResponse.ok(buildTokenResponse(accessToken, newRefreshToken, accessTokenTtl, exchangeResult.getScope()));
    }

    private ApiResponse<Map<String, Object>> handleClientCredentialsGrant(
            String clientId, String clientSecret, String scope) {
        ClientCredentialsResult result = oAuthAuthorizationService.clientCredentialsGrant(clientId, clientSecret, scope);

        String accessToken = jwtTokenService.generateAccessToken(
                null, result.getClientId(), result.getScope(), null, result.getAccessTokenTtl());

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("access_token", accessToken);
        data.put("token_type", "Bearer");
        data.put("expires_in", result.getAccessTokenTtl());
        data.put("scope", result.getScope());
        return ApiResponse.ok(data);
    }

    private ApiResponse<Map<String, Object>> handleRefreshTokenGrant(String refreshToken, String clientId) {
        RefreshTokenRotationResult rotationResult = refreshTokenDomainService.rotateRefreshToken(refreshToken, clientId);

        int accessTokenTtl = 1800;
        String accessToken = jwtTokenService.generateAccessToken(
                rotationResult.getUserId(), clientId,
                rotationResult.getScope(), rotationResult.getSessionId(), accessTokenTtl);

        return ApiResponse.ok(buildTokenResponse(accessToken, rotationResult.getNewRefreshToken(), accessTokenTtl, rotationResult.getScope()));
    }

    private ApiResponse<Map<String, Object>> handleDeviceCodeGrant(String deviceCode, String clientId) {
        DeviceAuthorizationResult result = deviceAuthorizationService.pollDeviceAuthorization(deviceCode, clientId);

        int accessTokenTtl = 1800;
        String accessToken = jwtTokenService.generateAccessToken(
                result.getUserId(), result.getClientId(), result.getScope(), null, accessTokenTtl);

        String newRefreshToken = refreshTokenDomainService.issueRefreshToken(
                result.getUserId(), null, result.getClientId(), 2592000);

        return ApiResponse.ok(buildTokenResponse(accessToken, newRefreshToken, accessTokenTtl, result.getScope()));
    }

    private Map<String, Object> buildTokenResponse(String accessToken, String refreshToken,
                                                    int expiresIn, String scope) {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("access_token", accessToken);
        data.put("token_type", "Bearer");
        data.put("expires_in", expiresIn);
        data.put("refresh_token", refreshToken);
        data.put("scope", scope);
        return data;
    }
}
