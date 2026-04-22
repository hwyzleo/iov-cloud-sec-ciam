package net.hwyz.iov.cloud.sec.ciam.service.domain.repository;

import net.hwyz.iov.cloud.sec.ciam.service.infrastructure.repository.dao.dataobject.AuditLogPo;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * 审计日志表仓储接口。
 */
public interface CiamAuditLogRepository {

    /** 根据业务 ID 查询 */
    Optional<AuditLogPo> findByAuditId(String auditId);

    /** 根据用户 ID 和时间范围查询 */
    List<AuditLogPo> findByUserIdAndTimeRange(String userId, LocalDateTime startTime, LocalDateTime endTime);

    /** 根据事件类型和时间范围查询 */
    List<AuditLogPo> findByEventTypeAndTimeRange(String eventType, LocalDateTime startTime, LocalDateTime endTime);

    /** 根据追踪标识查询 */
    List<AuditLogPo> findByTraceId(String traceId);

    /** 插入审计日志记录 */
    int insert(AuditLogPo entity);
}
