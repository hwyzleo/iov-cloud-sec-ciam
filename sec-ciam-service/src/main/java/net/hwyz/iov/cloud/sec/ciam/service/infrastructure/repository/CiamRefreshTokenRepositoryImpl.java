package net.hwyz.iov.cloud.sec.ciam.service.infrastructure.repository;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import lombok.RequiredArgsConstructor;
import net.hwyz.iov.cloud.sec.ciam.service.domain.query.TokenQuery;
import net.hwyz.iov.cloud.sec.ciam.service.domain.repository.CiamRefreshTokenRepository;
import net.hwyz.iov.cloud.sec.ciam.service.infrastructure.repository.dao.CiamRefreshTokenMapper;
import net.hwyz.iov.cloud.sec.ciam.service.infrastructure.repository.dao.dataobject.RefreshTokenPo;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class CiamRefreshTokenRepositoryImpl implements CiamRefreshTokenRepository {

    private final CiamRefreshTokenMapper mapper;

    @Override
    public Optional<RefreshTokenPo> findByRefreshTokenId(String refreshTokenId) {
        return Optional.ofNullable(mapper.selectOne(
                new LambdaQueryWrapper<RefreshTokenPo>()
                        .eq(RefreshTokenPo::getRefreshTokenId, refreshTokenId)
                        .eq(RefreshTokenPo::getRowValid, 1)));
    }

    @Override
    public int insert(RefreshTokenPo entity) {
        return mapper.insert(entity);
    }

    @Override
    public int updateByRefreshTokenId(RefreshTokenPo entity) {
        return mapper.update(entity,
                new LambdaUpdateWrapper<RefreshTokenPo>()
                        .eq(RefreshTokenPo::getRefreshTokenId, entity.getRefreshTokenId()));
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
    public Optional<RefreshTokenPo> findByTokenFingerprint(String tokenFingerprint) {
        return Optional.ofNullable(mapper.selectOne(
                new LambdaQueryWrapper<RefreshTokenPo>()
                        .eq(RefreshTokenPo::getTokenFingerprint, tokenFingerprint)
                        .eq(RefreshTokenPo::getRowValid, 1)));
    }

    @Override
    public List<RefreshTokenPo> findAll() {
        return mapper.searchTokens(null, null, null, null, null, null, null);
    }

    @Override
    public List<RefreshTokenPo> search(TokenQuery query) {
        return mapper.searchTokens(
                query.getRefreshTokenId(),
                query.getUserId(),
                query.getSessionId(),
                query.getClientId(),
                query.getTokenStatus(),
                query.getStartTime() != null ? query.getStartTime().toInstant() : null,
                query.getEndTime() != null ? query.getEndTime().toInstant() : null
        );
    }

    @Override
    public List<RefreshTokenPo> findByUserId(String userId) {
        return mapper.searchTokens(null, userId, null, null, null, null, null);
    }

    @Override
    public List<RefreshTokenPo> findBySessionId(String sessionId) {
        return mapper.searchTokens(null, null, sessionId, null, null, null, null);
    }
}
