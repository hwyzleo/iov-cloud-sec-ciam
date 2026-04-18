package net.hwyz.iov.cloud.sec.ciam.infrastructure.repository.dao.dataobject;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.Instant;

/**
 * JWK 密钥表数据对象。
 */
@Data
@TableName("ciam_jwk")
public class CiamJwkDo {

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    @TableField("key_id")
    private String keyId;

    @TableField("private_key_pem")
    private String privateKeyPem;

    @TableField("public_key_pem")
    private String publicKeyPem;

    @TableField("algorithm")
    private String algorithm;

    @TableField("key_size")
    private Integer keySize;

    @TableField("status")
    private Integer status;

    @TableField("issue_time")
    private Instant issueTime;

    @TableField("expire_time")
    private Instant expireTime;

    @TableField("is_primary")
    private Integer isPrimary;

    @TableField("create_time")
    private Instant createTime;

    @TableField("modify_time")
    private Instant modifyTime;

    @TableField("row_version")
    private Integer rowVersion;
}
