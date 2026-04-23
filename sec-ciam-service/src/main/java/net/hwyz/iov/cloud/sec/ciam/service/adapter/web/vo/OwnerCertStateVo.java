package net.hwyz.iov.cloud.sec.ciam.service.adapter.web.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

/**
 * 车主认证状态 VO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OwnerCertStateVo {
    private String ownerCertId;
    private String vehicleId;
    private String vin;
    private Integer certStatus;
    private Instant effectiveTime;
    private Instant expireTime;
    private String resultMessage;
}
