package net.hwyz.iov.cloud.sec.ciam.service.infrastructure.repository;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import lombok.RequiredArgsConstructor;
import net.hwyz.iov.cloud.sec.ciam.service.common.security.FieldEncryptor;
import net.hwyz.iov.cloud.sec.ciam.service.domain.repository.CiamUserIdentityRepository;
import net.hwyz.iov.cloud.sec.ciam.service.infrastructure.repository.dao.CiamUserIdentityMapper;
import net.hwyz.iov.cloud.sec.ciam.service.infrastructure.repository.dao.dataobject.CiamUserIdentityDo;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class CiamUserIdentityRepositoryImpl implements CiamUserIdentityRepository {

    private final CiamUserIdentityMapper mapper;

    @Override
    public Optional<CiamUserIdentityDo> findByTypeAndHash(String identityType, String identityHash) {
        return Optional.ofNullable(mapper.selectOne(
                new LambdaQueryWrapper<CiamUserIdentityDo>()
                        .eq(CiamUserIdentityDo::getIdentityType, identityType)
                        .eq(CiamUserIdentityDo::getIdentityHash, identityHash)
                        .eq(CiamUserIdentityDo::getRowValid, 1)));
    }

    @Override
    public Optional<CiamUserIdentityDo> findByTypeAndValue(String identityType, String identityValue) {
        String identityHash = FieldEncryptor.hash(identityValue);
        return Optional.ofNullable(mapper.selectOne(
                new LambdaQueryWrapper<CiamUserIdentityDo>()
                        .eq(CiamUserIdentityDo::getIdentityType, identityType)
                        .eq(CiamUserIdentityDo::getIdentityHash, identityHash)
                        .eq(CiamUserIdentityDo::getRowValid, 1)));
    }

    @Override
    public List<CiamUserIdentityDo> findByUserId(String userId) {
        return mapper.selectList(
                new LambdaQueryWrapper<CiamUserIdentityDo>()
                        .eq(CiamUserIdentityDo::getUserId, userId)
                        .eq(CiamUserIdentityDo::getRowValid, 1));
    }

    @Override
    public Optional<CiamUserIdentityDo> findByIdentityId(String identityId) {
        return Optional.ofNullable(mapper.selectOne(
                new LambdaQueryWrapper<CiamUserIdentityDo>()
                        .eq(CiamUserIdentityDo::getIdentityId, identityId)));
    }

    @Override
    public int insert(CiamUserIdentityDo entity) {
        return mapper.insert(entity);
    }

    @Override
    public int updateByIdentityId(CiamUserIdentityDo entity) {
        return mapper.update(entity,
                new LambdaUpdateWrapper<CiamUserIdentityDo>()
                        .eq(CiamUserIdentityDo::getIdentityId, entity.getIdentityId()));
    }

    @Override
    public int updateIdentityValue(String userId, String identityType, String identityHash) {
        return mapper.update(null,
                new LambdaUpdateWrapper<CiamUserIdentityDo>()
                        .eq(CiamUserIdentityDo::getUserId, userId)
                        .eq(CiamUserIdentityDo::getIdentityType, identityType)
                        .set(CiamUserIdentityDo::getIdentityHash, identityHash));
    }

    @Override
    public int updateIdentityValue(String userId, String identityType, String identityHash, String identityValue) {
        return mapper.update(null,
                new LambdaUpdateWrapper<CiamUserIdentityDo>()
                        .eq(CiamUserIdentityDo::getUserId, userId)
                        .eq(CiamUserIdentityDo::getIdentityType, identityType)
                        .set(CiamUserIdentityDo::getIdentityHash, identityHash)
                        .set(CiamUserIdentityDo::getIdentityValue, identityValue));
    }

    @Override
    public int physicalDeleteByUserId(String userId) {
        return mapper.delete(
                new LambdaQueryWrapper<CiamUserIdentityDo>()
                        .eq(CiamUserIdentityDo::getUserId, userId));
    }
}
