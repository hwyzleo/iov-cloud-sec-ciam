package net.hwyz.iov.cloud.sec.ciam.service.infrastructure.persistence.repository;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import lombok.RequiredArgsConstructor;
import net.hwyz.iov.cloud.sec.ciam.service.domain.repository.OwnerCertStateRepository;
import net.hwyz.iov.cloud.sec.ciam.service.infrastructure.persistence.mapper.CiamOwnerCertStateMapper;
import net.hwyz.iov.cloud.sec.ciam.service.infrastructure.persistence.po.OwnerCertStatePo;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class OwnerCertStateRepositoryImpl implements OwnerCertStateRepository {

    private final CiamOwnerCertStateMapper mapper;

    @Override
    public Optional<OwnerCertStatePo> findByOwnerCertId(String ownerCertId) {
        return Optional.ofNullable(mapper.selectOne(
                new LambdaQueryWrapper<OwnerCertStatePo>()
                        .eq(OwnerCertStatePo::getOwnerCertId, ownerCertId)));
    }

    @Override
    public List<OwnerCertStatePo> findByUserIdAndCertStatus(String userId, int certStatus) {
        return mapper.selectList(
                new LambdaQueryWrapper<OwnerCertStatePo>()
                        .eq(OwnerCertStatePo::getUserId, userId)
                        .eq(OwnerCertStatePo::getCertStatus, certStatus)
                        .eq(OwnerCertStatePo::getRowValid, 1));
    }

    @Override
    public List<OwnerCertStatePo> findByUserId(String userId) {
        return mapper.selectList(
                new LambdaQueryWrapper<OwnerCertStatePo>()
                        .eq(OwnerCertStatePo::getUserId, userId)
                        .eq(OwnerCertStatePo::getRowValid, 1));
    }

    @Override
    public int insert(OwnerCertStatePo entity) {
        return mapper.insert(entity);
    }

    @Override
    public int updateByOwnerCertId(OwnerCertStatePo entity) {
        return mapper.update(entity,
                new LambdaUpdateWrapper<OwnerCertStatePo>()
                        .eq(OwnerCertStatePo::getOwnerCertId, entity.getOwnerCertId()));
    }
}
