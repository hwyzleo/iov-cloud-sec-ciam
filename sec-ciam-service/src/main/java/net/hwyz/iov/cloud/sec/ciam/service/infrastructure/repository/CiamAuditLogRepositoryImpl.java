package net.hwyz.iov.cloud.sec.ciam.service.infrastructure.repository;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import net.hwyz.iov.cloud.sec.ciam.service.domain.repository.CiamAuditLogRepository;
import net.hwyz.iov.cloud.sec.ciam.service.infrastructure.repository.dao.CiamAuditLogMapper;
import net.hwyz.iov.cloud.sec.ciam.service.infrastructure.repository.dao.dataobject.AuditLogPo;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class CiamAuditLogRepositoryImpl implements CiamAuditLogRepository {

    private final CiamAuditLogMapper mapper;

    @Override
    public Optional<AuditLogPo> findByAuditId(String auditId) {
        return Optional.ofNullable(mapper.selectOne(
                new LambdaQueryWrapper<AuditLogPo>()
                        .eq(AuditLogPo::getAuditId, auditId)));
    }

    @Override
    public List<AuditLogPo> findByUserIdAndTimeRange(String userId, LocalDateTime startTime, LocalDateTime endTime) {
        return mapper.selectList(
                new LambdaQueryWrapper<AuditLogPo>()
                        .eq(AuditLogPo::getUserId, userId)
                        .ge(AuditLogPo::getEventTime, startTime)
                        .le(AuditLogPo::getEventTime, endTime)
                        .eq(AuditLogPo::getRowValid, 1));
    }

    @Override
    public List<AuditLogPo> findByEventTypeAndTimeRange(String eventType, LocalDateTime startTime, LocalDateTime endTime) {
        return mapper.selectList(
                new LambdaQueryWrapper<AuditLogPo>()
                        .eq(AuditLogPo::getEventType, eventType)
                        .ge(AuditLogPo::getEventTime, startTime)
                        .le(AuditLogPo::getEventTime, endTime)
                        .eq(AuditLogPo::getRowValid, 1));
    }

    @Override
    public List<AuditLogPo> findByTraceId(String traceId) {
        return mapper.selectList(
                new LambdaQueryWrapper<AuditLogPo>()
                        .eq(AuditLogPo::getTraceId, traceId)
                        .eq(AuditLogPo::getRowValid, 1));
    }

    @Override
    public int insert(AuditLogPo entity) {
        return mapper.insert(entity);
    }
}
