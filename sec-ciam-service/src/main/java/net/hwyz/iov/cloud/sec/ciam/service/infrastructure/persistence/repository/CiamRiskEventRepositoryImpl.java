package net.hwyz.iov.cloud.sec.ciam.service.infrastructure.persistence.repository;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import lombok.RequiredArgsConstructor;
import net.hwyz.iov.cloud.sec.ciam.service.domain.repository.CiamRiskEventRepository;
import net.hwyz.iov.cloud.sec.ciam.service.infrastructure.persistence.mapper.CiamRiskEventMapper;
import net.hwyz.iov.cloud.sec.ciam.service.infrastructure.persistence.po.RiskEventPo;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class CiamRiskEventRepositoryImpl implements CiamRiskEventRepository {

    private final CiamRiskEventMapper mapper;

    @Override
    public Optional<RiskEventPo> findByRiskEventId(String riskEventId) {
        return Optional.ofNullable(mapper.selectOne(
                new LambdaQueryWrapper<RiskEventPo>()
                        .eq(RiskEventPo::getRiskEventId, riskEventId)));
    }

    @Override
    public List<RiskEventPo> findByUserIdAndTimeRange(String userId, LocalDateTime startTime, LocalDateTime endTime) {
        return mapper.selectList(
                new LambdaQueryWrapper<RiskEventPo>()
                        .eq(RiskEventPo::getUserId, userId)
                        .ge(RiskEventPo::getEventTime, startTime)
                        .le(RiskEventPo::getEventTime, endTime)
                        .eq(RiskEventPo::getRowValid, 1));
    }

    @Override
    public List<RiskEventPo> findByRiskLevel(int riskLevel) {
        return mapper.selectList(
                new LambdaQueryWrapper<RiskEventPo>()
                        .eq(RiskEventPo::getRiskLevel, riskLevel)
                        .eq(RiskEventPo::getRowValid, 1));
    }

    @Override
    public int insert(RiskEventPo entity) {
        return mapper.insert(entity);
    }

    @Override
    public int updateByRiskEventId(RiskEventPo entity) {
        return mapper.update(entity,
                new LambdaUpdateWrapper<RiskEventPo>()
                        .eq(RiskEventPo::getRiskEventId, entity.getRiskEventId()));
    }
}
