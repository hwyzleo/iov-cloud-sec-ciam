package net.hwyz.iov.cloud.sec.ciam.service.infrastructure.persistence.repository;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import net.hwyz.iov.cloud.sec.ciam.service.domain.model.MergeRequest;
import net.hwyz.iov.cloud.sec.ciam.service.domain.repository.CiamMergeRequestRepository;
import net.hwyz.iov.cloud.sec.ciam.service.infrastructure.persistence.converter.MergeRequestPoConverter;
import net.hwyz.iov.cloud.sec.ciam.service.infrastructure.persistence.mapper.CiamMergeRequestMapper;
import net.hwyz.iov.cloud.sec.ciam.service.infrastructure.persistence.po.MergeRequestPo;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Repository
@RequiredArgsConstructor
public class CiamMergeRequestRepositoryImpl implements CiamMergeRequestRepository {

    private final CiamMergeRequestMapper mapper;

    @Override
    public Optional<MergeRequest> findByMergeRequestId(String mergeRequestId) {
        return Optional.ofNullable(mapper.selectOne(new LambdaQueryWrapper<MergeRequestPo>()
                .eq(MergeRequestPo::getMergeRequestId, mergeRequestId)))
                .map(MergeRequestPoConverter.INSTANCE::toDomain);
    }

    @Override
    public List<MergeRequest> findByReviewStatus(int reviewStatus) {
        return mapper.selectList(new LambdaQueryWrapper<MergeRequestPo>()
                .eq(MergeRequestPo::getReviewStatus, reviewStatus))
                .stream()
                .map(MergeRequestPoConverter.INSTANCE::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public int insert(MergeRequest entity) {
        return mapper.insert(MergeRequestPoConverter.INSTANCE.toPo(entity));
    }

    @Override
    public int updateByMergeRequestId(MergeRequest entity) {
        MergeRequestPo po = MergeRequestPoConverter.INSTANCE.toPo(entity);
        return mapper.update(po, new LambdaQueryWrapper<MergeRequestPo>()
                .eq(MergeRequestPo::getMergeRequestId, po.getMergeRequestId()));
    }
}
