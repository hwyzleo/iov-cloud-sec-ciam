package net.hwyz.iov.cloud.sec.ciam.service.adapter.web.vo;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonFormat;
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

    @JsonProperty("audit_id")
    private String auditId;

    @JsonProperty("user_id")
    private String userId;

    @JsonProperty("session_id")
    private String sessionId;

    @JsonProperty("client_id")
    private String clientId;

    @JsonProperty("client_type")
    private String clientType;

    @JsonProperty("event_type")
    private String eventType;

    @JsonProperty("event_name")
    private String eventName;

    @JsonProperty("operation_result")
    private Integer operationResult;

    @JsonProperty("request_uri")
    private String requestUri;

    @JsonProperty("request_method")
    private String requestMethod;

    @JsonProperty("response_code")
    private String responseCode;

    @JsonProperty("ip_address")
    private String ipAddress;

    @JsonProperty("trace_id")
    private String traceId;

    @JsonProperty("request_snapshot")
    private String requestSnapshot;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    @JsonProperty("event_time")
    private Instant eventTime;

    @JsonProperty("description")
    private String description;
}
