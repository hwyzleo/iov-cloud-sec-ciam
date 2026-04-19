package net.hwyz.iov.cloud.sec.ciam.service.infrastructure.repository.dao.dataobject;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.Instant;

/**
 * 登录标识表数据对象。
 */
@Data
@TableName("ciam_user_identity")
public class CiamUserIdentityDo {

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    @TableField("identity_id")
    private String identityId;

    @TableField("user_id")
    private String userId;

    @TableField("identity_type")
    private String identityType;

    @TableField("identity_value")
    private String identityValue;

    @TableField("identity_hash")
    private String identityHash;

    @TableField("country_code")
    private String countryCode;

    @TableField("verified_flag")
    private Integer verifiedFlag;

    @TableField("primary_flag")
    private Integer primaryFlag;

    @TableField("bind_source")
    private String bindSource;

    @TableField("bind_time")
    private Instant bindTime;

    @TableField("unbind_time")
    private Instant unbindTime;

    @TableField("identity_status")
    private Integer identityStatus;

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
