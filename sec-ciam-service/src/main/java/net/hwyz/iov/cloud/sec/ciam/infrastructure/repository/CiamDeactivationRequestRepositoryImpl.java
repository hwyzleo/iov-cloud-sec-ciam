package net.hwyz.iov.cloud.sec.ciam.infrastructure.repository;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import lombok.RequiredArgsConstructor;
import net.hwyz.iov.cloud.sec.ciam.domain.repository.CiamDeactivationRequestRepository;
import net.hwyz.iov.cloud.sec.ciam.infrastructure.repository.dao.CiamDeactivationRequestMapper;
import net.hwyz.iov.cloud.sec.ciam.infrastructure.repository.dao.dataobject.CiamDeactivationRequestDo;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class CiamDeactivationRequestRepositoryImpl implements CiamDeactivationRequestRepository {

    private final CiamDeactivationRequestMapper mapper;

    @Override
    public Optional<CiamDeactivationRequestDo> findByDeactivationRequestId(String deactivationRequestId) {
        return Optional.ofNullable(mapper.selectOne(
                new LambdaQueryWrapper<CiamDeactivationRequestDo>()
                        .eq(CiamDeactivationRequestDo::getDeactivationRequestId, deactivationRequestId)));
    }

    @Override
    public List<CiamDeactivationRequestDo> findByUserIdAndReviewStatus(String userId, int reviewStatus) {
        return mapper.selectList(
                new LambdaQueryWrapper<CiamDeactivationRequestDo>()
                        .eq(CiamDeactivationRequestDo::getUserId, userId)
                        .eq(CiamDeactivationRequestDo::getReviewStatus, reviewStatus)
                        .eq(CiamDeactivationRequestDo::getRowValid, 1));
    }

    @Override
    public List<CiamDeactivationRequestDo> findByReviewStatus(int reviewStatus) {
        return mapper.selectList(
                new LambdaQueryWrapper<CiamDeactivationRequestDo>()
                        .eq(CiamDeactivationRequestDo::getReviewStatus, reviewStatus)
                        .eq(CiamDeactivationRequestDo::getRowValid, 1));
    }

    @Override
    public List<CiamDeactivationRequestDo> findByExecuteStatus(int executeStatus) {
        return mapper.selectList(
                new LambdaQueryWrapper<CiamDeactivationRequestDo>()
                        .eq(CiamDeactivationRequestDo::getExecuteStatus, executeStatus)
                        .eq(CiamDeactivationRequestDo::getRowValid, 1));
    }

    @Override
    public int insert(CiamDeactivationRequestDo entity) {
        return mapper.insert(entity);
    }

    @Override
    public int updateByDeactivationRequestId(CiamDeactivationRequestDo entity) {
        return mapper.update(entity,
                new LambdaUpdateWrapper<CiamDeactivationRequestDo>()
                        .eq(CiamDeactivationRequestDo::getDeactivationRequestId, entity.getDeactivationRequestId()));
    }
}
