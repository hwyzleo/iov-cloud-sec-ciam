package net.hwyz.iov.cloud.sec.ciam.service.infrastructure.persistence.repository;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import lombok.RequiredArgsConstructor;
import net.hwyz.iov.cloud.sec.ciam.service.domain.model.UserConsent;
import net.hwyz.iov.cloud.sec.ciam.service.domain.repository.UserConsentRepository;
import net.hwyz.iov.cloud.sec.ciam.service.infrastructure.persistence.converter.UserConsentPoConverter;
import net.hwyz.iov.cloud.sec.ciam.service.infrastructure.persistence.mapper.CiamUserConsentMapper;
import net.hwyz.iov.cloud.sec.ciam.service.infrastructure.persistence.po.UserConsentPo;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Repository
@RequiredArgsConstructor
public class UserConsentRepositoryImpl implements UserConsentRepository {

    private final CiamUserConsentMapper mapper;

    @Override
    public Optional<UserConsent> findByConsentId(String consentId) {
        return Optional.ofNullable(mapper.selectOne(
                new LambdaQueryWrapper<UserConsentPo>()
                        .eq(UserConsentPo::getConsentId, consentId)
                        .eq(UserConsentPo::getRowValid, 1)))
                .map(UserConsentPoConverter.INSTANCE::toDomain);
    }

    @Override
    public List<UserConsent> findByUserId(String userId) {
        return mapper.selectList(
                new LambdaQueryWrapper<UserConsentPo>()
                        .eq(UserConsentPo::getUserId, userId)
                        .eq(UserConsentPo::getRowValid, 1))
                .stream()
                .map(UserConsentPoConverter.INSTANCE::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public List<UserConsent> findByUserIdAndConsentType(String userId, String consentType) {
        return mapper.selectList(
                new LambdaQueryWrapper<UserConsentPo>()
                        .eq(UserConsentPo::getUserId, userId)
                        .eq(UserConsentPo::getConsentType, consentType)
                        .eq(UserConsentPo::getRowValid, 1))
                .stream()
                .map(UserConsentPoConverter.INSTANCE::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public int insert(UserConsent entity) {
        return mapper.insert(UserConsentPoConverter.INSTANCE.toPo(entity));
    }

    @Override
    public int updateByConsentId(UserConsent entity) {
        UserConsentPo po = UserConsentPoConverter.INSTANCE.toPo(entity);
        return mapper.update(po,
                new LambdaUpdateWrapper<UserConsentPo>()
                        .eq(UserConsentPo::getConsentId, po.getConsentId()));
    }
}
