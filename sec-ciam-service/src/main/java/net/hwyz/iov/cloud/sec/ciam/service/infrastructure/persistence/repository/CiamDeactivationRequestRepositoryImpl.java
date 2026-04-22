package net.hwyz.iov.cloud.sec.ciam.service.infrastructure.persistence.repository;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import lombok.RequiredArgsConstructor;
import net.hwyz.iov.cloud.sec.ciam.service.domain.repository.CiamDeactivationRequestRepository;
import net.hwyz.iov.cloud.sec.ciam.service.infrastructure.persistence.mapper.CiamDeactivationRequestMapper;
import net.hwyz.iov.cloud.sec.ciam.service.infrastructure.persistence.po.DeactivationRequestPo;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class CiamDeactivationRequestRepositoryImpl implements CiamDeactivationRequestRepository {

    private final CiamDeactivationRequestMapper mapper;

    @Override
    public Optional<DeactivationRequestPo> findByDeactivationRequestId(String deactivationRequestId) {
        return Optional.ofNullable(mapper.selectOne(
                new LambdaQueryWrapper<DeactivationRequestPo>()
                        .eq(DeactivationRequestPo::getDeactivationRequestId, deactivationRequestId)));
    }

    @Override
    public List<DeactivationRequestPo> findByUserIdAndReviewStatus(String userId, int reviewStatus) {
        return mapper.selectList(
                new LambdaQueryWrapper<DeactivationRequestPo>()
                        .eq(DeactivationRequestPo::getUserId, userId)
                        .eq(DeactivationRequestPo::getReviewStatus, reviewStatus)
                        .eq(DeactivationRequestPo::getRowValid, 1));
    }

    @Override
    public List<DeactivationRequestPo> findByReviewStatus(int reviewStatus) {
        return mapper.selectList(
                new LambdaQueryWrapper<DeactivationRequestPo>()
                        .eq(DeactivationRequestPo::getReviewStatus, reviewStatus)
                        .eq(DeactivationRequestPo::getRowValid, 1));
    }

    @Override
    public List<DeactivationRequestPo> findByExecuteStatus(int executeStatus) {
        return mapper.selectList(
                new LambdaQueryWrapper<DeactivationRequestPo>()
                        .eq(DeactivationRequestPo::getExecuteStatus, executeStatus)
                        .eq(DeactivationRequestPo::getRowValid, 1));
    }

    @Override
    public int insert(DeactivationRequestPo entity) {
        return mapper.insert(entity);
    }

    @Override
    public int updateByDeactivationRequestId(DeactivationRequestPo entity) {
        return mapper.update(entity,
                new LambdaUpdateWrapper<DeactivationRequestPo>()
                        .eq(DeactivationRequestPo::getDeactivationRequestId, entity.getDeactivationRequestId()));
    }
}
