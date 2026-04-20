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
public class Device {
    private String deviceId;
    private String userId;
    private String clientType;
    private String clientId;
    private String appVersion;
    private String deviceName;
    private String deviceOs;
    private String deviceFingerprint;
    private String language;
    private Integer trustedFlag;
    private Integer deviceStatus;
    private Instant firstLoginTime;
    private Instant lastLoginTime;
    private String description;
}
