package net.hwyz.iov.cloud.sec.ciam.service.infrastructure.repository.dao.dataobject;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.Instant;

/**
 * 授权码记录表数据对象。
 */
@Data
@TableName("ciam_auth_code")
public class CiamAuthCodeDo {

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    @TableField("auth_code_id")
    private String authCodeId;

    @TableField("client_id")
    private String clientId;

    @TableField("user_id")
    private String userId;

    @TableField("session_id")
    private String sessionId;

    @TableField("code_hash")
    private String codeHash;

    @TableField("redirect_uri")
    private String redirectUri;

    @TableField("scope")
    private String scope;

    @TableField("code_challenge")
    private String codeChallenge;

    @TableField("challenge_method")
    private String challengeMethod;

    @TableField("expire_time")
    private Instant expireTime;

    @TableField("used_flag")
    private Integer usedFlag;

    @TableField("used_time")
    private Instant usedTime;

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
