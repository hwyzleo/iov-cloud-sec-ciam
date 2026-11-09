package net.hwyz.iov.cloud.sec.ciam.service.domain.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.hwyz.iov.cloud.framework.common.exception.BusinessException;
import net.hwyz.iov.cloud.sec.ciam.service.common.exception.CiamErrorCode;
import net.hwyz.iov.cloud.sec.ciam.service.common.security.TokenDigest;
import net.hwyz.iov.cloud.framework.common.util.DateTimeUtil;
import net.hwyz.iov.cloud.sec.ciam.service.domain.enums.TokenStatus;
import net.hwyz.iov.cloud.sec.ciam.service.domain.model.RefreshToken;
import net.hwyz.iov.cloud.sec.ciam.service.domain.model.Session;
import net.hwyz.iov.cloud.sec.ciam.service.domain.repository.RefreshTokenRepository;
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

    private final RefreshTokenRepository refreshTokenRepository;

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
        return issueRefreshToken(userId, sessionId, clientId,
                DateTimeUtil.getNowInstant().plusSeconds(ttlSeconds));
    }

    /**
     * 签发 Refresh Token（指定绝对到期时间）。
     * <p>
     * 手机登录链路使用本入口，使 {@code refreshToken.expireTime = session.expireTime}
     * （SEC-CIAM-DSN-CR-003 RD-003-7，§4.4），避免令牌与会话到期边界不一致。
     *
     * @param userId     用户业务唯一标识
     * @param sessionId  会话业务唯一标识
     * @param clientId   OAuth 客户端标识
     * @param expireTime 绝对到期时间
     * @return 原始 Refresh Token 字符串（仅此次返回，不落库）
     */
    public String issueRefreshToken(String userId,
                                    String sessionId,
                                    String clientId,
                                    Instant expireTime) {
        String rawToken = generateRawToken();
        String fingerprint = TokenDigest.fingerprint(rawToken);
        Instant now = DateTimeUtil.getNowInstant();

        RefreshToken domain = RefreshToken.builder()
                .refreshTokenId(UUID.randomUUID().toString())
                .userId(userId)
                .sessionId(sessionId)
                .clientId(clientId)
                .tokenFingerprint(fingerprint)
                .tokenStatus(TokenStatus.ACTIVE.getCode())
                .issueTime(now)
                .expireTime(expireTime)
                .build();

        refreshTokenRepository.insert(domain);
        log.info("签发 Refresh Token: userId={}, sessionId={}, clientId={}, refreshTokenId={}",
                userId, sessionId, clientId, domain.getRefreshTokenId());

        return rawToken;
    }

    /**
     * 加载并校验待轮换的 Refresh Token（SEC-CIAM-DSN-CR-003 §5.2）。
     * <p>
     * 仅做只读校验；重放（ROTATED）路径会先撤销会话活跃令牌链，再抛出
     * {@link RefreshTokenReplayException}，以便在 {@code noRollbackFor} 事务中提交撤销结果。
     *
     * @param rawToken 当前持有的原始 Refresh Token
     * @param clientId OAuth 客户端标识
     * @return 处于 ACTIVE 且未过期、客户端匹配的令牌记录
     * @throws RefreshTokenReplayException 已轮换令牌被重放（撤销链已提交）
     * @throws BusinessException          TOKEN_INVALID 令牌不存在、状态非法、已过期或客户端不匹配
     */
    public RefreshToken loadForRotation(String rawToken, String clientId) {
        String fingerprint = TokenDigest.fingerprint(rawToken);
        RefreshToken existing = refreshTokenRepository.findByTokenFingerprint(fingerprint)
                .orElseThrow(() -> new BusinessException(CiamErrorCode.TOKEN_INVALID));

        TokenStatus status = TokenStatus.fromCode(existing.getTokenStatus());
        if (status == TokenStatus.ROTATED) {
            log.warn("检测到 Refresh Token 重放攻击: refreshTokenId={}, userId={}",
                    existing.getRefreshTokenId(), existing.getUserId());
            // 安全策略：撤销该会话下所有活跃令牌（防重放），撤销需随专用异常一起提交
            refreshTokenRepository.revokeAllBySessionId(existing.getSessionId());
            throw new RefreshTokenReplayException("令牌已被使用，疑似重放攻击");
        }

        if (status != TokenStatus.ACTIVE) {
            throw new BusinessException(CiamErrorCode.TOKEN_INVALID);
        }

        if (existing.getExpireTime() == null
                || !DateTimeUtil.getNowInstant().isBefore(existing.getExpireTime())) {
            throw new BusinessException(CiamErrorCode.TOKEN_INVALID, "令牌已过期");
        }

        if (existing.getClientId() != null && !existing.getClientId().equals(clientId)) {
            throw new BusinessException(CiamErrorCode.TOKEN_INVALID, "客户端不匹配");
        }

        return existing;
    }

    /**
     * 执行单成功者轮换（CAS）并签发子令牌（SEC-CIAM-DSN-CR-003 RD-003-6/7，§5.2）。
     * <p>
     * 约束：
     * <ul>
     *   <li>旧令牌通过条件更新 {@code ACTIVE -> ROTATED} 完成，只有 affected rows = 1 才算成功；</li>
     *   <li>CAS 失败按重放策略撤销会话令牌链并抛出 {@link RefreshTokenReplayException}；</li>
     *   <li>子令牌继承父令牌 {@code expire_time}，不重新计算完整 TTL；</li>
     *   <li>上下文从持久会话恢复（会话为 null 时退化为令牌记录，仅用于范围外 OAuth 流程）。</li>
     * </ul>
     *
     * @param current 已通过 {@link #loadForRotation} 校验的令牌记录
     * @param session 关联的持久会话（手机链路必填；范围外流程可为 null）
     * @return 轮换结果（新令牌 + 完整上下文 + 绝对到期边界）
     * @throws RefreshTokenReplayException 并发竞争失败（撤销链已提交）
     */
    public RefreshTokenRotationResult rotate(RefreshToken current, Session session) {
        Instant now = DateTimeUtil.getNowInstant();

        int affected = refreshTokenRepository.compareAndSetActiveToRotated(
                current.getRefreshTokenId(), now);
        if (affected != 1) {
            log.warn("Refresh Token 轮换竞争失败（CAS=0）: refreshTokenId={}, sessionId={}",
                    current.getRefreshTokenId(), current.getSessionId());
            // 并发竞争中失败方按重放处理：撤销会话活跃令牌链并拒绝
            refreshTokenRepository.revokeAllBySessionId(current.getSessionId());
            throw new RefreshTokenReplayException("令牌轮换竞争失败，已撤销会话令牌链");
        }

        String newRawToken = generateRawToken();
        RefreshToken child = RefreshToken.builder()
                .refreshTokenId(UUID.randomUUID().toString())
                .userId(current.getUserId())
                .sessionId(current.getSessionId())
                .clientId(current.getClientId())
                .tokenFingerprint(TokenDigest.fingerprint(newRawToken))
                .parentTokenId(current.getRefreshTokenId())
                .tokenStatus(TokenStatus.ACTIVE.getCode())
                .issueTime(now)
                // 绝对到期边界继承父令牌，不延长（RD-003-7）
                .expireTime(current.getExpireTime())
                .build();
        refreshTokenRepository.insert(child);

        log.info("Refresh Token 轮换完成: oldTokenId={}, newTokenId={}, userId={}, expireTime={}",
                current.getRefreshTokenId(), child.getRefreshTokenId(),
                current.getUserId(), child.getExpireTime());

        String clientType = session != null ? session.getClientType() : null;
        String deviceId = session != null ? session.getDeviceId() : null;
        String scope = session != null ? session.getScope() : null;

        return new RefreshTokenRotationResult(
                newRawToken,
                current.getUserId(),
                current.getSessionId(),
                current.getClientId(),
                clientType,
                deviceId,
                scope,
                current.getExpireTime());
    }

    /**
     * 撤销指定的 Refresh Token。
     *
     * @param rawToken 原始 Refresh Token
     * @throws BusinessException TOKEN_INVALID 令牌不存在
     */
    public void revokeRefreshToken(String rawToken) {
        String fingerprint = TokenDigest.fingerprint(rawToken);
        RefreshToken existing = refreshTokenRepository.findByTokenFingerprint(fingerprint)
                .orElseThrow(() -> new BusinessException(CiamErrorCode.TOKEN_INVALID));

        TokenStatus status = TokenStatus.fromCode(existing.getTokenStatus());
        if (status == TokenStatus.REVOKED) {
            log.info("令牌已处于撤销状态，跳过: refreshTokenId={}", existing.getRefreshTokenId());
            return;
        }

        Instant now = DateTimeUtil.getNowInstant();
        existing.setTokenStatus(TokenStatus.REVOKED.getCode());
        existing.setRevokeTime(now);
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
