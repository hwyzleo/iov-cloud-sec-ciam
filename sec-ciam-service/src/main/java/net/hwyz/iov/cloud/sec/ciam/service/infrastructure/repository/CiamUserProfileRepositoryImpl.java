package net.hwyz.iov.cloud.sec.ciam.service.infrastructure.repository;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import lombok.RequiredArgsConstructor;
import net.hwyz.iov.cloud.sec.ciam.service.domain.repository.CiamUserProfileRepository;
import net.hwyz.iov.cloud.sec.ciam.service.infrastructure.repository.dao.CiamUserProfileMapper;
import net.hwyz.iov.cloud.sec.ciam.service.infrastructure.repository.dao.dataobject.CiamUserProfileDo;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class CiamUserProfileRepositoryImpl implements CiamUserProfileRepository {

    private final CiamUserProfileMapper mapper;

    @Override
    public Optional<CiamUserProfileDo> findByUserId(String userId) {
        return Optional.ofNullable(mapper.selectOne(
                new LambdaQueryWrapper<CiamUserProfileDo>()
                        .eq(CiamUserProfileDo::getUserId, userId)
                        .eq(CiamUserProfileDo::getRowValid, 1)));
    }

    @Override
    public Optional<CiamUserProfileDo> findByProfileId(String profileId) {
        return Optional.ofNullable(mapper.selectOne(
                new LambdaQueryWrapper<CiamUserProfileDo>()
                        .eq(CiamUserProfileDo::getProfileId, profileId)));
    }

    @Override
    public int insert(CiamUserProfileDo entity) {
        return mapper.insert(entity);
    }

    @Override
    public int updateByProfileId(CiamUserProfileDo entity) {
        return mapper.update(entity,
                new LambdaUpdateWrapper<CiamUserProfileDo>()
                        .eq(CiamUserProfileDo::getProfileId, entity.getProfileId()));
    }

    @Override
    public int updateByUserId(CiamUserProfileDo entity) {
        return mapper.update(entity,
                new LambdaUpdateWrapper<CiamUserProfileDo>()
                        .eq(CiamUserProfileDo::getUserId, entity.getUserId()));
    }

    @Override
    public int physicalDeleteByUserId(String userId) {
        return mapper.delete(
                new LambdaQueryWrapper<CiamUserProfileDo>()
                        .eq(CiamUserProfileDo::getUserId, userId));
    }
}
