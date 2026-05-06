package net.hwyz.iov.cloud.sec.ciam.service.adapter.web.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

/**
 * 审计日志 VO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuditLogVo {

    private String auditId;

    private String userId;

    private String sessionId;

    private String clientId;

    private String clientType;

    private String eventType;

    private String eventName;

    private Integer operationResult;

    private String requestUri;

    private String requestMethod;

    private String responseCode;

    private String ipAddress;

    private String traceId;

    private String requestSnapshot;

    private Instant eventTime;

    private String description;
}