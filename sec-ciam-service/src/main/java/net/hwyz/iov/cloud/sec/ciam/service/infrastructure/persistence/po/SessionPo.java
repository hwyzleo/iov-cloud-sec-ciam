package net.hwyz.iov.cloud.sec.ciam.service.infrastructure.persistence.po;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.Instant;

/**
 * 会话表数据对象。
 */
@Data
@TableName("ciam_session")
public class SessionPo {

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    @TableField("session_id")
    private String sessionId;

    @TableField("user_id")
    private String userId;

    @TableField("device_id")
    private String deviceId;

    @TableField("client_id")
    private String clientId;

    @TableField("client_type")
    private String clientType;

    @TableField("login_ip")
    private String loginIp;

    @TableField("login_region")
    private String loginRegion;

    @TableField("risk_level")
    private Integer riskLevel;

    @TableField("session_status")
    private Integer sessionStatus;

    @TableField("login_time")
    private Instant loginTime;

    @TableField("last_active_time")
    private Instant lastActiveTime;

    @TableField("logout_time")
    private Instant logoutTime;

    @TableField("expire_time")
    private Instant expireTime;

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
