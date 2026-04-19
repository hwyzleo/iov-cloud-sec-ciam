package net.hwyz.iov.cloud.sec.ciam.service.infrastructure.repository;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import lombok.RequiredArgsConstructor;
import net.hwyz.iov.cloud.sec.ciam.service.domain.repository.CiamMfaChallengeRepository;
import net.hwyz.iov.cloud.sec.ciam.service.infrastructure.repository.dao.CiamMfaChallengeMapper;
import net.hwyz.iov.cloud.sec.ciam.service.infrastructure.repository.dao.dataobject.CiamMfaChallengeDo;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class CiamMfaChallengeRepositoryImpl implements CiamMfaChallengeRepository {

    private final CiamMfaChallengeMapper mapper;

    @Override
    public Optional<CiamMfaChallengeDo> findByChallengeId(String challengeId) {
        return Optional.ofNullable(mapper.selectOne(
                new LambdaQueryWrapper<CiamMfaChallengeDo>()
                        .eq(CiamMfaChallengeDo::getChallengeId, challengeId)));
    }

    @Override
    public List<CiamMfaChallengeDo> findByUserIdAndStatus(String userId, int challengeStatus) {
        return mapper.selectList(
                new LambdaQueryWrapper<CiamMfaChallengeDo>()
                        .eq(CiamMfaChallengeDo::getUserId, userId)
                        .eq(CiamMfaChallengeDo::getChallengeStatus, challengeStatus)
                        .eq(CiamMfaChallengeDo::getRowValid, 1));
    }

    @Override
    public int insert(CiamMfaChallengeDo entity) {
        return mapper.insert(entity);
    }

    @Override
    public int updateByChallengeId(CiamMfaChallengeDo entity) {
        return mapper.update(entity,
                new LambdaUpdateWrapper<CiamMfaChallengeDo>()
                        .eq(CiamMfaChallengeDo::getChallengeId, entity.getChallengeId()));
    }
}
