package net.hwyz.iov.cloud.sec.ciam.infrastructure.repository;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import lombok.RequiredArgsConstructor;
import net.hwyz.iov.cloud.sec.ciam.domain.repository.CiamRiskEventRepository;
import net.hwyz.iov.cloud.sec.ciam.infrastructure.repository.dao.CiamRiskEventMapper;
import net.hwyz.iov.cloud.sec.ciam.infrastructure.repository.dao.dataobject.CiamRiskEventDo;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class CiamRiskEventRepositoryImpl implements CiamRiskEventRepository {

    private final CiamRiskEventMapper mapper;

    @Override
    public Optional<CiamRiskEventDo> findByRiskEventId(String riskEventId) {
        return Optional.ofNullable(mapper.selectOne(
                new LambdaQueryWrapper<CiamRiskEventDo>()
                        .eq(CiamRiskEventDo::getRiskEventId, riskEventId)));
    }

    @Override
    public List<CiamRiskEventDo> findByUserIdAndTimeRange(String userId, LocalDateTime startTime, LocalDateTime endTime) {
        return mapper.selectList(
                new LambdaQueryWrapper<CiamRiskEventDo>()
                        .eq(CiamRiskEventDo::getUserId, userId)
                        .ge(CiamRiskEventDo::getEventTime, startTime)
                        .le(CiamRiskEventDo::getEventTime, endTime)
                        .eq(CiamRiskEventDo::getRowValid, 1));
    }

    @Override
    public List<CiamRiskEventDo> findByRiskLevel(int riskLevel) {
        return mapper.selectList(
                new LambdaQueryWrapper<CiamRiskEventDo>()
                        .eq(CiamRiskEventDo::getRiskLevel, riskLevel)
                        .eq(CiamRiskEventDo::getRowValid, 1));
    }

    @Override
    public int insert(CiamRiskEventDo entity) {
        return mapper.insert(entity);
    }

    @Override
    public int updateByRiskEventId(CiamRiskEventDo entity) {
        return mapper.update(entity,
                new LambdaUpdateWrapper<CiamRiskEventDo>()
                        .eq(CiamRiskEventDo::getRiskEventId, entity.getRiskEventId()));
    }
}
