package net.hwyz.iov.cloud.sec.ciam.infrastructure.search.document;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;

/**
 * 审计日志检索索引文档。
 * <p>
 * 对应 Elasticsearch 中的审计日志索引，可通过 MySQL 或 Kafka 异步写入。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuditLogSearchDocument {

    /** 审计日志业务唯一标识 */
    private String auditId;

    /** 用户业务唯一标识 */
    private String userId;

    /** 事件类型 */
    private String eventType;

    /** 事件名称 */
    private String eventName;

    /** 操作结果：1-成功，0-失败 */
    private Integer operationResult;

    /** 请求 IP 地址 */
    private String ipAddress;

    /** 事件时间 */
    private OffsetDateTime eventTime;
}
