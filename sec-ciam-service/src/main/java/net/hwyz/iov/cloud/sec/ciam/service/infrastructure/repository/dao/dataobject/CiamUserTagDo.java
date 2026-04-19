package net.hwyz.iov.cloud.sec.ciam.service.infrastructure.repository.dao.dataobject;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.Instant;

/**
 * 认证标签表数据对象。
 */
@Data
@TableName("ciam_user_tag")
public class CiamUserTagDo {

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    @TableField("tag_id")
    private String tagId;

    @TableField("user_id")
    private String userId;

    @TableField("tag_code")
    private String tagCode;

    @TableField("tag_name")
    private String tagName;

    @TableField("tag_status")
    private Integer tagStatus;

    @TableField("tag_source")
    private String tagSource;

    @TableField("effective_time")
    private Instant effectiveTime;

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
