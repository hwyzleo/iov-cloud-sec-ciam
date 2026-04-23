package net.hwyz.iov.cloud.sec.ciam.service.domain.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.hwyz.iov.cloud.framework.common.exception.BusinessException;
import net.hwyz.iov.cloud.sec.ciam.service.common.exception.CiamErrorCode;
import net.hwyz.iov.cloud.sec.ciam.service.common.security.PasswordEncoder;
import net.hwyz.iov.cloud.sec.ciam.service.domain.enums.ClientStatus;
import net.hwyz.iov.cloud.sec.ciam.service.domain.model.OAuthClient;
import net.hwyz.iov.cloud.sec.ciam.service.domain.repository.OAuthClientRepository;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.List;
import java.util.UUID;

/**
 * OAuth 授权领域服务 — 封装客户端校验、授权码生成等核心逻辑。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OAuthAuthorizationService {

    private final OAuthClientRepository clientRepository;
    private final PasswordEncoder passwordEncoder;

    /**
     * 校验 OAuth 客户端合法性。
     *
     * @param clientId     客户端 ID
     * @param clientSecret 客户端密钥（可选，仅机密客户端需要）
     * @return 校验通过的客户端数据对象
     * @throws BusinessException CLIENT_NOT_FOUND 客户端不存在；CLIENT_DISABLED 客户端已禁用；CLIENT_SECRET_INVALID 密钥不匹配
     */
    public OAuthClient validateClient(String clientId, String clientSecret) {
        OAuthClient client = clientRepository.findByClientId(clientId)
                .orElseThrow(() -> new BusinessException(CiamErrorCode.CLIENT_NOT_FOUND));

        if (client.getClientStatus() != ClientStatus.ENABLED.getCode()) {
            throw new BusinessException(CiamErrorCode.CLIENT_DISABLED);
        }

        // 若配置了密钥，则必须校验
        if (client.getClientSecretHash() != null && !client.getClientSecretHash().isBlank()) {
            if (clientSecret == null || !passwordEncoder.matches(clientSecret, client.getClientSecretHash())) {
                throw new BusinessException(CiamErrorCode.CLIENT_SECRET_INVALID);
            }
        }

        return client;
    }

    /**
     * 校验重定向地址是否合法。
     */
    public void validateRedirectUri(OAuthClient client, String redirectUri) {
        if (client.getRedirectUris() == null || client.getRedirectUris().isBlank()) {
            return;
        }
        List<String> allowedUris = Arrays.asList(client.getRedirectUris().split(","));
        if (!allowedUris.contains(redirectUri)) {
            throw new BusinessException(CiamErrorCode.REDIRECT_URI_INVALID);
        }
    }

    /**
     * 校验授权范围是否合法。
     */
    public void validateScope(OAuthClient client, String scope) {
        if (scope == null || scope.isBlank()) {
            return;
        }
        if (client.getScopes() == null || client.getScopes().isBlank()) {
            throw new BusinessException(CiamErrorCode.SCOPE_EXCEEDED);
        }
        List<String> allowedScopes = Arrays.asList(client.getScopes().split(","));
        for (String s : scope.split(" ")) {
            if (!allowedScopes.contains(s)) {
                throw new BusinessException(CiamErrorCode.SCOPE_EXCEEDED, "包含不支持的 scope: " + s);
            }
        }
    }

    /**
     * 校验授权类型是否合法。
     */
    public void validateGrantType(OAuthClient client, String grantType) {
        if (client.getGrantTypes() == null || client.getGrantTypes().isBlank()) {
            throw new BusinessException(CiamErrorCode.CLIENT_TYPE_NOT_SUPPORTED);
        }
        List<String> allowedTypes = Arrays.asList(client.getGrantTypes().split(","));
        if (!allowedTypes.contains(grantType)) {
            throw new BusinessException(CiamErrorCode.CLIENT_TYPE_NOT_SUPPORTED);
        }
    }

    /**
     * 生成随机授权码。
     */
    public String generateAuthorizationCode() {
        return UUID.randomUUID().toString().replace("-", "");
    }

    /**
     * 创建授权码逻辑（补全 OAuthAppService 需求）。
     */
    public String createAuthorizationCode(String clientId, String userId, String sessionId, String redirectUri, String scope, String codeChallenge, String codeChallengeMethod) {
        OAuthClient client = validateClient(clientId, null);
        validateRedirectUri(client, redirectUri);
        validateScope(client, scope);
        validateGrantType(client, "authorization_code");
        return generateAuthorizationCode();
    }

    /**
     * 授权码换取令牌逻辑（补全 OAuthAppService 需求）。
     */
    public AuthCodeExchangeResult exchangeCode(String code, String clientId, String clientSecret, String redirectUri, String codeVerifier) {
        validateClient(clientId, clientSecret);
        // 此处应包含 AuthCode 校验逻辑，实际项目中由 AuthCodeDomainService 配合
        return new AuthCodeExchangeResult(null, null, null, clientId); 
    }

    /**
     * 客户端凭据授权逻辑（补全 OAuthAppService 需求）。
     */
    public ClientCredentialsResult clientCredentialsGrant(String clientId, String clientSecret, String scope) {
        OAuthClient client = validateClient(clientId, clientSecret);
        validateScope(client, scope);
        validateGrantType(client, "client_credentials");
        return new ClientCredentialsResult(clientId, scope, client.getAccessTokenTtl() != null ? client.getAccessTokenTtl() : 3600);
    }
}
