package net.hwyz.iov.cloud.sec.ciam.service.infrastructure.persistence.repository;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import lombok.RequiredArgsConstructor;
import net.hwyz.iov.cloud.sec.ciam.service.domain.model.RefreshToken;
import net.hwyz.iov.cloud.sec.ciam.service.domain.model.TokenSearchCriteria;
import net.hwyz.iov.cloud.sec.ciam.service.domain.repository.RefreshTokenRepository;
import net.hwyz.iov.cloud.sec.ciam.service.infrastructure.persistence.converter.RefreshTokenPoConverter;
import net.hwyz.iov.cloud.sec.ciam.service.infrastructure.persistence.mapper.CiamRefreshTokenMapper;
import net.hwyz.iov.cloud.sec.ciam.service.infrastructure.persistence.po.RefreshTokenPo;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Repository
@RequiredArgsConstructor
public class RefreshTokenRepositoryImpl implements RefreshTokenRepository {

    private final CiamRefreshTokenMapper mapper;

    @Override
    public Optional<RefreshToken> findByRefreshTokenId(String refreshTokenId) {
        return Optional.ofNullable(mapper.selectOne(
                new LambdaQueryWrapper<RefreshTokenPo>()
                        .eq(RefreshTokenPo::getRefreshTokenId, refreshTokenId)
                        .eq(RefreshTokenPo::getRowValid, 1)))
                .map(RefreshTokenPoConverter.INSTANCE::toDomain);
    }

    @Override
    public int insert(RefreshToken entity) {
        return mapper.insert(RefreshTokenPoConverter.INSTANCE.toDo(entity));
    }

    @Override
    public int updateByRefreshTokenId(RefreshToken entity) {
        RefreshTokenPo po = RefreshTokenPoConverter.INSTANCE.toDo(entity);
        return mapper.update(po,
                new LambdaUpdateWrapper<RefreshTokenPo>()
                        .eq(RefreshTokenPo::getRefreshTokenId, po.getRefreshTokenId()));
    }

    @Override
    public int revokeAllBySessionId(String sessionId) {
        RefreshTokenPo update = new RefreshTokenPo();
        update.setTokenStatus(0); 
        update.setRevokeTime(Instant.now());
        return mapper.update(update,
                new LambdaUpdateWrapper<RefreshTokenPo>()
                        .eq(RefreshTokenPo::getSessionId, sessionId)
                        .eq(RefreshTokenPo::getTokenStatus, 1));
    }

    @Override
    public int revokeAllByUserId(String userId) {
        RefreshTokenPo update = new RefreshTokenPo();
        update.setTokenStatus(0);
        update.setRevokeTime(Instant.now());
        return mapper.update(update,
                new LambdaUpdateWrapper<RefreshTokenPo>()
                        .eq(RefreshTokenPo::getUserId, userId)
                        .eq(RefreshTokenPo::getTokenStatus, 1));
    }

    @Override
    public Optional<RefreshToken> findByTokenFingerprint(String tokenFingerprint) {
        return Optional.ofNullable(mapper.selectOne(
                new LambdaQueryWrapper<RefreshTokenPo>()
                        .eq(RefreshTokenPo::getTokenFingerprint, tokenFingerprint)
                        .eq(RefreshTokenPo::getRowValid, 1)))
                .map(RefreshTokenPoConverter.INSTANCE::toDomain);
    }

    @Override
    public List<RefreshToken> search(TokenSearchCriteria criteria) {
        return mapper.searchTokens(
                criteria.getRefreshTokenId(),
                criteria.getUserId(),
                criteria.getSessionId(),
                criteria.getClientId(),
                criteria.getTokenStatus(),
                criteria.getStartTime() != null ? criteria.getStartTime().toInstant() : null,
                criteria.getEndTime() != null ? criteria.getEndTime().toInstant() : null
        ).stream().map(RefreshTokenPoConverter.INSTANCE::toDomain).collect(Collectors.toList());
    }

    @Override
    public List<RefreshToken> findByUserId(String userId) {
        return mapper.searchTokens(null, userId, null, null, null, null, null)
                .stream().map(RefreshTokenPoConverter.INSTANCE::toDomain).collect(Collectors.toList());
    }

    @Override
    public List<RefreshToken> findBySessionId(String sessionId) {
        return mapper.searchTokens(null, null, sessionId, null, null, null, null)
                .stream().map(RefreshTokenPoConverter.INSTANCE::toDomain).collect(Collectors.toList());
    }
}
