package net.hwyz.iov.cloud.sec.ciam.infrastructure.repository.dao.dataobject;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.Instant;

/**
 * 风险事件表数据对象。
 */
@Data
@TableName("ciam_risk_event")
public class CiamRiskEventDo {

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    @TableField("risk_event_id")
    private String riskEventId;

    @TableField("user_id")
    private String userId;

    @TableField("session_id")
    private String sessionId;

    @TableField("device_id")
    private String deviceId;

    @TableField("risk_scene")
    private String riskScene;

    @TableField("risk_type")
    private String riskType;

    @TableField("risk_level")
    private Integer riskLevel;

    @TableField("client_type")
    private String clientType;

    @TableField("ip_address")
    private String ipAddress;

    @TableField("region_code")
    private String regionCode;

    @TableField("decision_result")
    private String decisionResult;

    @TableField("hit_rules")
    private String hitRules;

    @TableField("event_time")
    private Instant eventTime;

    @TableField("handled_flag")
    private Integer handledFlag;

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
