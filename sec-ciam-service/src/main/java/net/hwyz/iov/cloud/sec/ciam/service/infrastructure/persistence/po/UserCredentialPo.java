package net.hwyz.iov.cloud.sec.ciam.service.infrastructure.persistence.po;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.Instant;

/**
 * 凭据表数据对象。
 */
@Data
@TableName("ciam_user_credential")
public class UserCredentialPo {

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    @TableField("credential_id")
    private String credentialId;

    @TableField("user_id")
    private String userId;

    @TableField("credential_type")
    private String credentialType;

    @TableField("credential_hash")
    private String credentialHash;

    @TableField("salt")
    private String salt;

    @TableField("hash_algorithm")
    private String hashAlgorithm;

    @TableField("password_set_time")
    private Instant passwordSetTime;

    @TableField("last_verify_time")
    private Instant lastVerifyTime;

    @TableField("fail_count")
    private Integer failCount;

    @TableField("locked_until")
    private Instant lockedUntil;

    @TableField("credential_status")
    private Integer credentialStatus;

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
