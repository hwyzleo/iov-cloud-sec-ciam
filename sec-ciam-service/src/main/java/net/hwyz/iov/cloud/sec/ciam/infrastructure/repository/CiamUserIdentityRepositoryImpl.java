package net.hwyz.iov.cloud.sec.ciam.infrastructure.repository;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import lombok.RequiredArgsConstructor;
import net.hwyz.iov.cloud.sec.ciam.domain.repository.CiamUserIdentityRepository;
import net.hwyz.iov.cloud.sec.ciam.infrastructure.repository.dao.CiamUserIdentityMapper;
import net.hwyz.iov.cloud.sec.ciam.infrastructure.repository.dao.dataobject.CiamUserIdentityDo;
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
    public int physicalDeleteByUserId(String userId) {
        return mapper.delete(
                new LambdaQueryWrapper<CiamUserIdentityDo>()
                        .eq(CiamUserIdentityDo::getUserId, userId));
    }
}
