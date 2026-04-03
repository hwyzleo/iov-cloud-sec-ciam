package net.hwyz.iov.cloud.sec.ciam.infrastructure.repository;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import lombok.RequiredArgsConstructor;
import net.hwyz.iov.cloud.sec.ciam.domain.enums.TokenStatus;
import net.hwyz.iov.cloud.sec.ciam.domain.repository.CiamRefreshTokenRepository;
import net.hwyz.iov.cloud.sec.ciam.infrastructure.repository.dao.CiamRefreshTokenMapper;
import net.hwyz.iov.cloud.sec.ciam.infrastructure.repository.dao.dataobject.CiamRefreshTokenDo;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class CiamRefreshTokenRepositoryImpl implements CiamRefreshTokenRepository {

    private final CiamRefreshTokenMapper mapper;

    @Override
    public Optional<CiamRefreshTokenDo> findByTokenFingerprint(String tokenFingerprint) {
        return Optional.ofNullable(mapper.selectOne(
                new LambdaQueryWrapper<CiamRefreshTokenDo>()
                        .eq(CiamRefreshTokenDo::getTokenFingerprint, tokenFingerprint)));
    }

    @Override
    public Optional<CiamRefreshTokenDo> findByRefreshTokenId(String refreshTokenId) {
        return Optional.ofNullable(mapper.selectOne(
                new LambdaQueryWrapper<CiamRefreshTokenDo>()
                        .eq(CiamRefreshTokenDo::getRefreshTokenId, refreshTokenId)));
    }

    @Override
    public List<CiamRefreshTokenDo> findByUserIdAndStatus(String userId, int tokenStatus) {
        return mapper.selectList(
                new LambdaQueryWrapper<CiamRefreshTokenDo>()
                        .eq(CiamRefreshTokenDo::getUserId, userId)
                        .eq(CiamRefreshTokenDo::getTokenStatus, tokenStatus));
    }

    @Override
    public List<CiamRefreshTokenDo> findBySessionIdAndStatus(String sessionId, int tokenStatus) {
        return mapper.selectList(
                new LambdaQueryWrapper<CiamRefreshTokenDo>()
                        .eq(CiamRefreshTokenDo::getSessionId, sessionId)
                        .eq(CiamRefreshTokenDo::getTokenStatus, tokenStatus));
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
    public int revokeAllByUserId(String userId) {
        CiamRefreshTokenDo update = new CiamRefreshTokenDo();
        update.setTokenStatus(TokenStatus.REVOKED.getCode());
        update.setRevokeTime(LocalDateTime.now());
        return mapper.update(update,
                new LambdaUpdateWrapper<CiamRefreshTokenDo>()
                        .eq(CiamRefreshTokenDo::getUserId, userId)
                        .eq(CiamRefreshTokenDo::getTokenStatus, TokenStatus.ACTIVE.getCode()));
    }

    @Override
    public int revokeAllBySessionId(String sessionId) {
        CiamRefreshTokenDo update = new CiamRefreshTokenDo();
        update.setTokenStatus(TokenStatus.REVOKED.getCode());
        update.setRevokeTime(LocalDateTime.now());
        return mapper.update(update,
                new LambdaUpdateWrapper<CiamRefreshTokenDo>()
                        .eq(CiamRefreshTokenDo::getSessionId, sessionId)
                        .eq(CiamRefreshTokenDo::getTokenStatus, TokenStatus.ACTIVE.getCode()));
    }
}
