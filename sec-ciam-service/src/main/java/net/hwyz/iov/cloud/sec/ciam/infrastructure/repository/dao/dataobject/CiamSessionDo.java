package net.hwyz.iov.cloud.sec.ciam.infrastructure.repository.dao.dataobject;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 会话表数据对象。
 */
@Data
@TableName("ciam_session")
public class CiamSessionDo {

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
    private LocalDateTime loginTime;

    @TableField("last_active_time")
    private LocalDateTime lastActiveTime;

    @TableField("logout_time")
    private LocalDateTime logoutTime;

    @TableField("expire_time")
    private LocalDateTime expireTime;

    @TableField("description")
    private String description;

    @TableField("create_time")
    private LocalDateTime createTime;

    @TableField("create_by")
    private String createBy;

    @TableField("modify_time")
    private LocalDateTime modifyTime;

    @TableField("modify_by")
    private String modifyBy;

    @TableField("row_version")
    private Integer rowVersion;

    @TableField("row_valid")
    private Integer rowValid;
}
