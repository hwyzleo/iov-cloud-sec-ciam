package net.hwyz.iov.cloud.sec.ciam.infrastructure.repository;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import lombok.RequiredArgsConstructor;
import net.hwyz.iov.cloud.sec.ciam.domain.repository.CiamMergeRequestRepository;
import net.hwyz.iov.cloud.sec.ciam.infrastructure.repository.dao.CiamMergeRequestMapper;
import net.hwyz.iov.cloud.sec.ciam.infrastructure.repository.dao.dataobject.CiamMergeRequestDo;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class CiamMergeRequestRepositoryImpl implements CiamMergeRequestRepository {

    private final CiamMergeRequestMapper mapper;

    @Override
    public Optional<CiamMergeRequestDo> findByMergeRequestId(String mergeRequestId) {
        return Optional.ofNullable(mapper.selectOne(
                new LambdaQueryWrapper<CiamMergeRequestDo>()
                        .eq(CiamMergeRequestDo::getMergeRequestId, mergeRequestId)));
    }

    @Override
    public List<CiamMergeRequestDo> findByReviewStatus(int reviewStatus) {
        return mapper.selectList(
                new LambdaQueryWrapper<CiamMergeRequestDo>()
                        .eq(CiamMergeRequestDo::getReviewStatus, reviewStatus)
                        .eq(CiamMergeRequestDo::getRowValid, 1));
    }

    @Override
    public List<CiamMergeRequestDo> findBySourceUserId(String sourceUserId) {
        return mapper.selectList(
                new LambdaQueryWrapper<CiamMergeRequestDo>()
                        .eq(CiamMergeRequestDo::getSourceUserId, sourceUserId)
                        .eq(CiamMergeRequestDo::getRowValid, 1));
    }

    @Override
    public List<CiamMergeRequestDo> findByTargetUserId(String targetUserId) {
        return mapper.selectList(
                new LambdaQueryWrapper<CiamMergeRequestDo>()
                        .eq(CiamMergeRequestDo::getTargetUserId, targetUserId)
                        .eq(CiamMergeRequestDo::getRowValid, 1));
    }

    @Override
    public int insert(CiamMergeRequestDo entity) {
        return mapper.insert(entity);
    }

    @Override
    public int updateByMergeRequestId(CiamMergeRequestDo entity) {
        return mapper.update(entity,
                new LambdaUpdateWrapper<CiamMergeRequestDo>()
                        .eq(CiamMergeRequestDo::getMergeRequestId, entity.getMergeRequestId()));
    }
}
