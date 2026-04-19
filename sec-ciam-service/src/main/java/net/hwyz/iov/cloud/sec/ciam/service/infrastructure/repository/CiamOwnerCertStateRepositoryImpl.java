package net.hwyz.iov.cloud.sec.ciam.service.infrastructure.repository;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import lombok.RequiredArgsConstructor;
import net.hwyz.iov.cloud.sec.ciam.service.domain.repository.CiamOwnerCertStateRepository;
import net.hwyz.iov.cloud.sec.ciam.service.infrastructure.repository.dao.CiamOwnerCertStateMapper;
import net.hwyz.iov.cloud.sec.ciam.service.infrastructure.repository.dao.dataobject.CiamOwnerCertStateDo;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class CiamOwnerCertStateRepositoryImpl implements CiamOwnerCertStateRepository {

    private final CiamOwnerCertStateMapper mapper;

    @Override
    public Optional<CiamOwnerCertStateDo> findByOwnerCertId(String ownerCertId) {
        return Optional.ofNullable(mapper.selectOne(
                new LambdaQueryWrapper<CiamOwnerCertStateDo>()
                        .eq(CiamOwnerCertStateDo::getOwnerCertId, ownerCertId)));
    }

    @Override
    public List<CiamOwnerCertStateDo> findByUserIdAndCertStatus(String userId, int certStatus) {
        return mapper.selectList(
                new LambdaQueryWrapper<CiamOwnerCertStateDo>()
                        .eq(CiamOwnerCertStateDo::getUserId, userId)
                        .eq(CiamOwnerCertStateDo::getCertStatus, certStatus)
                        .eq(CiamOwnerCertStateDo::getRowValid, 1));
    }

    @Override
    public List<CiamOwnerCertStateDo> findByUserId(String userId) {
        return mapper.selectList(
                new LambdaQueryWrapper<CiamOwnerCertStateDo>()
                        .eq(CiamOwnerCertStateDo::getUserId, userId)
                        .eq(CiamOwnerCertStateDo::getRowValid, 1));
    }

    @Override
    public int insert(CiamOwnerCertStateDo entity) {
        return mapper.insert(entity);
    }

    @Override
    public int updateByOwnerCertId(CiamOwnerCertStateDo entity) {
        return mapper.update(entity,
                new LambdaUpdateWrapper<CiamOwnerCertStateDo>()
                        .eq(CiamOwnerCertStateDo::getOwnerCertId, entity.getOwnerCertId()));
    }
}
