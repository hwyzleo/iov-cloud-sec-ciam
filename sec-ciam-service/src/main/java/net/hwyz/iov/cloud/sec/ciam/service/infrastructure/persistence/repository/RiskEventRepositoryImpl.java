package net.hwyz.iov.cloud.sec.ciam.service.infrastructure.persistence.repository;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import lombok.RequiredArgsConstructor;
import net.hwyz.iov.cloud.sec.ciam.service.domain.model.RiskEvent;
import net.hwyz.iov.cloud.sec.ciam.service.domain.repository.RiskEventRepository;
import net.hwyz.iov.cloud.sec.ciam.service.infrastructure.persistence.converter.RiskEventPoConverter;
import net.hwyz.iov.cloud.sec.ciam.service.infrastructure.persistence.mapper.CiamRiskEventMapper;
import net.hwyz.iov.cloud.sec.ciam.service.infrastructure.persistence.po.RiskEventPo;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Repository
@RequiredArgsConstructor
public class RiskEventRepositoryImpl implements RiskEventRepository {

    private final CiamRiskEventMapper mapper;

    @Override
    public Optional<RiskEvent> findByRiskEventId(String riskEventId) {
        return Optional.ofNullable(mapper.selectOne(
                new LambdaQueryWrapper<RiskEventPo>()
                        .eq(RiskEventPo::getRiskEventId, riskEventId)
                        .eq(RiskEventPo::getRowValid, 1)))
                .map(RiskEventPoConverter.INSTANCE::toDomain);
    }

    @Override
    public List<RiskEvent> findByUserIdAndTimeRange(String userId, LocalDateTime startTime, LocalDateTime endTime) {
        return mapper.selectList(
                new LambdaQueryWrapper<RiskEventPo>()
                        .eq(RiskEventPo::getUserId, userId)
                        .between(RiskEventPo::getEventTime, startTime, endTime)
                        .eq(RiskEventPo::getRowValid, 1))
                .stream()
                .map(RiskEventPoConverter.INSTANCE::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public List<RiskEvent> findByRiskLevel(int riskLevel) {
        return mapper.selectList(
                new LambdaQueryWrapper<RiskEventPo>()
                        .eq(RiskEventPo::getRiskLevel, riskLevel)
                        .eq(RiskEventPo::getRowValid, 1))
                .stream()
                .map(RiskEventPoConverter.INSTANCE::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public int insert(RiskEvent entity) {
        return mapper.insert(RiskEventPoConverter.INSTANCE.toPo(entity));
    }

    @Override
    public int updateByRiskEventId(RiskEvent entity) {
        RiskEventPo po = RiskEventPoConverter.INSTANCE.toPo(entity);
        return mapper.update(po,
                new LambdaUpdateWrapper<RiskEventPo>()
                        .eq(RiskEventPo::getRiskEventId, po.getRiskEventId()));
    }
}
