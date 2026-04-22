package net.hwyz.iov.cloud.sec.ciam.service.infrastructure.repository.dao.dataobject;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.Instant;

/**
 * 邀请关系表数据对象。
 */
@Data
@TableName("ciam_invitation_relation")
public class InvitationRelationPo {

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    @TableField("relation_id")
    private String relationId;

    @TableField("inviter_user_id")
    private String inviterUserId;

    @TableField("invitee_user_id")
    private String inviteeUserId;

    @TableField("invite_code")
    private String inviteCode;

    @TableField("invite_channel_code")
    private String inviteChannelCode;

    @TableField("invite_activity_code")
    private String inviteActivityCode;

    @TableField("relation_lock_flag")
    private Integer relationLockFlag;

    @TableField("register_time")
    private Instant registerTime;

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
