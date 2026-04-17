package net.hwyz.iov.cloud.sec.ciam.infrastructure.repository.dao.dataobject;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.Instant;

/**
 * 协议与营销同意表数据对象。
 */
@Data
@TableName("ciam_user_consent")
public class CiamUserConsentDo {

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    @TableField("consent_id")
    private String consentId;

    @TableField("user_id")
    private String userId;

    @TableField("consent_type")
    private String consentType;

    @TableField("consent_status")
    private Integer consentStatus;

    @TableField("policy_version")
    private String policyVersion;

    @TableField("source_channel")
    private String sourceChannel;

    @TableField("client_type")
    private String clientType;

    @TableField("operate_ip")
    private String operateIp;

    @TableField("operate_time")
    private Instant operateTime;

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
