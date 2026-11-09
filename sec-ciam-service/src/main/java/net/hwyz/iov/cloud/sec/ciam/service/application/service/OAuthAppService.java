package net.hwyz.iov.cloud.sec.ciam.service.application.service;

import lombok.RequiredArgsConstructor;
import net.hwyz.iov.cloud.sec.ciam.service.domain.service.*;
import net.hwyz.iov.cloud.sec.ciam.service.domain.model.RefreshToken;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * OAuth & OIDC 应用服务 — 封装授权码流程、令牌交换、设备授权、OIDC 信息等逻辑。
 */
@Service
@RequiredArgsConstructor
public class OAuthAppService {

    private final OAuthAuthorizationService oAuthAuthorizationService;
    private final DeviceAuthorizationService deviceAuthorizationService;
    private final RefreshTokenDomainService refreshTokenDomainService;
    private final JwtTokenService jwtTokenService;
    private final OidcService oidcService;

    /** 签发授权码 */
    public String createAuthorizationCode(String clientId, String userId, String sessionId,
                                          String redirectUri, String scope,
                                          String codeChallenge, String challengeMethod) {
        return oAuthAuthorizationService.createAuthorizationCode(
                clientId, userId, sessionId, redirectUri, scope, codeChallenge, challengeMethod);
    }

    /** 处理授权码授权 */
    public Map<String, Object> handleAuthorizationCodeGrant(String code, String clientId, String clientSecret,
                                                            String redirectUri, String codeVerifier) {
        AuthCodeExchangeResult exchangeResult = oAuthAuthorizationService.exchangeCode(
                code, clientId, clientSecret, redirectUri, codeVerifier);

        int accessTokenTtl = 1800;
        String accessToken = jwtTokenService.generateAccessToken(
                exchangeResult.getUserId(), null, exchangeResult.getClientId(),
                exchangeResult.getScope(), exchangeResult.getSessionId(), accessTokenTtl);

        String refreshToken = refreshTokenDomainService.issueRefreshToken(
                exchangeResult.getUserId(), exchangeResult.getSessionId(),
                exchangeResult.getClientId(), 2592000);

        return buildTokenResponse(accessToken, refreshToken, accessTokenTtl, exchangeResult.getScope());
    }

    /** 处理客户端凭据授权 */
    public Map<String, Object> handleClientCredentialsGrant(String clientId, String clientSecret, String scope) {
        ClientCredentialsResult result = oAuthAuthorizationService.clientCredentialsGrant(clientId, clientSecret, scope);

        String accessToken = jwtTokenService.generateAccessToken(
                null, null, result.getClientId(), result.getScope(), null, result.getAccessTokenTtl());

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("access_token", accessToken);
        data.put("token_type", "Bearer");
        data.put("expires_in", result.getAccessTokenTtl());
        data.put("scope", result.getScope());
        return data;
    }

    /**
     * 处理刷新令牌授权。
     * <p>
     * SEC-CIAM-DSN-CR-003 §9.4：共享轮换方法的事务边界与异常映射保持一致，不扩展 OAuth 功能。
     * 重放路径需提交撤销结果，因此重放异常不触发回滚。
     */
    @Transactional(rollbackFor = Exception.class, noRollbackFor = RefreshTokenReplayException.class)
    public Map<String, Object> handleRefreshTokenGrant(String refreshToken, String clientId) {
        RefreshToken current = refreshTokenDomainService.loadForRotation(refreshToken, clientId);
        RefreshTokenRotationResult rotationResult = refreshTokenDomainService.rotate(current, null);

        int accessTokenTtl = 1800;
        String accessToken = jwtTokenService.generateAccessToken(AccessTokenContext.builder()
                .userId(rotationResult.getUserId())
                .deviceId(rotationResult.getDeviceId())
                .clientId(rotationResult.getClientId())
                .clientType(rotationResult.getClientType())
                .scope(rotationResult.getScope())
                .sessionId(rotationResult.getSessionId())
                .ttlSeconds(accessTokenTtl)
                .expireBound(rotationResult.getExpireTime())
                .build());

        return buildTokenResponse(accessToken, rotationResult.getNewRefreshToken(), accessTokenTtl, rotationResult.getScope());
    }

    /** 处理设备码授权 */
    public Map<String, Object> handleDeviceCodeGrant(String deviceCode, String clientId) {
        DeviceAuthorizationResult result = deviceAuthorizationService.pollDeviceAuthorization(deviceCode, clientId);

        int accessTokenTtl = 1800;
        String accessToken = jwtTokenService.generateAccessToken(
                result.getUserId(), null, result.getClientId(), result.getScope(), null, accessTokenTtl);

        String newRefreshToken = refreshTokenDomainService.issueRefreshToken(
                result.getUserId(), null, result.getClientId(), 2592000);

        return buildTokenResponse(accessToken, newRefreshToken, accessTokenTtl, result.getScope());
    }

    /** 发起设备授权 */
    public DeviceAuthorizationResponse initiateDeviceAuthorization(String clientId, String scope) {
        return deviceAuthorizationService.initiateDeviceAuthorization(clientId, scope);
    }

    /** 用户确认设备授权 */
    public void approveDeviceAuthorization(String userCode, String userId) {
        deviceAuthorizationService.approveDeviceAuthorization(userCode, userId);
    }

    /** 令牌撤销 */
    public void revokeToken(String token) {
        refreshTokenDomainService.revokeRefreshToken(token);
    }

    /** 获取 OIDC UserInfo */
    public OidcUserInfo getUserInfo(String userId) {
        return oidcService.getUserInfo(userId);
    }

    /** 获取 OIDC Discovery Document */
    public OidcDiscoveryDocument getDiscoveryDocument() {
        return oidcService.getDiscoveryDocument();
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
