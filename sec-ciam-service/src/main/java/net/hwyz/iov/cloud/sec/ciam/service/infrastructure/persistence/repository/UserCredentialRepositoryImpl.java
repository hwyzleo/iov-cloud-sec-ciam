package net.hwyz.iov.cloud.sec.ciam.service.infrastructure.persistence.repository;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import lombok.RequiredArgsConstructor;
import net.hwyz.iov.cloud.sec.ciam.service.domain.repository.UserCredentialRepository;
import net.hwyz.iov.cloud.sec.ciam.service.infrastructure.persistence.mapper.CiamUserCredentialMapper;
import net.hwyz.iov.cloud.sec.ciam.service.infrastructure.persistence.po.UserCredentialPo;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class UserCredentialRepositoryImpl implements UserCredentialRepository {

    private final CiamUserCredentialMapper mapper;

    @Override
    public Optional<UserCredentialPo> findByUserIdAndType(String userId, String credentialType) {
        return Optional.ofNullable(mapper.selectOne(
                new LambdaQueryWrapper<UserCredentialPo>()
                        .eq(UserCredentialPo::getUserId, userId)
                        .eq(UserCredentialPo::getCredentialType, credentialType)
                        .eq(UserCredentialPo::getRowValid, 1)));
    }

    @Override
    public Optional<UserCredentialPo> findByCredentialId(String credentialId) {
        return Optional.ofNullable(mapper.selectOne(
                new LambdaQueryWrapper<UserCredentialPo>()
                        .eq(UserCredentialPo::getCredentialId, credentialId)));
    }

    @Override
    public int insert(UserCredentialPo entity) {
        return mapper.insert(entity);
    }

    @Override
    public int updateByCredentialId(UserCredentialPo entity) {
        return mapper.update(entity,
                new LambdaUpdateWrapper<UserCredentialPo>()
                        .eq(UserCredentialPo::getCredentialId, entity.getCredentialId()));
    }

    @Override
    public int physicalDeleteByUserId(String userId) {
        return mapper.delete(
                new LambdaQueryWrapper<UserCredentialPo>()
                        .eq(UserCredentialPo::getUserId, userId));
    }
}
