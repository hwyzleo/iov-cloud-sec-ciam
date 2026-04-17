package net.hwyz.iov.cloud.sec.ciam.infrastructure.repository.dao.dataobject;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.Instant;

/**
 * 车主认证状态表数据对象。
 */
@Data
@TableName("ciam_owner_cert_state")
public class CiamOwnerCertStateDo {

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    @TableField("owner_cert_id")
    private String ownerCertId;

    @TableField("user_id")
    private String userId;

    @TableField("vehicle_id")
    private String vehicleId;

    @TableField("vin")
    private String vin;

    @TableField("cert_status")
    private Integer certStatus;

    @TableField("cert_source")
    private String certSource;

    @TableField("callback_time")
    private Instant callbackTime;

    @TableField("last_query_time")
    private Instant lastQueryTime;

    @TableField("effective_time")
    private Instant effectiveTime;

    @TableField("expire_time")
    private Instant expireTime;

    @TableField("result_message")
    private String resultMessage;

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
