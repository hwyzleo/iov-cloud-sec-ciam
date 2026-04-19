package net.hwyz.iov.cloud.sec.ciam.service.application;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.hwyz.iov.cloud.framework.common.exception.BusinessException;
import net.hwyz.iov.cloud.framework.common.util.DateTimeUtil;
import net.hwyz.iov.cloud.sec.ciam.service.common.exception.CiamErrorCode;
import net.hwyz.iov.cloud.sec.ciam.service.domain.enums.TokenStatus;
import net.hwyz.iov.cloud.sec.ciam.service.domain.repository.CiamRefreshTokenRepository;
import net.hwyz.iov.cloud.sec.ciam.service.domain.search.SearchResult;
import net.hwyz.iov.cloud.sec.ciam.service.infrastructure.repository.dao.dataobject.CiamRefreshTokenDo;
import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 运营后台令牌查询应用服务 — 提供令牌信息查询的检索能力。
 * <p>
 * 职责：
 * <ul>
 *   <li>令牌列表检索</li>
 *   <li>令牌详情查询</li>
 *   <li>用户令牌列表查询</li>
 *   <li>会话令牌列表查询</li>
 * </ul>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TokenQueryAppService {

    private final CiamRefreshTokenRepository refreshTokenRepository;

    /**
     * 检索令牌列表。
     *
     * @param refreshTokenId 令牌 ID（精确）
     * @param userId         用户 ID（精确）
     * @param sessionId      会话 ID（精确）
     * @param clientId       客户端 ID（精确）
     * @param tokenStatus    令牌状态（精确）
     * @param startTime      签发开始时间
     * @param endTime        签发结束时间
     * @param page           页码（从 0 开始）
     * @param size           每页大小
     * @return 令牌检索结果
     */
    public SearchResult<TokenSearchResult> queryTokenList(String refreshTokenId,
                                                           String userId,
                                                           String sessionId,
                                                           String clientId,
                                                           Integer tokenStatus,
                                                           OffsetDateTime startTime,
                                                           OffsetDateTime endTime,
                                                           int page,
                                                           int size) {
        log.info("检索令牌列表：refreshTokenId={}, userId={}, sessionId={}, clientId={}, tokenStatus={}, startTime={}, endTime={}",
                refreshTokenId, userId, sessionId, clientId, tokenStatus, startTime, endTime);

        List<CiamRefreshTokenDo> allTokens = refreshTokenRepository.findAll();

        List<TokenSearchResult> filteredList = allTokens.stream()
                .map(this::toTokenSearchResult)
                .filter(doc -> {
                    if (refreshTokenId != null && !refreshTokenId.isEmpty() && !refreshTokenId.equals(doc.refreshTokenId())) {
                        return false;
                    }
                    if (userId != null && !userId.isEmpty() && !userId.equals(doc.userId())) {
                        return false;
                    }
                    if (sessionId != null && !sessionId.isEmpty() && !sessionId.equals(doc.sessionId())) {
                        return false;
                    }
                    if (clientId != null && !clientId.isEmpty() && !clientId.equals(doc.clientId())) {
                        return false;
                    }
                    if (tokenStatus != null && !tokenStatus.equals(doc.tokenStatus())) {
                        return false;
                    }
                    if (startTime != null || endTime != null) {
                        OffsetDateTime issueTime = doc.issueTime();
                        if (issueTime == null) {
                            return false;
                        }
                        if (startTime != null && issueTime.isBefore(startTime)) {
                            return false;
                        }
                        if (endTime != null && issueTime.isAfter(endTime)) {
                            return false;
                        }
                    }
                    return true;
                })
                .collect(Collectors.toList());

        return paginate(filteredList, page, size);
    }

    /**
     * 查询令牌详情。
     *
     * @param refreshTokenId 令牌业务唯一标识
     * @return 令牌详情
     */
    public CiamRefreshTokenDo queryToken(String refreshTokenId) {
        CiamRefreshTokenDo token = refreshTokenRepository.findByRefreshTokenId(refreshTokenId)
                .orElseThrow(() -> new BusinessException(CiamErrorCode.TOKEN_INVALID));
        log.info("查询令牌详情：refreshTokenId={}", refreshTokenId);
        return token;
    }

    /**
     * 查询用户的令牌列表。
     *
     * @param userId 用户业务唯一标识
     * @return 该用户的所有令牌
     */
    public List<CiamRefreshTokenDo> queryUserTokens(String userId) {
        log.info("查询用户令牌列表：userId={}", userId);
        return refreshTokenRepository.findByUserId(userId);
    }

    /**
     * 查询会话的令牌列表。
     *
     * @param sessionId 会话业务唯一标识
     * @return 该会话的所有令牌
     */
    public List<CiamRefreshTokenDo> querySessionTokens(String sessionId) {
        log.info("查询会话令牌列表：sessionId={}", sessionId);
        return refreshTokenRepository.findBySessionId(sessionId);
    }

    // ---- 内部方法 ----

    /**
     * 将 CiamRefreshTokenDo 转换为 TokenSearchResult。
     */
    private TokenSearchResult toTokenSearchResult(CiamRefreshTokenDo token) {
        return new TokenSearchResult(
                token.getRefreshTokenId(),
                token.getUserId(),
                token.getSessionId(),
                token.getClientId(),
                token.getTokenFingerprint(),
                token.getParentTokenId(),
                token.getTokenStatus(),
                DateTimeUtil.instantToOffsetDateTime(token.getIssueTime()),
                DateTimeUtil.instantToOffsetDateTime(token.getUsedTime()),
                DateTimeUtil.instantToOffsetDateTime(token.getRevokeTime()),
                DateTimeUtil.instantToOffsetDateTime(token.getExpireTime()),
                DateTimeUtil.instantToOffsetDateTime(token.getCreateTime()),
                token.getDescription()
        );
    }

    /**
     * 对内存列表做简单分页。
     */
    private <T> SearchResult<T> paginate(List<T> all, int page, int size) {
        int total = all.size();
        int fromIndex = Math.min(page * size, total);
        int toIndex = Math.min(fromIndex + size, total);
        List<T> pageItems = all.subList(fromIndex, toIndex);
        return SearchResult.<T>builder()
                .items(pageItems)
                .total(total)
                .page(page)
                .size(size)
                .build();
    }

    /**
     * 令牌搜索结果记录。
     */
    public record TokenSearchResult(
            String refreshTokenId,
            String userId,
            String sessionId,
            String clientId,
            String tokenFingerprint,
            String parentTokenId,
            Integer tokenStatus,
            OffsetDateTime issueTime,
            OffsetDateTime usedTime,
            OffsetDateTime revokeTime,
            OffsetDateTime expireTime,
            OffsetDateTime createTime,
            String description
    ) {
    }
}
