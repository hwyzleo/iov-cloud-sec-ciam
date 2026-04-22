package net.hwyz.iov.cloud.sec.ciam.service.infrastructure.persistence.repository;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import lombok.RequiredArgsConstructor;
import net.hwyz.iov.cloud.sec.ciam.service.common.security.FieldEncryptor;
import net.hwyz.iov.cloud.sec.ciam.service.domain.model.UserIdentity;
import net.hwyz.iov.cloud.sec.ciam.service.domain.repository.CiamUserIdentityRepository;
import net.hwyz.iov.cloud.sec.ciam.service.infrastructure.persistence.converter.UserIdentityPoConverter;
import net.hwyz.iov.cloud.sec.ciam.service.infrastructure.persistence.mapper.CiamUserIdentityMapper;
import net.hwyz.iov.cloud.sec.ciam.service.infrastructure.persistence.po.UserIdentityPo;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class CiamUserIdentityRepositoryImpl implements CiamUserIdentityRepository {

    private final CiamUserIdentityMapper mapper;
    private final UserIdentityPoConverter userIdentityPoConverter;

    @Override
    public Optional<UserIdentity> findByTypeAndHash(String identityType, String identityHash) {
        UserIdentityPo po = mapper.selectOne(
                new LambdaQueryWrapper<UserIdentityPo>()
                        .eq(UserIdentityPo::getIdentityType, identityType)
                        .eq(UserIdentityPo::getIdentityHash, identityHash)
                        .eq(UserIdentityPo::getRowValid, 1));
        return Optional.ofNullable(userIdentityPoConverter.toDomain(po));
    }

    @Override
    public Optional<UserIdentity> findByTypeAndValue(String identityType, String identityValue) {
        String identityHash = FieldEncryptor.hash(identityValue);
        UserIdentityPo po = mapper.selectOne(
                new LambdaQueryWrapper<UserIdentityPo>()
                        .eq(UserIdentityPo::getIdentityType, identityType)
                        .eq(UserIdentityPo::getIdentityHash, identityHash)
                        .eq(UserIdentityPo::getRowValid, 1));
        return Optional.ofNullable(userIdentityPoConverter.toDomain(po));
    }

    @Override
    public List<UserIdentity> findByUserId(String userId) {
        List<UserIdentityPo> pos = mapper.selectList(
                new LambdaQueryWrapper<UserIdentityPo>()
                        .eq(UserIdentityPo::getUserId, userId)
                        .eq(UserIdentityPo::getRowValid, 1));
        return pos.stream().map(userIdentityPoConverter::toDomain).toList();
    }

    @Override
    public Optional<UserIdentity> findByIdentityId(String identityId) {
        UserIdentityPo po = mapper.selectOne(
                new LambdaQueryWrapper<UserIdentityPo>()
                        .eq(UserIdentityPo::getIdentityId, identityId));
        return Optional.ofNullable(userIdentityPoConverter.toDomain(po));
    }

    @Override
    public int insert(UserIdentity entity) {
        return mapper.insert(userIdentityPoConverter.toPo(entity));
    }

    @Override
    public int updateByIdentityId(UserIdentity entity) {
        UserIdentityPo po = userIdentityPoConverter.toPo(entity);
        return mapper.update(po,
                new LambdaUpdateWrapper<UserIdentityPo>()
                        .eq(UserIdentityPo::getIdentityId, po.getIdentityId()));
    }

    @Override
    public int updateIdentityValue(String userId, String identityType, String identityHash) {
        return mapper.update(null,
                new LambdaUpdateWrapper<UserIdentityPo>()
                        .eq(UserIdentityPo::getUserId, userId)
                        .eq(UserIdentityPo::getIdentityType, identityType)
                        .set(UserIdentityPo::getIdentityHash, identityHash));
    }

    @Override
    public int updateIdentityValue(String userId, String identityType, String identityHash, String identityValue) {
        return mapper.update(null,
                new LambdaUpdateWrapper<UserIdentityPo>()
                        .eq(UserIdentityPo::getUserId, userId)
                        .eq(UserIdentityPo::getIdentityType, identityType)
                        .set(UserIdentityPo::getIdentityHash, identityHash)
                        .set(UserIdentityPo::getIdentityValue, identityValue));
    }

    @Override
    public int physicalDeleteByUserId(String userId) {
        return mapper.delete(
                new LambdaQueryWrapper<UserIdentityPo>()
                        .eq(UserIdentityPo::getUserId, userId));
    }
}
