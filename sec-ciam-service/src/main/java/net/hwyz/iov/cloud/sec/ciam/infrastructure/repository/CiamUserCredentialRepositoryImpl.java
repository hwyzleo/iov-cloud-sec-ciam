package net.hwyz.iov.cloud.sec.ciam.infrastructure.repository;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import lombok.RequiredArgsConstructor;
import net.hwyz.iov.cloud.sec.ciam.domain.repository.CiamUserCredentialRepository;
import net.hwyz.iov.cloud.sec.ciam.infrastructure.repository.dao.CiamUserCredentialMapper;
import net.hwyz.iov.cloud.sec.ciam.infrastructure.repository.dao.dataobject.CiamUserCredentialDo;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class CiamUserCredentialRepositoryImpl implements CiamUserCredentialRepository {

    private final CiamUserCredentialMapper mapper;

    @Override
    public Optional<CiamUserCredentialDo> findByUserIdAndType(String userId, String credentialType) {
        return Optional.ofNullable(mapper.selectOne(
                new LambdaQueryWrapper<CiamUserCredentialDo>()
                        .eq(CiamUserCredentialDo::getUserId, userId)
                        .eq(CiamUserCredentialDo::getCredentialType, credentialType)
                        .eq(CiamUserCredentialDo::getRowValid, 1)));
    }

    @Override
    public Optional<CiamUserCredentialDo> findByCredentialId(String credentialId) {
        return Optional.ofNullable(mapper.selectOne(
                new LambdaQueryWrapper<CiamUserCredentialDo>()
                        .eq(CiamUserCredentialDo::getCredentialId, credentialId)));
    }

    @Override
    public int insert(CiamUserCredentialDo entity) {
        return mapper.insert(entity);
    }

    @Override
    public int updateByCredentialId(CiamUserCredentialDo entity) {
        return mapper.update(entity,
                new LambdaUpdateWrapper<CiamUserCredentialDo>()
                        .eq(CiamUserCredentialDo::getCredentialId, entity.getCredentialId()));
    }

    @Override
    public int physicalDeleteByUserId(String userId) {
        return mapper.delete(
                new LambdaQueryWrapper<CiamUserCredentialDo>()
                        .eq(CiamUserCredentialDo::getUserId, userId));
    }
}
