package net.hwyz.iov.cloud.sec.ciam.service.application.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.hwyz.iov.cloud.framework.common.exception.BusinessException;
import net.hwyz.iov.cloud.framework.common.util.DateTimeUtil;
import net.hwyz.iov.cloud.framework.web.util.PageUtil;
import net.hwyz.iov.cloud.sec.ciam.service.application.assembler.RefreshTokenAssembler;
import net.hwyz.iov.cloud.sec.ciam.service.application.dto.RefreshTokenDto2;
import net.hwyz.iov.cloud.sec.ciam.service.application.dto.query.TokenQuery;
import net.hwyz.iov.cloud.sec.ciam.service.common.exception.CiamErrorCode;
import net.hwyz.iov.cloud.sec.ciam.service.domain.model.RefreshToken;
import net.hwyz.iov.cloud.sec.ciam.service.domain.model.TokenSearchCriteria;
import net.hwyz.iov.cloud.sec.ciam.service.domain.repository.RefreshTokenRepository;
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

    private final RefreshTokenRepository refreshTokenRepository;

    public List<TokenSearchResult> queryTokenList(TokenQuery query) {
        TokenSearchCriteria criteria = TokenSearchCriteria.builder()
                .refreshTokenId(query.getRefreshTokenId())
                .userId(query.getUserId())
                .sessionId(query.getSessionId())
                .clientId(query.getClientId())
                .tokenStatus(query.getTokenStatus())
                .startTime(query.getStartTime())
                .endTime(query.getEndTime())
                .build();
        List<RefreshToken> allTokens = refreshTokenRepository.search(criteria);

        return PageUtil.convert(allTokens, this::toTokenSearchResult);
    }

    public RefreshTokenDto2 queryToken(String refreshTokenId) {
        RefreshToken token = refreshTokenRepository.findByRefreshTokenId(refreshTokenId)
                .orElseThrow(() -> new BusinessException(CiamErrorCode.TOKEN_INVALID));
        return RefreshTokenAssembler.INSTANCE.toDto(token);
    }

    public List<RefreshTokenDto2> queryUserTokens(String userId) {
        return refreshTokenRepository.findByUserId(userId).stream()
                .map(RefreshTokenAssembler.INSTANCE::toDto)
                .collect(Collectors.toList());
    }

    public List<RefreshTokenDto2> querySessionTokens(String sessionId) {
        return refreshTokenRepository.findBySessionId(sessionId).stream()
                .map(RefreshTokenAssembler.INSTANCE::toDto)
                .collect(Collectors.toList());
    }

    private TokenSearchResult toTokenSearchResult(RefreshToken token) {
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
                null, // PO 才有 createTime，Domain 暂未包含，如需可添加
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
