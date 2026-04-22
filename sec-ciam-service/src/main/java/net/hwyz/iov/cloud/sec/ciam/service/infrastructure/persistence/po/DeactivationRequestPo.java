package net.hwyz.iov.cloud.sec.ciam.service.infrastructure.persistence.po;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.Instant;

/**
 * 注销申请表数据对象。
 */
@Data
@TableName("ciam_deactivation_request")
public class DeactivationRequestPo {

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    @TableField("deactivation_request_id")
    private String deactivationRequestId;

    @TableField("user_id")
    private String userId;

    @TableField("request_source")
    private String requestSource;

    @TableField("request_reason")
    private String requestReason;

    @TableField("check_status")
    private Integer checkStatus;

    @TableField("review_status")
    private Integer reviewStatus;

    @TableField("execute_status")
    private Integer executeStatus;

    @TableField("requested_time")
    private Instant requestedTime;

    @TableField("reviewer")
    private String reviewer;

    @TableField("review_time")
    private Instant reviewTime;

    @TableField("execute_time")
    private Instant executeTime;

    @TableField("retain_audit_only")
    private Integer retainAuditOnly;

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
