package net.hwyz.iov.cloud.sec.ciam.infrastructure.repository;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import net.hwyz.iov.cloud.sec.ciam.domain.repository.CiamAuditLogRepository;
import net.hwyz.iov.cloud.sec.ciam.infrastructure.repository.dao.CiamAuditLogMapper;
import net.hwyz.iov.cloud.sec.ciam.infrastructure.repository.dao.dataobject.CiamAuditLogDo;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class CiamAuditLogRepositoryImpl implements CiamAuditLogRepository {

    private final CiamAuditLogMapper mapper;

    @Override
    public Optional<CiamAuditLogDo> findByAuditId(String auditId) {
        return Optional.ofNullable(mapper.selectOne(
                new LambdaQueryWrapper<CiamAuditLogDo>()
                        .eq(CiamAuditLogDo::getAuditId, auditId)));
    }

    @Override
    public List<CiamAuditLogDo> findByUserIdAndTimeRange(String userId, LocalDateTime startTime, LocalDateTime endTime) {
        return mapper.selectList(
                new LambdaQueryWrapper<CiamAuditLogDo>()
                        .eq(CiamAuditLogDo::getUserId, userId)
                        .ge(CiamAuditLogDo::getEventTime, startTime)
                        .le(CiamAuditLogDo::getEventTime, endTime)
                        .eq(CiamAuditLogDo::getRowValid, 1));
    }

    @Override
    public List<CiamAuditLogDo> findByEventTypeAndTimeRange(String eventType, LocalDateTime startTime, LocalDateTime endTime) {
        return mapper.selectList(
                new LambdaQueryWrapper<CiamAuditLogDo>()
                        .eq(CiamAuditLogDo::getEventType, eventType)
                        .ge(CiamAuditLogDo::getEventTime, startTime)
                        .le(CiamAuditLogDo::getEventTime, endTime)
                        .eq(CiamAuditLogDo::getRowValid, 1));
    }

    @Override
    public List<CiamAuditLogDo> findByTraceId(String traceId) {
        return mapper.selectList(
                new LambdaQueryWrapper<CiamAuditLogDo>()
                        .eq(CiamAuditLogDo::getTraceId, traceId)
                        .eq(CiamAuditLogDo::getRowValid, 1));
    }

    @Override
    public int insert(CiamAuditLogDo entity) {
        return mapper.insert(entity);
    }
}
