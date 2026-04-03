package net.hwyz.iov.cloud.sec.ciam.infrastructure.repository.dao.dataobject;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 刷新令牌表数据对象。
 */
@Data
@TableName("ciam_refresh_token")
public class CiamRefreshTokenDo {

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
    private LocalDateTime issueTime;

    @TableField("used_time")
    private LocalDateTime usedTime;

    @TableField("revoke_time")
    private LocalDateTime revokeTime;

    @TableField("expire_time")
    private LocalDateTime expireTime;

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
