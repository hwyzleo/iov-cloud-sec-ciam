package net.hwyz.iov.cloud.sec.ciam.service.infrastructure.repository;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import lombok.RequiredArgsConstructor;
import net.hwyz.iov.cloud.sec.ciam.service.domain.repository.CiamUserProfileRepository;
import net.hwyz.iov.cloud.sec.ciam.service.infrastructure.repository.dao.CiamUserProfileMapper;
import net.hwyz.iov.cloud.sec.ciam.service.infrastructure.repository.dao.dataobject.UserProfilePo;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class CiamUserProfileRepositoryImpl implements CiamUserProfileRepository {

    private final CiamUserProfileMapper mapper;

    @Override
    public Optional<UserProfilePo> findByUserId(String userId) {
        return Optional.ofNullable(mapper.selectOne(
                new LambdaQueryWrapper<UserProfilePo>()
                        .eq(UserProfilePo::getUserId, userId)
                        .eq(UserProfilePo::getRowValid, 1)));
    }

    @Override
    public Optional<UserProfilePo> findByProfileId(String profileId) {
        return Optional.ofNullable(mapper.selectOne(
                new LambdaQueryWrapper<UserProfilePo>()
                        .eq(UserProfilePo::getProfileId, profileId)));
    }

    @Override
    public int insert(UserProfilePo entity) {
        return mapper.insert(entity);
    }

    @Override
    public int updateByProfileId(UserProfilePo entity) {
        return mapper.update(entity,
                new LambdaUpdateWrapper<UserProfilePo>()
                        .eq(UserProfilePo::getProfileId, entity.getProfileId()));
    }

    @Override
    public int updateByUserId(UserProfilePo entity) {
        return mapper.update(entity,
                new LambdaUpdateWrapper<UserProfilePo>()
                        .eq(UserProfilePo::getUserId, entity.getUserId()));
    }

    @Override
    public int physicalDeleteByUserId(String userId) {
        return mapper.delete(
                new LambdaQueryWrapper<UserProfilePo>()
                        .eq(UserProfilePo::getUserId, userId));
    }
}
