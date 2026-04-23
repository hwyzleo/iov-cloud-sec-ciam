package net.hwyz.iov.cloud.sec.ciam.service.application.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SessionDto2 {
    private String sessionId;
    private String userId;
    private String deviceId;
    private String clientId;
    private String clientType;
    private String loginIp;
    private String loginRegion;
    private Integer riskLevel;
    private Integer sessionStatus;
    private Instant loginTime;
    private Instant lastActiveTime;
    private Instant expireTime;
    private String description;
}
