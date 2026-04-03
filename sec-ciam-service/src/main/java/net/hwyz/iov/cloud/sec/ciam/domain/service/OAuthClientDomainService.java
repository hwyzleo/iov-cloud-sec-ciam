package net.hwyz.iov.cloud.sec.ciam.domain.service;

import lombok.RequiredArgsConstructor;
import net.hwyz.iov.cloud.framework.common.exception.BusinessException;
import net.hwyz.iov.cloud.sec.ciam.common.exception.CiamErrorCode;
import net.hwyz.iov.cloud.sec.ciam.common.security.PasswordEncoder;
import net.hwyz.iov.cloud.sec.ciam.common.util.DateTimeUtil;
import net.hwyz.iov.cloud.sec.ciam.domain.enums.ClientStatus;
import net.hwyz.iov.cloud.sec.ciam.domain.enums.OAuthClientType;
import net.hwyz.iov.cloud.sec.ciam.domain.repository.CiamOAuthClientRepository;
import net.hwyz.iov.cloud.sec.ciam.infrastructure.repository.dao.dataobject.CiamOAuthClientDo;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.util.Arrays;
import java.util.Optional;
import java.util.UUID;

/**
 * OAuth 客户端领域服务 — 封装客户端注册、查询、校验、更新与状态管理逻辑。
 * <p>
 * 支持 public、confidential、internal 三种客户端类型。
 * 机密/内部客户端注册时生成随机密钥并以 BCrypt 哈希存储，原始密钥仅在注册时返回一次。
 */
@Service
@RequiredArgsConstructor
public class OAuthClientDomainService {

    private static final SecureRandom SECURE_RANDOM = new SecureRandom();
    private static final int SECRET_LENGTH = 32;

    private final CiamOAuthClientRepository clientRepository;
    private final PasswordEncoder passwordEncoder;

    /**
     * 注册新的 OAuth 客户端。
     * <p>
     * 生成唯一 client_id（UUID 格式）。对于 confidential/internal 类型，
     * 生成随机 client_secret 并以 BCrypt 哈希存储；public 类型不生成密钥。
     *
     * @param clientName      客户端名称
     * @param clientType      客户端类型
     * @param redirectUris    回调地址（逗号分隔）
     * @param grantTypes      授权类型（逗号分隔）
     * @param scopes          授权范围（逗号分隔）
     * @param pkceRequired    是否强制 PKCE
     * @param accessTokenTtl  Access Token 有效期（秒）
     * @param refreshTokenTtl Refresh Token 有效期（秒）
     * @return 注册结果，包含 clientId 和原始 clientSecret（仅此一次）
     */
    public ClientRegistrationResult registerClient(String clientName,
                                                   OAuthClientType clientType,
                                                   String redirectUris,
                                                   String grantTypes,
                                                   String scopes,
                                                   boolean pkceRequired,
                                                   int accessTokenTtl,
                                                   int refreshTokenTtl) {
        String clientId = UUID.randomUUID().toString();
        String rawSecret = null;
        String secretHash = null;

        if (clientType != OAuthClientType.PUBLIC) {
            rawSecret = generateSecret();
            secretHash = passwordEncoder.encode(rawSecret);
        }

        CiamOAuthClientDo entity = new CiamOAuthClientDo();
        entity.setClientId(clientId);
        entity.setClientName(clientName);
        entity.setClientSecretHash(secretHash);
        entity.setClientType(clientType.getValue());
        entity.setRedirectUris(redirectUris);
        entity.setGrantTypes(grantTypes);
        entity.setScopes(scopes);
        entity.setPkceRequired(pkceRequired ? 1 : 0);
        entity.setAccessTokenTtl(accessTokenTtl);
        entity.setRefreshTokenTtl(refreshTokenTtl);
        entity.setClientStatus(ClientStatus.ENABLED.getCode());
        entity.setRowVersion(1);
        entity.setRowValid(1);
        entity.setCreateTime(DateTimeUtil.now());
        entity.setModifyTime(DateTimeUtil.now());
        clientRepository.insert(entity);

        return new ClientRegistrationResult(clientId, rawSecret, clientName);
    }

    /**
     * 根据客户端标识查询客户端。
     *
     * @param clientId 客户端标识
     * @return 客户端记录（如存在）
     */
    public Optional<CiamOAuthClientDo> findByClientId(String clientId) {
        return clientRepository.findByClientId(clientId);
    }

