package net.hwyz.iov.cloud.sec.ciam.service.infrastructure.persistence.repository;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import lombok.RequiredArgsConstructor;
import net.hwyz.iov.cloud.sec.ciam.service.common.security.FieldEncryptor;
import net.hwyz.iov.cloud.sec.ciam.service.domain.repository.CiamUserIdentityRepository;
import net.hwyz.iov.cloud.sec.ciam.service.infrastructure.persistence.mapper.CiamUserIdentityMapper;
import net.hwyz.iov.cloud.sec.ciam.service.infrastructure.persistence.po.UserIdentityPo;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class CiamUserIdentityRepositoryImpl implements CiamUserIdentityRepository {

    private final CiamUserIdentityMapper mapper;

    @Override
    public Optional<UserIdentityPo> findByTypeAndHash(String identityType, String identityHash) {
        return Optional.ofNullable(mapper.selectOne(
                new LambdaQueryWrapper<UserIdentityPo>()
                        .eq(UserIdentityPo::getIdentityType, identityType)
                        .eq(UserIdentityPo::getIdentityHash, identityHash)
                        .eq(UserIdentityPo::getRowValid, 1)));
    }

    @Override
    public Optional<UserIdentityPo> findByTypeAndValue(String identityType, String identityValue) {
        String identityHash = FieldEncryptor.hash(identityValue);
        return Optional.ofNullable(mapper.selectOne(
                new LambdaQueryWrapper<UserIdentityPo>()
                        .eq(UserIdentityPo::getIdentityType, identityType)
                        .eq(UserIdentityPo::getIdentityHash, identityHash)
                        .eq(UserIdentityPo::getRowValid, 1)));
    }

    @Override
    public List<UserIdentityPo> findByUserId(String userId) {
        return mapper.selectList(
                new LambdaQueryWrapper<UserIdentityPo>()
                        .eq(UserIdentityPo::getUserId, userId)
                        .eq(UserIdentityPo::getRowValid, 1));
    }

    @Override
    public Optional<UserIdentityPo> findByIdentityId(String identityId) {
        return Optional.ofNullable(mapper.selectOne(
                new LambdaQueryWrapper<UserIdentityPo>()
                        .eq(UserIdentityPo::getIdentityId, identityId)));
    }

    @Override
    public int insert(UserIdentityPo entity) {
        return mapper.insert(entity);
    }

    @Override
    public int updateByIdentityId(UserIdentityPo entity) {
        return mapper.update(entity,
                new LambdaUpdateWrapper<UserIdentityPo>()
                        .eq(UserIdentityPo::getIdentityId, entity.getIdentityId()));
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
