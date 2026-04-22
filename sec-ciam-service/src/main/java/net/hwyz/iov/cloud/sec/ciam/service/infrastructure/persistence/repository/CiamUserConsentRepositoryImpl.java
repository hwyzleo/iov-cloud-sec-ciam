package net.hwyz.iov.cloud.sec.ciam.service.infrastructure.persistence.repository;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import lombok.RequiredArgsConstructor;
import net.hwyz.iov.cloud.sec.ciam.service.domain.repository.CiamUserConsentRepository;
import net.hwyz.iov.cloud.sec.ciam.service.infrastructure.persistence.mapper.CiamUserConsentMapper;
import net.hwyz.iov.cloud.sec.ciam.service.infrastructure.persistence.po.UserConsentPo;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class CiamUserConsentRepositoryImpl implements CiamUserConsentRepository {

    private final CiamUserConsentMapper mapper;

    @Override
    public Optional<UserConsentPo> findByConsentId(String consentId) {
        return Optional.ofNullable(mapper.selectOne(
                new LambdaQueryWrapper<UserConsentPo>()
                        .eq(UserConsentPo::getConsentId, consentId)));
    }

    @Override
    public List<UserConsentPo> findByUserId(String userId) {
        return mapper.selectList(
                new LambdaQueryWrapper<UserConsentPo>()
                        .eq(UserConsentPo::getUserId, userId)
                        .eq(UserConsentPo::getRowValid, 1));
    }

    @Override
    public List<UserConsentPo> findByUserIdAndConsentType(String userId, String consentType) {
        return mapper.selectList(
                new LambdaQueryWrapper<UserConsentPo>()
                        .eq(UserConsentPo::getUserId, userId)
                        .eq(UserConsentPo::getConsentType, consentType)
                        .eq(UserConsentPo::getRowValid, 1));
    }

    @Override
    public int insert(UserConsentPo entity) {
        return mapper.insert(entity);
    }

    @Override
    public int updateByConsentId(UserConsentPo entity) {
        return mapper.update(entity,
                new LambdaUpdateWrapper<UserConsentPo>()
                        .eq(UserConsentPo::getConsentId, entity.getConsentId()));
    }
}
