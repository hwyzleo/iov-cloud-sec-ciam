package net.hwyz.iov.cloud.sec.ciam.service.infrastructure.persistence.repository;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import net.hwyz.iov.cloud.sec.ciam.service.domain.model.DeactivationRequest;
import net.hwyz.iov.cloud.sec.ciam.service.domain.repository.DeactivationRequestRepository;
import net.hwyz.iov.cloud.sec.ciam.service.infrastructure.persistence.converter.DeactivationRequestPoConverter;
import net.hwyz.iov.cloud.sec.ciam.service.infrastructure.persistence.mapper.CiamDeactivationRequestMapper;
import net.hwyz.iov.cloud.sec.ciam.service.infrastructure.persistence.po.DeactivationRequestPo;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Repository
@RequiredArgsConstructor
public class DeactivationRequestRepositoryImpl implements DeactivationRequestRepository {

    private final CiamDeactivationRequestMapper mapper;

    @Override
    public Optional<DeactivationRequest> findByDeactivationRequestId(String deactivationRequestId) {
        return Optional.ofNullable(mapper.selectOne(new LambdaQueryWrapper<DeactivationRequestPo>()
                .eq(DeactivationRequestPo::getDeactivationRequestId, deactivationRequestId)))
                .map(DeactivationRequestPoConverter.INSTANCE::toDomain);
    }

    @Override
    public List<DeactivationRequest> findByReviewStatus(int reviewStatus) {
        return mapper.selectList(new LambdaQueryWrapper<DeactivationRequestPo>()
                .eq(DeactivationRequestPo::getReviewStatus, reviewStatus))
                .stream()
                .map(DeactivationRequestPoConverter.INSTANCE::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public int insert(DeactivationRequest entity) {
        return mapper.insert(DeactivationRequestPoConverter.INSTANCE.toPo(entity));
    }

    @Override
    public int updateByDeactivationRequestId(DeactivationRequest entity) {
        DeactivationRequestPo po = DeactivationRequestPoConverter.INSTANCE.toPo(entity);
        return mapper.update(po, new LambdaQueryWrapper<DeactivationRequestPo>()
                .eq(DeactivationRequestPo::getDeactivationRequestId, po.getDeactivationRequestId()));
    }
}
