package net.hwyz.iov.cloud.sec.ciam.infrastructure.repository.dao.dataobject;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.Instant;

/**
 * 接入应用表数据对象。
 */
@Data
@TableName("ciam_oauth_client")
public class CiamOAuthClientDo {

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    @TableField("client_id")
    private String clientId;

    @TableField("client_name")
    private String clientName;

    @TableField("client_secret_hash")
    private String clientSecretHash;

    @TableField("client_type")
    private String clientType;

    @TableField("redirect_uris")
    private String redirectUris;

    @TableField("grant_types")
    private String grantTypes;

    @TableField("scopes")
    private String scopes;

    @TableField("pkce_required")
    private Integer pkceRequired;

    @TableField("access_token_ttl")
    private Integer accessTokenTtl;

    @TableField("refresh_token_ttl")
    private Integer refreshTokenTtl;

    @TableField("client_status")
    private Integer clientStatus;

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
