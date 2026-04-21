package net.hwyz.iov.cloud.sec.ciam.service.infrastructure.repository;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import lombok.RequiredArgsConstructor;
import net.hwyz.iov.cloud.sec.ciam.service.domain.query.TokenQuery;
import net.hwyz.iov.cloud.sec.ciam.service.domain.repository.CiamRefreshTokenRepository;
import net.hwyz.iov.cloud.sec.ciam.service.infrastructure.repository.dao.CiamRefreshTokenMapper;
import net.hwyz.iov.cloud.sec.ciam.service.infrastructure.repository.dao.dataobject.CiamRefreshTokenDo;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class CiamRefreshTokenRepositoryImpl implements CiamRefreshTokenRepository {

    private final CiamRefreshTokenMapper mapper;

    @Override
    public Optional<CiamRefreshTokenDo> findByRefreshTokenId(String refreshTokenId) {
        return Optional.ofNullable(mapper.selectOne(
                new LambdaQueryWrapper<CiamRefreshTokenDo>()
                        .eq(CiamRefreshTokenDo::getRefreshTokenId, refreshTokenId)
                        .eq(CiamRefreshTokenDo::getRowValid, 1)));
    }

    @Override
    public int insert(CiamRefreshTokenDo entity) {
        return mapper.insert(entity);
    }

    @Override
    public int updateByRefreshTokenId(CiamRefreshTokenDo entity) {
        return mapper.update(entity,
                new LambdaUpdateWrapper<CiamRefreshTokenDo>()
                        .eq(CiamRefreshTokenDo::getRefreshTokenId, entity.getRefreshTokenId()));
    }

    @Override
    public int revokeAllBySessionId(String sessionId) {
        CiamRefreshTokenDo update = new CiamRefreshTokenDo();
        update.setTokenStatus(0); 
        update.setRevokeTime(Instant.now());
        return mapper.update(update,
                new LambdaUpdateWrapper<CiamRefreshTokenDo>()
                        .eq(CiamRefreshTokenDo::getSessionId, sessionId)
                        .eq(CiamRefreshTokenDo::getTokenStatus, 1));
    }

    @Override
    public int revokeAllByUserId(String userId) {
        CiamRefreshTokenDo update = new CiamRefreshTokenDo();
        update.setTokenStatus(0);
        update.setRevokeTime(Instant.now());
        return mapper.update(update,
                new LambdaUpdateWrapper<CiamRefreshTokenDo>()
                        .eq(CiamRefreshTokenDo::getUserId, userId)
                        .eq(CiamRefreshTokenDo::getTokenStatus, 1));
    }

    @Override
    public Optional<CiamRefreshTokenDo> findByTokenFingerprint(String tokenFingerprint) {
        return Optional.ofNullable(mapper.selectOne(
                new LambdaQueryWrapper<CiamRefreshTokenDo>()
                        .eq(CiamRefreshTokenDo::getTokenFingerprint, tokenFingerprint)
                        .eq(CiamRefreshTokenDo::getRowValid, 1)));
    }

    @Override
    public List<CiamRefreshTokenDo> findAll() {
        return mapper.searchTokens(null, null, null, null, null, null, null);
    }

    @Override
    public List<CiamRefreshTokenDo> search(TokenQuery query) {
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
    public List<CiamRefreshTokenDo> findByUserId(String userId) {
        return mapper.searchTokens(null, userId, null, null, null, null, null);
    }

    @Override
    public List<CiamRefreshTokenDo> findBySessionId(String sessionId) {
        return mapper.searchTokens(null, null, sessionId, null, null, null, null);
    }
}
