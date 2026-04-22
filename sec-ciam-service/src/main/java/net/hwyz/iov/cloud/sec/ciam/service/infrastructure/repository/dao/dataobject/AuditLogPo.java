package net.hwyz.iov.cloud.sec.ciam.service.infrastructure.repository.dao.dataobject;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.Instant;

/**
 * 审计日志表数据对象。
 */
@Data
@TableName("ciam_audit_log")
public class AuditLogPo {

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    @TableField("audit_id")
    private String auditId;

    @TableField("user_id")
    private String userId;

    @TableField("session_id")
    private String sessionId;

    @TableField("client_id")
    private String clientId;

    @TableField("client_type")
    private String clientType;

    @TableField("event_type")
    private String eventType;

    @TableField("event_name")
    private String eventName;

    @TableField("operation_result")
    private Integer operationResult;

    @TableField("request_uri")
    private String requestUri;

    @TableField("request_method")
    private String requestMethod;

    @TableField("response_code")
    private String responseCode;

    @TableField("ip_address")
    private String ipAddress;

    @TableField("trace_id")
    private String traceId;

    @TableField("request_snapshot")
    private String requestSnapshot;

    @TableField("event_time")
    private Instant eventTime;

    @TableField("description")
    private String description;

    @TableField("create_time")
    private Instant createTime;

    @TableField("create_by")
    private String createBy;

    @TableField("modify_time")
    private Instant modifyTime;

    @TableField("modify_by")
    private String modifyBy;

    @TableField("row_version")
    private Integer rowVersion;

    @TableField("row_valid")
    private Integer rowValid;
}
