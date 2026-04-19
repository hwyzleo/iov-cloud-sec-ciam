package net.hwyz.iov.cloud.sec.ciam.infrastructure.repository.dao.dataobject;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.Instant;

/**
 * 设备表数据对象。
 */
@Data
@TableName("ciam_device")
public class CiamDeviceDo {

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    @TableField("device_id")
    private String deviceId;

    @TableField("user_id")
    private String userId;

    @TableField("client_type")
    private String clientType;

    @TableField("client_id")
    private String clientId;

    @TableField("device_name")
    private String deviceName;

    @TableField("device_os")
    private String deviceOs;

    @TableField("app_version")
    private String appVersion;

    @TableField("device_fingerprint")
    private String deviceFingerprint;

    @TableField("language")
    private String language;

    @TableField("trusted_flag")
    private Integer trustedFlag;

    @TableField("first_login_time")
    private Instant firstLoginTime;

    @TableField("last_login_time")
    private Instant lastLoginTime;

    @TableField("device_status")
    private Integer deviceStatus;

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
