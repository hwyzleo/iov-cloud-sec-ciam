package net.hwyz.iov.cloud.sec.ciam.service.infrastructure.repository.dao.dataobject;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.Instant;

/**
 * 账号合并申请表数据对象。
 */
@Data
@TableName("ciam_merge_request")
public class MergeRequestPo {

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    @TableField("merge_request_id")
    private String mergeRequestId;

    @TableField("source_user_id")
    private String sourceUserId;

    @TableField("target_user_id")
    private String targetUserId;

    @TableField("conflict_identity_type")
    private String conflictIdentityType;

    @TableField("conflict_identity_hash")
    private String conflictIdentityHash;

    @TableField("apply_source")
    private String applySource;

    @TableField("review_status")
    private Integer reviewStatus;

    @TableField("reviewer")
    private String reviewer;

    @TableField("review_time")
    private Instant reviewTime;

    @TableField("final_user_id")
    private String finalUserId;

    @TableField("finish_time")
    private Instant finishTime;

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
