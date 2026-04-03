package net.hwyz.iov.cloud.sec.ciam.infrastructure.repository.dao.dataobject;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 凭据表数据对象。
 */
@Data
@TableName("ciam_user_credential")
public class CiamUserCredentialDo {

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
    private LocalDateTime passwordSetTime;

    @TableField("last_verify_time")
    private LocalDateTime lastVerifyTime;

    @TableField("fail_count")
    private Integer failCount;

    @TableField("locked_until")
    private LocalDateTime lockedUntil;

    @TableField("credential_status")
    private Integer credentialStatus;

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
