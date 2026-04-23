package net.hwyz.iov.cloud.sec.ciam.service.infrastructure.persistence.repository;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import net.hwyz.iov.cloud.sec.ciam.service.domain.model.AuditLog;
import net.hwyz.iov.cloud.sec.ciam.service.domain.repository.AuditLogRepository;
import net.hwyz.iov.cloud.sec.ciam.service.infrastructure.persistence.converter.AuditLogPoConverter;
import net.hwyz.iov.cloud.sec.ciam.service.infrastructure.persistence.mapper.CiamAuditLogMapper;
import net.hwyz.iov.cloud.sec.ciam.service.infrastructure.persistence.po.AuditLogPo;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Repository
@RequiredArgsConstructor
public class AuditLogRepositoryImpl implements AuditLogRepository {

    private final CiamAuditLogMapper mapper;
    private final AuditLogPoConverter poConverter;

    @Override
    public Optional<AuditLog> findByAuditId(String auditId) {
        AuditLogPo po = mapper.selectOne(
                new LambdaQueryWrapper<AuditLogPo>()
                        .eq(AuditLogPo::getAuditId, auditId));
        return Optional.ofNullable(poConverter.toDomain(po));
    }

    @Override
    public List<AuditLog> findByUserIdAndTimeRange(String userId, LocalDateTime startTime, LocalDateTime endTime) {
        return mapper.selectList(
                new LambdaQueryWrapper<AuditLogPo>()
                        .eq(AuditLogPo::getUserId, userId)
                        .ge(AuditLogPo::getEventTime, startTime)
                        .le(AuditLogPo::getEventTime, endTime)
                        .eq(AuditLogPo::getRowValid, 1))
                .stream().map(poConverter::toDomain).collect(Collectors.toList());
    }

    @Override
    public List<AuditLog> findByEventTypeAndTimeRange(String eventType, LocalDateTime startTime, LocalDateTime endTime) {
        return mapper.selectList(
                new LambdaQueryWrapper<AuditLogPo>()
                        .eq(AuditLogPo::getEventType, eventType)
                        .ge(AuditLogPo::getEventTime, startTime)
                        .le(AuditLogPo::getEventTime, endTime)
                        .eq(AuditLogPo::getRowValid, 1))
                .stream().map(poConverter::toDomain).collect(Collectors.toList());
    }

    @Override
    public List<AuditLog> findByTraceId(String traceId) {
        return mapper.selectList(
                new LambdaQueryWrapper<AuditLogPo>()
                        .eq(AuditLogPo::getTraceId, traceId)
                        .eq(AuditLogPo::getRowValid, 1))
                .stream().map(poConverter::toDomain).collect(Collectors.toList());
    }

    @Override
    public int insert(AuditLog entity) {
        return mapper.insert(poConverter.toPo(entity));
    }
}
