package net.hwyz.iov.cloud.sec.ciam.service.infrastructure.repository.dao.dataobject;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.Instant;

/**
 * 用户主表数据对象。
 */
@Data
@TableName("ciam_user")
public class UserPo {

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    @TableField("user_id")
    private String userId;

    @TableField("user_status")
    private Integer userStatus;

    @TableField("brand_code")
    private String brandCode;

    @TableField("register_source")
    private String registerSource;

    @TableField("register_channel")
    private String registerChannel;

    @TableField("primary_identity_type")
    private String primaryIdentityType;

    @TableField("last_login_time")
    private Instant lastLoginTime;

    @TableField("deactivated_time")
    private Instant deactivatedTime;

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
