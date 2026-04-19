package net.hwyz.iov.cloud.sec.ciam.service.infrastructure.repository;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import lombok.RequiredArgsConstructor;
import net.hwyz.iov.cloud.sec.ciam.service.domain.repository.CiamUserConsentRepository;
import net.hwyz.iov.cloud.sec.ciam.service.infrastructure.repository.dao.CiamUserConsentMapper;
import net.hwyz.iov.cloud.sec.ciam.service.infrastructure.repository.dao.dataobject.CiamUserConsentDo;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class CiamUserConsentRepositoryImpl implements CiamUserConsentRepository {

    private final CiamUserConsentMapper mapper;

    @Override
    public Optional<CiamUserConsentDo> findByConsentId(String consentId) {
        return Optional.ofNullable(mapper.selectOne(
                new LambdaQueryWrapper<CiamUserConsentDo>()
                        .eq(CiamUserConsentDo::getConsentId, consentId)));
    }

    @Override
    public List<CiamUserConsentDo> findByUserId(String userId) {
        return mapper.selectList(
                new LambdaQueryWrapper<CiamUserConsentDo>()
                        .eq(CiamUserConsentDo::getUserId, userId)
                        .eq(CiamUserConsentDo::getRowValid, 1));
    }

    @Override
    public List<CiamUserConsentDo> findByUserIdAndConsentType(String userId, String consentType) {
        return mapper.selectList(
                new LambdaQueryWrapper<CiamUserConsentDo>()
                        .eq(CiamUserConsentDo::getUserId, userId)
                        .eq(CiamUserConsentDo::getConsentType, consentType)
                        .eq(CiamUserConsentDo::getRowValid, 1));
    }

    @Override
    public int insert(CiamUserConsentDo entity) {
        return mapper.insert(entity);
    }

    @Override
    public int updateByConsentId(CiamUserConsentDo entity) {
        return mapper.update(entity,
                new LambdaUpdateWrapper<CiamUserConsentDo>()
                        .eq(CiamUserConsentDo::getConsentId, entity.getConsentId()));
    }
}
