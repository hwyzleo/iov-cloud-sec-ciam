package net.hwyz.iov.cloud.sec.ciam.service.infrastructure.persistence.repository;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import lombok.RequiredArgsConstructor;
import net.hwyz.iov.cloud.sec.ciam.service.domain.model.UserCredential;
import net.hwyz.iov.cloud.sec.ciam.service.domain.repository.UserCredentialRepository;
import net.hwyz.iov.cloud.sec.ciam.service.infrastructure.persistence.converter.UserCredentialPoConverter;
import net.hwyz.iov.cloud.sec.ciam.service.infrastructure.persistence.mapper.CiamUserCredentialMapper;
import net.hwyz.iov.cloud.sec.ciam.service.infrastructure.persistence.po.UserCredentialPo;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class UserCredentialRepositoryImpl implements UserCredentialRepository {

    private final CiamUserCredentialMapper mapper;

    @Override
    public Optional<UserCredential> findByUserIdAndType(String userId, String credentialType) {
        return Optional.ofNullable(mapper.selectOne(
                new LambdaQueryWrapper<UserCredentialPo>()
                        .eq(UserCredentialPo::getUserId, userId)
                        .eq(UserCredentialPo::getCredentialType, credentialType)
                        .eq(UserCredentialPo::getRowValid, 1)))
                .map(UserCredentialPoConverter.INSTANCE::toDomain);
    }

    @Override
    public Optional<UserCredential> findByCredentialId(String credentialId) {
        return Optional.ofNullable(mapper.selectOne(
                new LambdaQueryWrapper<UserCredentialPo>()
                        .eq(UserCredentialPo::getCredentialId, credentialId)
                        .eq(UserCredentialPo::getRowValid, 1)))
                .map(UserCredentialPoConverter.INSTANCE::toDomain);
    }

    @Override
    public int insert(UserCredential entity) {
        return mapper.insert(UserCredentialPoConverter.INSTANCE.toPo(entity));
    }

    @Override
    public int updateByCredentialId(UserCredential entity) {
        UserCredentialPo po = UserCredentialPoConverter.INSTANCE.toPo(entity);
        return mapper.update(po,
                new LambdaUpdateWrapper<UserCredentialPo>()
                        .eq(UserCredentialPo::getCredentialId, po.getCredentialId()));
    }

    @Override
    public int physicalDeleteByUserId(String userId) {
        return mapper.delete(
                new LambdaQueryWrapper<UserCredentialPo>()
                        .eq(UserCredentialPo::getUserId, userId));
    }
}
