package net.hwyz.iov.cloud.sec.ciam.service.common.audit;

import lombok.extern.slf4j.Slf4j;
import net.hwyz.iov.cloud.framework.common.util.DateTimeUtil;
import org.springframework.stereotype.Component;

/**
 * 基于 Slf4j 的审计日志记录实现。
 * <p>
 * 将审计事件以结构化格式写入独立的 AUDIT logger，
 * 便于通过 logback 配置将审计日志路由到独立文件或 Kafka。
 */
@Slf4j(topic = "AUDIT")
@Component
public class Slf4jAuditLogger implements AuditLogger {

    @Override
    public void log(AuditEvent event) {
        if (event == null) {
            return;
        }
        log.info("[AUDIT] type={} name={} result={} userId={} sessionId={} clientId={} clientType={} ip={} traceId={} uri={} method={} responseCode={} time={}",
                event.getEventType(),
                event.getEventName(),
                event.isSuccess() ? "SUCCESS" : "FAIL",
                event.getUserId(),
                event.getSessionId(),
                event.getClientId(),
                event.getClientType(),
                event.getIp(),
                event.getTraceId(),
                event.getRequestUri(),
                event.getRequestMethod(),
                event.getResponseCode(),
                event.getEventTime() != null ? DateTimeUtil.format(event.getEventTime()) : null);
    }
}
