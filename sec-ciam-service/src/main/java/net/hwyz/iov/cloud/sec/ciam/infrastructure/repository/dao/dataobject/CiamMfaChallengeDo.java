package net.hwyz.iov.cloud.sec.ciam.infrastructure.repository.dao.dataobject;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * MFA 挑战表数据对象。
 */
@Data
@TableName("ciam_mfa_challenge")
public class CiamMfaChallengeDo {

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    @TableField("challenge_id")
    private String challengeId;

    @TableField("user_id")
    private String userId;

    @TableField("session_id")
    private String sessionId;

    @TableField("challenge_type")
    private String challengeType;

    @TableField("challenge_scene")
    private String challengeScene;

    @TableField("receiver_mask")
    private String receiverMask;

    @TableField("verify_code_hash")
    private String verifyCodeHash;

    @TableField("send_time")
    private LocalDateTime sendTime;

    @TableField("expire_time")
    private LocalDateTime expireTime;

    @TableField("verify_time")
    private LocalDateTime verifyTime;

    @TableField("challenge_status")
    private Integer challengeStatus;

    @TableField("risk_event_id")
    private String riskEventId;

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
