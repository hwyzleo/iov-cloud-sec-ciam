package net.hwyz.iov.cloud.sec.ciam.service.infrastructure.persistence.repository;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import lombok.RequiredArgsConstructor;
import net.hwyz.iov.cloud.sec.ciam.service.domain.repository.CiamMergeRequestRepository;
import net.hwyz.iov.cloud.sec.ciam.service.infrastructure.persistence.mapper.CiamMergeRequestMapper;
import net.hwyz.iov.cloud.sec.ciam.service.infrastructure.persistence.po.MergeRequestPo;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class CiamMergeRequestRepositoryImpl implements CiamMergeRequestRepository {

    private final CiamMergeRequestMapper mapper;

    @Override
    public Optional<MergeRequestPo> findByMergeRequestId(String mergeRequestId) {
        return Optional.ofNullable(mapper.selectOne(
                new LambdaQueryWrapper<MergeRequestPo>()
                        .eq(MergeRequestPo::getMergeRequestId, mergeRequestId)));
    }

    @Override
    public List<MergeRequestPo> findByReviewStatus(int reviewStatus) {
        return mapper.selectList(
                new LambdaQueryWrapper<MergeRequestPo>()
                        .eq(MergeRequestPo::getReviewStatus, reviewStatus)
                        .eq(MergeRequestPo::getRowValid, 1));
    }

    @Override
    public List<MergeRequestPo> findBySourceUserId(String sourceUserId) {
        return mapper.selectList(
                new LambdaQueryWrapper<MergeRequestPo>()
                        .eq(MergeRequestPo::getSourceUserId, sourceUserId)
                        .eq(MergeRequestPo::getRowValid, 1));
    }

    @Override
    public List<MergeRequestPo> findByTargetUserId(String targetUserId) {
        return mapper.selectList(
                new LambdaQueryWrapper<MergeRequestPo>()
                        .eq(MergeRequestPo::getTargetUserId, targetUserId)
                        .eq(MergeRequestPo::getRowValid, 1));
    }

    @Override
    public int insert(MergeRequestPo entity) {
        return mapper.insert(entity);
    }

    @Override
    public int updateByMergeRequestId(MergeRequestPo entity) {
        return mapper.update(entity,
                new LambdaUpdateWrapper<MergeRequestPo>()
                        .eq(MergeRequestPo::getMergeRequestId, entity.getMergeRequestId()));
    }
}
