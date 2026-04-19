package net.hwyz.iov.cloud.sec.ciam.service.common.audit;

import lombok.extern.slf4j.Slf4j;
import net.hwyz.iov.cloud.framework.common.util.DateTimeUtil;
import net.hwyz.iov.cloud.sec.ciam.service.domain.repository.CiamAuditLogRepository;
import net.hwyz.iov.cloud.sec.ciam.service.infrastructure.repository.dao.dataobject.CiamAuditLogDo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * 持久化审计日志记录实现。
 * <p>
 * 将审计事件持久化到数据库（ciam_audit_log），同时保留 SLF4J 日志输出。
 * 若数据库写入失败，降级为仅 SLF4J 记录，保证审计链路不中断业务流程。
 */
@Slf4j(topic = "AUDIT")
@Primary
@Component
public class PersistentAuditLogger implements AuditLogger {

    private final CiamAuditLogRepository auditLogRepository;
    private final AuditLogger delegate;

    @Autowired
    public PersistentAuditLogger(CiamAuditLogRepository auditLogRepository, Slf4jAuditLogger slf4jAuditLogger) {
        this.auditLogRepository = auditLogRepository;
        this.delegate = slf4jAuditLogger;
    }

    /**
     * 用于测试的构造函数，允许注入任意 AuditLogger 作为委托。
     */
    PersistentAuditLogger(CiamAuditLogRepository auditLogRepository, AuditLogger delegate) {
        this.auditLogRepository = auditLogRepository;
        this.delegate = delegate;
    }

    @Override
    public void log(AuditEvent event) {
        if (event == null) {
            return;
        }
        // 始终先写 SLF4J 日志
        delegate.log(event);

        // 持久化到数据库
        try {
            CiamAuditLogDo logDo = mapToDataObject(event);
            auditLogRepository.insert(logDo);
        } catch (Exception e) {
            log.warn("[AUDIT] 审计日志持久化失败，已降级为仅日志记录: {}", e.getMessage());
        }
    }

    /**
     * 将 AuditEvent 映射为 CiamAuditLogDo。
     */
    CiamAuditLogDo mapToDataObject(AuditEvent event) {
        CiamAuditLogDo logDo = new CiamAuditLogDo();
        logDo.setAuditId(generateAuditId());
        logDo.setUserId(event.getUserId());
        logDo.setSessionId(event.getSessionId());
        logDo.setClientId(event.getClientId());
        logDo.setClientType(event.getClientType());
        logDo.setEventType(event.getEventType());
        logDo.setEventName(event.getEventName());
        logDo.setOperationResult(event.isSuccess() ? 1 : 0);
        logDo.setRequestUri(event.getRequestUri());
        logDo.setRequestMethod(event.getRequestMethod());
        logDo.setResponseCode(event.getResponseCode());
        logDo.setIpAddress(event.getIp());
        logDo.setTraceId(event.getTraceId());
        logDo.setRequestSnapshot(event.getRequestSnapshot());
        logDo.setEventTime(event.getEventTime() != null ? event.getEventTime() : DateTimeUtil.getNowInstant());
        logDo.setCreateTime(DateTimeUtil.getNowInstant());
        logDo.setRowVersion(1);
        logDo.setRowValid(1);
        return logDo;
    }

    private String generateAuditId() {
        return UUID.randomUUID().toString().replace("-", "");
    }
}
