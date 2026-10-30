package net.hwyz.iov.cloud.sec.ciam.service.infrastructure.persistence.po;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import net.hwyz.iov.cloud.framework.mysql.typehandler.UtcInstantTypeHandler;

import java.time.Instant;

/**
 * JWK 密钥表数据对象（SEC-CIAM-DSN-CR-002 C1 试点，RD-002-3）。
 * <p>
 * ciam_jwk 四列均为 DATETIME(3)（UTC 墙钟），字段级 opt-in
 * {@link UtcInstantTypeHandler}（Instant ↔ DATETIME UTC 显式映射）：
 * <ul>
 *   <li>{@code autoResultMap = true}：MyBatis-Plus 泛型 SELECT 读路径携带字段 typeHandler；</li>
 *   <li>仅对已登记为 UTC DATETIME 的列启用，不全局套用 Instant/TIMESTAMP；</li>
 *   <li>create_time/modify_time 由 DB DEFAULT/ON UPDATE 生成（app 不 set），handler 仅负责读取还原；</li>
 *   <li>issue_time/expire_time 为业务事件（app 写入），handler 负责显式 UTC 写读。</li>
 * </ul>
 */
@Data
@TableName(value = "ciam_jwk", autoResultMap = true)
public class JwkPo {

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

    @TableField(value = "issue_time", typeHandler = UtcInstantTypeHandler.class)
    private Instant issueTime;

    @TableField(value = "expire_time", typeHandler = UtcInstantTypeHandler.class)
    private Instant expireTime;

    @TableField("is_primary")
    private Integer isPrimary;

    @TableField(value = "create_time", typeHandler = UtcInstantTypeHandler.class)
    private Instant createTime;

    @TableField(value = "modify_time", typeHandler = UtcInstantTypeHandler.class)
    private Instant modifyTime;

    @TableField("row_version")
    private Integer rowVersion;
}
