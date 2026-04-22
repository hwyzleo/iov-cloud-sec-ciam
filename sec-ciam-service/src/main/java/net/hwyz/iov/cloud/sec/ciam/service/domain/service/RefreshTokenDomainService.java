package net.hwyz.iov.cloud.sec.ciam.service.domain.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.hwyz.iov.cloud.framework.common.exception.BusinessException;
import net.hwyz.iov.cloud.sec.ciam.service.common.exception.CiamErrorCode;
import net.hwyz.iov.cloud.sec.ciam.service.common.security.TokenDigest;
import net.hwyz.iov.cloud.framework.common.util.DateTimeUtil;
import net.hwyz.iov.cloud.sec.ciam.service.domain.enums.TokenStatus;
import net.hwyz.iov.cloud.sec.ciam.service.domain.repository.CiamRefreshTokenRepository;
import net.hwyz.iov.cloud.sec.ciam.service.infrastructure.persistence.po.RefreshTokenPo;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.time.Instant;
import java.util.Base64;
import java.util.UUID;

/**
 * Refresh Token 领域服务 — 签发、轮换（Rotation）与撤销。
 * <p>
 * 对应 design.md 模块 6（会话与设备管理模块）中的 Refresh Token 服务端存储与 Rotation 机制。
 * <ul>
 *   <li>Refresh Token 持久化至 MySQL，热点状态存 Redis（由仓储层透明处理）</li>
 *   <li>旧令牌一经使用立即标记为 ROTATED，防止重放攻击</li>
 *   <li>新令牌通过 parent_token_id 形成轮换链路</li>
 * </ul>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RefreshTokenDomainService {

    private static final int RAW_TOKEN_BYTE_LENGTH = 32;
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    private final CiamRefreshTokenRepository refreshTokenRepository;

    /**
     * 签发新的 Refresh Token。
     *
     * @param userId     用户业务唯一标识
     * @param sessionId  会话业务唯一标识
     * @param clientId   OAuth 客户端标识
     * @param ttlSeconds 令牌有效期（秒）
     * @return 原始 Refresh Token 字符串（仅此次返回，不落库）
     */
    public String issueRefreshToken(String userId,
                                    String sessionId,
                                    String clientId,
                                    int ttlSeconds) {
        String rawToken = generateRawToken();
        String fingerprint = TokenDigest.fingerprint(rawToken);
        Instant now = DateTimeUtil.getNowInstant();

        RefreshTokenPo entity = new RefreshTokenPo();
        entity.setRefreshTokenId(UUID.randomUUID().toString());
        entity.setUserId(userId);
        entity.setSessionId(sessionId);
        entity.setClientId(clientId);
        entity.setTokenFingerprint(fingerprint);
        entity.setTokenStatus(TokenStatus.ACTIVE.getCode());
        entity.setIssueTime(now);
        entity.setExpireTime(now.plusSeconds(ttlSeconds));
        entity.setCreateTime(now);
        entity.setModifyTime(now);
        entity.setRowValid(1);
        entity.setRowVersion(1);

        refreshTokenRepository.insert(entity);
        log.info("签发 Refresh Token: userId={}, sessionId={}, clientId={}, refreshTokenId={}",
                userId, sessionId, clientId, entity.getRefreshTokenId());

        return rawToken;
    }

    /**
     * 轮换 Refresh Token（Rotation）。
     * <p>
     * 流程：
     * <ol>
     *   <li>根据原始令牌计算指纹，查找数据库记录</li>
     *   <li>校验令牌状态为 ACTIVE、未过期、客户端匹配</li>
     *   <li>将旧令牌标记为 ROTATED，记录 used_time</li>
     *   <li>签发新令牌，parent_token_id 指向旧令牌</li>
     * </ol>
     *
     * @param rawToken 当前持有的原始 Refresh Token
     * @param clientId OAuth 客户端标识
     * @return 轮换结果，包含新令牌及关联信息
     * @throws BusinessException TOKEN_INVALID 令牌不存在、已轮换/撤销/过期、客户端不匹配
     */
    public RefreshTokenRotationResult rotateRefreshToken(String rawToken, String clientId) {
        String fingerprint = TokenDigest.fingerprint(rawToken);
        RefreshTokenPo existing = refreshTokenRepository.findByTokenFingerprint(fingerprint)
                .orElseThrow(() -> new BusinessException(CiamErrorCode.TOKEN_INVALID));

        // 重放检测：已轮换的令牌被再次使用
        TokenStatus status = TokenStatus.fromCode(existing.getTokenStatus());
        if (status == TokenStatus.ROTATED) {
            log.warn("检测到 Refresh Token 重放攻击: refreshTokenId={}, userId={}",
                    existing.getRefreshTokenId(), existing.getUserId());
            // 安全策略：撤销该用户在此会话下的所有令牌
            refreshTokenRepository.revokeAllBySessionId(existing.getSessionId());
            throw new BusinessException(CiamErrorCode.TOKEN_INVALID, "令牌已被使用，疑似重放攻击");
        }

        if (status != TokenStatus.ACTIVE) {
            throw new BusinessException(CiamErrorCode.TOKEN_INVALID);
        }

        // 过期校验
        if (existing.getExpireTime().isBefore(DateTimeUtil.getNowInstant())) {
            throw new BusinessException(CiamErrorCode.TOKEN_INVALID, "令牌已过期");
        }

        // 客户端匹配校验
        if (existing.getClientId() != null && !existing.getClientId().equals(clientId)) {
            throw new BusinessException(CiamErrorCode.TOKEN_INVALID, "客户端不匹配");
        }

        // 标记旧令牌为已轮换
        Instant now = DateTimeUtil.getNowInstant();
        existing.setTokenStatus(TokenStatus.ROTATED.getCode());
        existing.setUsedTime(now);
        existing.setModifyTime(now);
        refreshTokenRepository.updateByRefreshTokenId(existing);

        // 计算新令牌 TTL：继承旧令牌的剩余有效期
        long remainingSeconds = java.time.Duration.between(now, existing.getExpireTime()).getSeconds();
        // 使用原始 TTL（从签发到过期的完整时长）
        long originalTtl = java.time.Duration.between(existing.getIssueTime(), existing.getExpireTime()).getSeconds();

        // 签发新令牌
        String newRawToken = generateRawToken();
        String newFingerprint = TokenDigest.fingerprint(newRawToken);

        RefreshTokenPo newEntity = new RefreshTokenPo();
        newEntity.setRefreshTokenId(UUID.randomUUID().toString());
        newEntity.setUserId(existing.getUserId());
        newEntity.setSessionId(existing.getSessionId());
        newEntity.setClientId(existing.getClientId());
        newEntity.setTokenFingerprint(newFingerprint);
        newEntity.setParentTokenId(existing.getRefreshTokenId());
        newEntity.setTokenStatus(TokenStatus.ACTIVE.getCode());
        newEntity.setIssueTime(now);
        newEntity.setExpireTime(now.plusSeconds(originalTtl));
        newEntity.setCreateTime(now);
        newEntity.setModifyTime(now);
        newEntity.setRowValid(1);
        newEntity.setRowVersion(1);

        refreshTokenRepository.insert(newEntity);

        log.info("Refresh Token 轮换完成: oldTokenId={}, newTokenId={}, userId={}",
                existing.getRefreshTokenId(), newEntity.getRefreshTokenId(), existing.getUserId());

        return new RefreshTokenRotationResult(
                newRawToken,
                existing.getUserId(),
                existing.getSessionId(),
                null  // scope 由上层根据 clientId 查询填充
        );
    }

    /**
     * 撤销指定的 Refresh Token。
     *
     * @param rawToken 原始 Refresh Token
     * @throws BusinessException TOKEN_INVALID 令牌不存在
     */
    public void revokeRefreshToken(String rawToken) {
        String fingerprint = TokenDigest.fingerprint(rawToken);
        RefreshTokenPo existing = refreshTokenRepository.findByTokenFingerprint(fingerprint)
                .orElseThrow(() -> new BusinessException(CiamErrorCode.TOKEN_INVALID));

        TokenStatus status = TokenStatus.fromCode(existing.getTokenStatus());
        if (status == TokenStatus.REVOKED) {
            log.info("令牌已处于撤销状态，跳过: refreshTokenId={}", existing.getRefreshTokenId());
            return;
        }

        Instant now = DateTimeUtil.getNowInstant();
        existing.setTokenStatus(TokenStatus.REVOKED.getCode());
        existing.setRevokeTime(now);
        existing.setModifyTime(now);
        refreshTokenRepository.updateByRefreshTokenId(existing);

        log.info("Refresh Token 已撤销: refreshTokenId={}, userId={}",
                existing.getRefreshTokenId(), existing.getUserId());
    }

    // ---- 内部方法 ----

    /**
     * 生成密码学安全的随机令牌字符串。
     */
    private String generateRawToken() {
        byte[] bytes = new byte[RAW_TOKEN_BYTE_LENGTH];
        SECURE_RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }
}