    /**
     * 校验客户端凭据（适用于 confidential/internal 客户端）。
     *
     * @param clientId     客户端标识
     * @param clientSecret 客户端密钥
     * @return 校验通过返回 true
     * @throws BusinessException 客户端不存在时抛出 CLIENT_NOT_FOUND；客户端已停用时抛出 CLIENT_DISABLED
     */
    public boolean validateClient(String clientId, String clientSecret) {
        CiamOAuthClientDo client = clientRepository.findByClientId(clientId)
                .orElseThrow(() -> new BusinessException(CiamErrorCode.CLIENT_NOT_FOUND));

        if (client.getClientStatus() != ClientStatus.ENABLED.getCode()) {
            throw new BusinessException(CiamErrorCode.CLIENT_DISABLED);
        }

        if (client.getClientSecretHash() == null) {
            // public 客户端无密钥，无法通过密钥校验
            return false;
        }

        return passwordEncoder.matches(clientSecret, client.getClientSecretHash());
    }

    /**
     * 校验回调地址是否已注册。
     *
     * @param clientId    客户端标识
     * @param redirectUri 待校验的回调地址
     * @return 已注册返回 true
     * @throws BusinessException 客户端不存在时抛出 CLIENT_NOT_FOUND
     */
    public boolean validateRedirectUri(String clientId, String redirectUri) {
        CiamOAuthClientDo client = clientRepository.findByClientId(clientId)
                .orElseThrow(() -> new BusinessException(CiamErrorCode.CLIENT_NOT_FOUND));

        if (client.getRedirectUris() == null || client.getRedirectUris().isBlank()) {
            return false;
        }

        return Arrays.stream(client.getRedirectUris().split(","))
                .map(String::trim)
                .anyMatch(uri -> uri.equals(redirectUri));
    }

    /**
     * 更新客户端配置。
     *
     * @param clientId        客户端标识
     * @param clientName      客户端名称（null 则不更新）
     * @param redirectUris    回调地址（null 则不更新）
     * @param grantTypes      授权类型（null 则不更新）
     * @param scopes          授权范围（null 则不更新）
     * @param pkceRequired    是否强制 PKCE（null 则不更新）
     * @param accessTokenTtl  Access Token 有效期（null 则不更新）
     * @param refreshTokenTtl Refresh Token 有效期（null 则不更新）
     * @throws BusinessException 客户端不存在时抛出 CLIENT_NOT_FOUND
     */
    public void updateClient(String clientId,
                             String clientName,
                             String redirectUris,
                             String grantTypes,
                             String scopes,
                             Boolean pkceRequired,
                             Integer accessTokenTtl,
                             Integer refreshTokenTtl) {
        CiamOAuthClientDo client = clientRepository.findByClientId(clientId)
                .orElseThrow(() -> new BusinessException(CiamErrorCode.CLIENT_NOT_FOUND));

        if (clientName != null) {
            client.setClientName(clientName);
        }
        if (redirectUris != null) {
            client.setRedirectUris(redirectUris);
        }
        if (grantTypes != null) {
            client.setGrantTypes(grantTypes);
        }
        if (scopes != null) {
            client.setScopes(scopes);
        }
        if (pkceRequired != null) {
            client.setPkceRequired(pkceRequired ? 1 : 0);
        }
        if (accessTokenTtl != null) {
            client.setAccessTokenTtl(accessTokenTtl);
        }
        if (refreshTokenTtl != null) {
            client.setRefreshTokenTtl(refreshTokenTtl);
        }
        client.setModifyTime(DateTimeUtil.now());
        clientRepository.updateByClientId(client);
    }

    /**
     * 停用客户端。
     *
     * @param clientId 客户端标识
     * @throws BusinessException 客户端不存在时抛出 CLIENT_NOT_FOUND
     */
    public void disableClient(String clientId) {
        CiamOAuthClientDo client = clientRepository.findByClientId(clientId)
                .orElseThrow(() -> new BusinessException(CiamErrorCode.CLIENT_NOT_FOUND));

        client.setClientStatus(ClientStatus.DISABLED.getCode());
        client.setModifyTime(DateTimeUtil.now());
        clientRepository.updateByClientId(client);
    }

    /**
     * 启用客户端。
     *
     * @param clientId 客户端标识
     * @throws BusinessException 客户端不存在时抛出 CLIENT_NOT_FOUND
     */
    public void enableClient(String clientId) {
        CiamOAuthClientDo client = clientRepository.findByClientId(clientId)
                .orElseThrow(() -> new BusinessException(CiamErrorCode.CLIENT_NOT_FOUND));

        client.setClientStatus(ClientStatus.ENABLED.getCode());
        client.setModifyTime(DateTimeUtil.now());
        clientRepository.updateByClientId(client);
    }

    // ---- 内部方法 ----

    private String generateSecret() {
        byte[] bytes = new byte[SECRET_LENGTH];
        SECURE_RANDOM.nextBytes(bytes);
        StringBuilder sb = new StringBuilder(SECRET_LENGTH * 2);
        for (byte b : bytes) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }
}
