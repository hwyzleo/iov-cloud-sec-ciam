package net.hwyz.iov.cloud.sec.ciam.domain.service;

import lombok.RequiredArgsConstructor;
import net.hwyz.iov.cloud.framework.common.exception.BusinessException;
import net.hwyz.iov.cloud.sec.ciam.common.exception.CiamErrorCode;
import net.hwyz.iov.cloud.sec.ciam.common.security.PasswordEncoder;
import net.hwyz.iov.cloud.sec.ciam.common.security.TokenDigest;
import net.hwyz.iov.cloud.framework.common.util.DateTimeUtil;
import net.hwyz.iov.cloud.sec.ciam.domain.repository.CiamAuthCodeRepository;
import net.hwyz.iov.cloud.sec.ciam.domain.repository.CiamOAuthClientRepository;
import net.hwyz.iov.cloud.sec.ciam.infrastructure.repository.dao.dataobject.CiamAuthCodeDo;
import net.hwyz.iov.cloud.sec.ciam.infrastructure.repository.dao.dataobject.CiamOAuthClientDo;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.Base64;
import java.util.UUID;

/**
 * OAuth 授权码领域服务 — 封装 Authorization Code + PKCE 授权流程。
 * <p>
 * 负责授权码签发与交换，包括 PKCE S256 校验、授权码过期与重放防护。
 * 授权码以 SHA-256 指纹存储，原始值仅在签发时返回一次。
 */
@Service
@RequiredArgsConstructor
public class OAuthAuthorizationService {

    /** 授权码默认有效期：5 分钟 */
    static final int CODE_TTL_MINUTES = 5;

    private static final SecureRandom SECURE_RANDOM = new SecureRandom();
    private static final int CODE_BYTE_LENGTH = 32;

    private final CiamAuthCodeRepository authCodeRepository;
    private final CiamOAuthClientRepository clientRepository;
    private final PasswordEncoder passwordEncoder;

    /**
     * 签发授权码。
     *
     * @param clientId        客户端标识
     * @param userId          用户业务唯一标识
     * @param sessionId       会话业务唯一标识
     * @param redirectUri     回调地址
     * @param scope           授权范围
     * @param codeChallenge   PKCE code_challenge（可选，取决于客户端配置）
     * @param challengeMethod PKCE challenge method（当前仅支持 S256）
     * @return 原始授权码（仅此一次）
     */
    public String createAuthorizationCode(String clientId,
                                          String userId,
                                          String sessionId,
                                          String redirectUri,
                                          String scope,
                                          String codeChallenge,
                                          String challengeMethod) {
        // 校验客户端存在且启用
        CiamOAuthClientDo client = findEnabledClient(clientId);

        // 校验回调地址已注册
        if (!isRedirectUriRegistered(client, redirectUri)) {
            throw new BusinessException(CiamErrorCode.REDIRECT_URI_INVALID);
        }

        // 若客户端要求 PKCE，校验 codeChallenge 已提供
        if (client.getPkceRequired() != null && client.getPkceRequired() == 1) {
            if (codeChallenge == null || codeChallenge.isBlank()) {
                throw new BusinessException(CiamErrorCode.PKCE_CHALLENGE_REQUIRED);
            }
        }

        // 生成随机授权码
        String rawCode = generateCode();
        String codeHash = TokenDigest.fingerprint(rawCode);

        Instant now = DateTimeUtil.getNowInstant();
        CiamAuthCodeDo entity = new CiamAuthCodeDo();
        entity.setAuthCodeId(UUID.randomUUID().toString());
        entity.setClientId(clientId);
        entity.setUserId(userId);
        entity.setSessionId(sessionId);
        entity.setCodeHash(codeHash);
        entity.setRedirectUri(redirectUri);
        entity.setScope(scope);
        entity.setCodeChallenge(codeChallenge);
        entity.setChallengeMethod(challengeMethod);
        entity.setExpireTime(now.plusSeconds(CODE_TTL_MINUTES * 60L));
        entity.setUsedFlag(0);
        entity.setRowVersion(1);
        entity.setRowValid(1);
        entity.setCreateTime(now);
        entity.setModifyTime(now);

        authCodeRepository.insert(entity);
        return rawCode;
    }

