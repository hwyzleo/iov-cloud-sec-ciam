package net.hwyz.iov.cloud.sec.ciam.service.infrastructure.persistence.po;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.Instant;

/**
 * 刷新令牌表数据对象。
 */
@Data
@TableName("ciam_refresh_token")
public class RefreshTokenPo {

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    @TableField("refresh_token_id")
    private String refreshTokenId;

    @TableField("user_id")
    private String userId;

    @TableField("session_id")
    private String sessionId;

    @TableField("client_id")
    private String clientId;

    @TableField("token_fingerprint")
    private String tokenFingerprint;

    @TableField("parent_token_id")
    private String parentTokenId;

    @TableField("token_status")
    private Integer tokenStatus;

    @TableField("issue_time")
    private Instant issueTime;

    @TableField("used_time")
    private Instant usedTime;

    @TableField("revoke_time")
    private Instant revokeTime;

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
