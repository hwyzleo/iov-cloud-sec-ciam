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
public class RiskEvent {
    private String riskEventId;
    private String userId;
    private String sessionId;
    private String deviceId;
    private String riskScene;
    private String riskType;
    private Integer riskLevel;
    private String clientType;
    private String ipAddress;
    private String regionCode;
    private String decisionResult;
    private String hitRules;
    private Integer handledFlag;
    private Instant eventTime;
    private Instant modifyTime;
    private String description;
}