    /**
     * 交换授权码。
     *
     * @param code         原始授权码
     * @param clientId     客户端标识
     * @param clientSecret 客户端密钥（机密客户端必填）
     * @param redirectUri  回调地址
     * @param codeVerifier PKCE code_verifier（PKCE 场景必填）
     * @return 交换结果，包含 userId、sessionId、scope、clientId
     */
    public AuthCodeExchangeResult exchangeCode(String code,
                                               String clientId,
                                               String clientSecret,
                                               String redirectUri,
                                               String codeVerifier) {
        // 根据 code hash 查找授权码记录
        String codeHash = TokenDigest.fingerprint(code);
        CiamAuthCodeDo authCode = authCodeRepository.findByCodeHash(codeHash)
                .orElseThrow(() -> new BusinessException(CiamErrorCode.AUTH_CODE_INVALID));

        // 校验未过期
        if (authCode.getExpireTime().isBefore(DateTimeUtil.getNowInstant())) {
            throw new BusinessException(CiamErrorCode.AUTH_CODE_EXPIRED);
        }

        // 校验未使用
        if (authCode.getUsedFlag() != null && authCode.getUsedFlag() == 1) {
            throw new BusinessException(CiamErrorCode.AUTH_CODE_USED);
        }

        // 校验 client_id 匹配
        if (!clientId.equals(authCode.getClientId())) {
            throw new BusinessException(CiamErrorCode.AUTH_CODE_CLIENT_MISMATCH);
        }

        // 校验 redirect_uri 匹配
        if (!redirectUri.equals(authCode.getRedirectUri())) {
            throw new BusinessException(CiamErrorCode.AUTH_CODE_REDIRECT_MISMATCH);
        }

        // PKCE 校验
        if (authCode.getCodeChallenge() != null && !authCode.getCodeChallenge().isBlank()) {
            if (codeVerifier == null || codeVerifier.isBlank()) {
                throw new BusinessException(CiamErrorCode.PKCE_VERIFICATION_FAILED);
            }
            if (!verifyPkceS256(codeVerifier, authCode.getCodeChallenge())) {
                throw new BusinessException(CiamErrorCode.PKCE_VERIFICATION_FAILED);
            }
        }

        // 机密客户端校验 client_secret
        CiamOAuthClientDo client = findEnabledClient(clientId);
        if (client.getClientSecretHash() != null && !client.getClientSecretHash().isBlank()) {
            if (clientSecret == null || !passwordEncoder.matches(clientSecret, client.getClientSecretHash())) {
                throw new BusinessException(CiamErrorCode.CLIENT_SECRET_INVALID);
            }
        }

        // 标记授权码已使用
        markCodeAsUsed(authCode);

        return new AuthCodeExchangeResult(
                authCode.getUserId(),
                authCode.getSessionId(),
                authCode.getScope(),
                authCode.getClientId());
    }

    /**
     * Client Credentials 授权。
     * <p>
     * 面向内部服务调用场景，校验客户端凭据后返回授权结果。
     * 仅 confidential 和 internal 类型客户端允许使用此授权方式。
     *
     * @param clientId     客户端标识
     * @param clientSecret 客户端密钥
     * @param scope        请求的授权范围（逗号分隔）
     * @return 授权结果，包含 clientId、scope、accessTokenTtl
     */
    public ClientCredentialsResult clientCredentialsGrant(String clientId,
                                                          String clientSecret,
                                                          String scope) {
        // 校验客户端存在且启用
        CiamOAuthClientDo client = findEnabledClient(clientId);

        // 仅 confidential / internal 客户端允许使用 client_credentials
        if (client.getClientSecretHash() == null || client.getClientSecretHash().isBlank()) {
            throw new BusinessException(CiamErrorCode.CLIENT_TYPE_NOT_SUPPORTED);
        }

        // 校验 client_secret
        if (clientSecret == null || !passwordEncoder.matches(clientSecret, client.getClientSecretHash())) {
            throw new BusinessException(CiamErrorCode.CLIENT_SECRET_INVALID);
        }

        // 校验请求的 scope 不超出客户端允许范围
        String grantedScope = validateAndResolveScope(scope, client.getScopes());

        return new ClientCredentialsResult(clientId, grantedScope, client.getAccessTokenTtl());
    }

    // ---- 内部方法 ----

    /**
     * 校验请求的 scope 是否在客户端允许范围内。
     * 若请求 scope 为空，则返回客户端全部允许范围。
     */
    private String validateAndResolveScope(String requestedScope, String allowedScopes) {
        if (requestedScope == null || requestedScope.isBlank()) {
            return allowedScopes;
        }
        java.util.Set<String> allowed = java.util.Arrays.stream(
                        (allowedScopes == null ? "" : allowedScopes).split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .collect(java.util.stream.Collectors.toSet());

        for (String s : requestedScope.split(",")) {
            if (!allowed.contains(s.trim())) {
                throw new BusinessException(CiamErrorCode.SCOPE_EXCEEDED);
            }
        }
        return requestedScope;
    }

    private CiamOAuthClientDo findEnabledClient(String clientId) {
        CiamOAuthClientDo client = clientRepository.findByClientId(clientId)
                .orElseThrow(() -> new BusinessException(CiamErrorCode.CLIENT_NOT_FOUND));
        if (client.getClientStatus() == null || client.getClientStatus() != 1) {
            throw new BusinessException(CiamErrorCode.CLIENT_DISABLED);
        }
        return client;
    }

    private boolean isRedirectUriRegistered(CiamOAuthClientDo client, String redirectUri) {
        if (client.getRedirectUris() == null || client.getRedirectUris().isBlank()) {
            return false;
        }
        for (String uri : client.getRedirectUris().split(",")) {
            if (uri.trim().equals(redirectUri)) {
                return true;
            }
        }
        return false;
    }

    private void markCodeAsUsed(CiamAuthCodeDo authCode) {
        authCode.setUsedFlag(1);
        authCode.setUsedTime(DateTimeUtil.getNowInstant());
        authCode.setModifyTime(DateTimeUtil.getNowInstant());
        authCodeRepository.updateByAuthCodeId(authCode);
    }

    /**
     * PKCE S256 校验：BASE64URL(SHA256(code_verifier)) == code_challenge
     */
    static boolean verifyPkceS256(String codeVerifier, String codeChallenge) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(codeVerifier.getBytes(StandardCharsets.US_ASCII));
            String computed = Base64.getUrlEncoder().withoutPadding().encodeToString(hash);
            return computed.equals(codeChallenge);
        } catch (Exception e) {
            return false;
        }
    }

    private String generateCode() {
        byte[] bytes = new byte[CODE_BYTE_LENGTH];
        SECURE_RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }
}
