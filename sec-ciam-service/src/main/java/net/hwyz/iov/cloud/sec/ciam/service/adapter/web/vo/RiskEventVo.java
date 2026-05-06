package net.hwyz.iov.cloud.sec.ciam.service.adapter.web.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

/**
 * 风险事件 VO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RiskEventVo {

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

    private String description;
}