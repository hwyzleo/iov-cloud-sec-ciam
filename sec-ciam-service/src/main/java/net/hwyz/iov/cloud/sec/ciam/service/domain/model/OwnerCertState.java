package net.hwyz.iov.cloud.sec.ciam.service.domain.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OwnerCertState {
    private String ownerCertId;
    private String userId;
    private String vehicleId;
    private String vin;
    private Integer certStatus;
    private String certSource;
    private Instant callbackTime;
    private Instant lastQueryTime;
    private Instant effectiveTime;
    private Instant expireTime;
    private String resultMessage;
    private String description;
}
