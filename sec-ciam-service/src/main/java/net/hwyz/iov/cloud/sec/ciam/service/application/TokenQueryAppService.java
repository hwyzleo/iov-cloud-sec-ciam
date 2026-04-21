package net.hwyz.iov.cloud.sec.ciam.service.application;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.hwyz.iov.cloud.framework.common.exception.BusinessException;
import net.hwyz.iov.cloud.framework.common.util.DateTimeUtil;
import net.hwyz.iov.cloud.framework.web.util.PageUtil;
import net.hwyz.iov.cloud.sec.ciam.service.application.dto.RefreshTokenDTO;
import net.hwyz.iov.cloud.sec.ciam.service.application.mapper.RefreshTokenMapper;
import net.hwyz.iov.cloud.sec.ciam.service.common.exception.CiamErrorCode;
import net.hwyz.iov.cloud.sec.ciam.service.domain.query.TokenQuery;
import net.hwyz.iov.cloud.sec.ciam.service.domain.repository.CiamRefreshTokenRepository;
import net.hwyz.iov.cloud.sec.ciam.service.infrastructure.repository.dao.dataobject.CiamRefreshTokenDo;
import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 运营后台令牌查询应用服务
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TokenQueryAppService {

    private final CiamRefreshTokenRepository refreshTokenRepository;

    public List<TokenSearchResult> queryTokenList(TokenQuery query) {
        List<CiamRefreshTokenDo> allTokens = refreshTokenRepository.search(query);

        return PageUtil.convert(allTokens, this::toTokenSearchResult);
    }

    public RefreshTokenDTO queryToken(String refreshTokenId) {
        CiamRefreshTokenDo token = refreshTokenRepository.findByRefreshTokenId(refreshTokenId)
                .orElseThrow(() -> new BusinessException(CiamErrorCode.TOKEN_INVALID));
        return RefreshTokenMapper.INSTANCE.toDto(RefreshTokenMapper.INSTANCE.toDomain(token));
    }

    public List<RefreshTokenDTO> queryUserTokens(String userId) {
        return refreshTokenRepository.findByUserId(userId).stream()
                .map(doObj -> RefreshTokenMapper.INSTANCE.toDto(RefreshTokenMapper.INSTANCE.toDomain(doObj)))
                .collect(Collectors.toList());
    }

    public List<RefreshTokenDTO> querySessionTokens(String sessionId) {
        return refreshTokenRepository.findBySessionId(sessionId).stream()
                .map(doObj -> RefreshTokenMapper.INSTANCE.toDto(RefreshTokenMapper.INSTANCE.toDomain(doObj)))
                .collect(Collectors.toList());
    }

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
