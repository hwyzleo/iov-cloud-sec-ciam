package net.hwyz.iov.cloud.sec.ciam.service.infrastructure.persistence.repository;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import lombok.RequiredArgsConstructor;
import net.hwyz.iov.cloud.sec.ciam.service.domain.model.MfaChallenge;
import net.hwyz.iov.cloud.sec.ciam.service.domain.repository.MfaChallengeRepository;
import net.hwyz.iov.cloud.sec.ciam.service.infrastructure.persistence.converter.MfaChallengePoConverter;
import net.hwyz.iov.cloud.sec.ciam.service.infrastructure.persistence.mapper.CiamMfaChallengeMapper;
import net.hwyz.iov.cloud.sec.ciam.service.infrastructure.persistence.po.MfaChallengePo;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Repository
@RequiredArgsConstructor
public class MfaChallengeRepositoryImpl implements MfaChallengeRepository {

    private final CiamMfaChallengeMapper mapper;

    @Override
    public Optional<MfaChallenge> findByChallengeId(String challengeId) {
        MfaChallengePo po = mapper.selectOne(
                new LambdaQueryWrapper<MfaChallengePo>()
                        .eq(MfaChallengePo::getChallengeId, challengeId));
        return Optional.ofNullable(MfaChallengePoConverter.INSTANCE.toDomain(po));
    }

    @Override
    public List<MfaChallenge> findByUserIdAndStatus(String userId, int challengeStatus) {
        return mapper.selectList(
                new LambdaQueryWrapper<MfaChallengePo>()
                        .eq(MfaChallengePo::getUserId, userId)
                        .eq(MfaChallengePo::getChallengeStatus, challengeStatus)
                        .eq(MfaChallengePo::getRowValid, 1))
                .stream()
                .map(MfaChallengePoConverter.INSTANCE::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public int insert(MfaChallenge entity) {
        return mapper.insert(MfaChallengePoConverter.INSTANCE.toPo(entity));
    }

    @Override
    public int updateByChallengeId(MfaChallenge entity) {
        return mapper.update(MfaChallengePoConverter.INSTANCE.toPo(entity),
                new LambdaUpdateWrapper<MfaChallengePo>()
                        .eq(MfaChallengePo::getChallengeId, entity.getChallengeId()));
    }
}
