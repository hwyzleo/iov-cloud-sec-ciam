package net.hwyz.iov.cloud.sec.ciam.service.infrastructure.persistence.repository;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import lombok.RequiredArgsConstructor;
import net.hwyz.iov.cloud.sec.ciam.service.domain.repository.CiamMfaChallengeRepository;
import net.hwyz.iov.cloud.sec.ciam.service.infrastructure.persistence.mapper.CiamMfaChallengeMapper;
import net.hwyz.iov.cloud.sec.ciam.service.infrastructure.persistence.po.MfaChallengePo;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class CiamMfaChallengeRepositoryImpl implements CiamMfaChallengeRepository {

    private final CiamMfaChallengeMapper mapper;

    @Override
    public Optional<MfaChallengePo> findByChallengeId(String challengeId) {
        return Optional.ofNullable(mapper.selectOne(
                new LambdaQueryWrapper<MfaChallengePo>()
                        .eq(MfaChallengePo::getChallengeId, challengeId)));
    }

    @Override
    public List<MfaChallengePo> findByUserIdAndStatus(String userId, int challengeStatus) {
        return mapper.selectList(
                new LambdaQueryWrapper<MfaChallengePo>()
                        .eq(MfaChallengePo::getUserId, userId)
                        .eq(MfaChallengePo::getChallengeStatus, challengeStatus)
                        .eq(MfaChallengePo::getRowValid, 1));
    }

    @Override
    public int insert(MfaChallengePo entity) {
        return mapper.insert(entity);
    }

    @Override
    public int updateByChallengeId(MfaChallengePo entity) {
        return mapper.update(entity,
                new LambdaUpdateWrapper<MfaChallengePo>()
                        .eq(MfaChallengePo::getChallengeId, entity.getChallengeId()));
    }
}
