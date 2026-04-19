package net.hwyz.iov.cloud.sec.ciam.service.common.audit;

/**
 * 审计日志记录接口。
 * <p>
 * 定义审计事件的统一记录入口，具体实现可写入数据库、Kafka 或 Elasticsearch。
 */
public interface AuditLogger {

    /**
     * 记录审计事件。
     *
     * @param event 审计事件
     */
    void log(AuditEvent event);
}
