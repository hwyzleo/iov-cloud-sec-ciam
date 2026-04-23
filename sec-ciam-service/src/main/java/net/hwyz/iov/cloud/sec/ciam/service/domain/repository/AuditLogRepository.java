package net.hwyz.iov.cloud.sec.ciam.service.domain.repository;

import net.hwyz.iov.cloud.sec.ciam.service.domain.model.AuditLog;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * 审计日志表仓储接口。
 */
public interface AuditLogRepository {

    /** 根据业务 ID 查询 */
    Optional<AuditLog> findByAuditId(String auditId);

    /** 根据用户 ID 和时间范围查询 */
    List<AuditLog> findByUserIdAndTimeRange(String userId, LocalDateTime startTime, LocalDateTime endTime);

    /** 根据事件类型和时间范围查询 */
    List<AuditLog> findByEventTypeAndTimeRange(String eventType, LocalDateTime startTime, LocalDateTime endTime);

    /** 根据追踪标识查询 */
    List<AuditLog> findByTraceId(String traceId);

    /** 插入审计日志记录 */
    int insert(AuditLog entity);
}
