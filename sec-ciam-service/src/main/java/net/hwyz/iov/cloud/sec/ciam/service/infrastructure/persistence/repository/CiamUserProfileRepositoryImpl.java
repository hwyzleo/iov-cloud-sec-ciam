package net.hwyz.iov.cloud.sec.ciam.service.infrastructure.persistence.repository;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import lombok.RequiredArgsConstructor;
import net.hwyz.iov.cloud.sec.ciam.service.domain.model.UserProfile;
import net.hwyz.iov.cloud.sec.ciam.service.domain.repository.CiamUserProfileRepository;
import net.hwyz.iov.cloud.sec.ciam.service.infrastructure.persistence.converter.UserProfilePoConverter;
import net.hwyz.iov.cloud.sec.ciam.service.infrastructure.persistence.mapper.CiamUserProfileMapper;
import net.hwyz.iov.cloud.sec.ciam.service.infrastructure.persistence.po.UserProfilePo;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class CiamUserProfileRepositoryImpl implements CiamUserProfileRepository {

    private final CiamUserProfileMapper mapper;
    private final UserProfilePoConverter userProfilePoConverter;

    @Override
    public Optional<UserProfile> findByUserId(String userId) {
        UserProfilePo po = mapper.selectOne(
                new LambdaQueryWrapper<UserProfilePo>()
                        .eq(UserProfilePo::getUserId, userId)
                        .eq(UserProfilePo::getRowValid, 1));
        return Optional.ofNullable(userProfilePoConverter.toDomain(po));
    }

    @Override
    public Optional<UserProfile> findByProfileId(String profileId) {
        UserProfilePo po = mapper.selectOne(
                new LambdaQueryWrapper<UserProfilePo>()
                        .eq(UserProfilePo::getProfileId, profileId));
        return Optional.ofNullable(userProfilePoConverter.toDomain(po));
    }

    @Override
    public int insert(UserProfile entity) {
        return mapper.insert(userProfilePoConverter.toPo(entity));
    }

    @Override
    public int updateByProfileId(UserProfile entity) {
        UserProfilePo po = userProfilePoConverter.toPo(entity);
        return mapper.update(po,
                new LambdaUpdateWrapper<UserProfilePo>()
                        .eq(UserProfilePo::getProfileId, po.getProfileId()));
    }

    @Override
    public int updateByUserId(UserProfile entity) {
        UserProfilePo po = userProfilePoConverter.toPo(entity);
        return mapper.update(po,
                new LambdaUpdateWrapper<UserProfilePo>()
                        .eq(UserProfilePo::getUserId, po.getUserId()));
    }

    @Override
    public int physicalDeleteByUserId(String userId) {
        return mapper.delete(
                new LambdaQueryWrapper<UserProfilePo>()
                        .eq(UserProfilePo::getUserId, userId));
    }
}
