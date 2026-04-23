package net.hwyz.iov.cloud.sec.ciam.service.application.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

/**
 * 车主认证状态 DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OwnerCertStateDto {
    private String ownerCertId;
    private String userId;
    private String vehicleId;
    private String vin;
    private Integer certStatus;
    private String certSource;
    private Instant callbackTime;
    private Instant effectiveTime;
    private Instant expireTime;
    private String resultMessage;
}
