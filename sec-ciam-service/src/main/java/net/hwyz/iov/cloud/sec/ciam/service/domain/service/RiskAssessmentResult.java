package net.hwyz.iov.cloud.sec.ciam.service.domain.service;

import lombok.Builder;
import lombok.Getter;
import net.hwyz.iov.cloud.sec.ciam.service.domain.enums.DecisionResult;
import net.hwyz.iov.cloud.sec.ciam.service.domain.enums.RiskLevel;

import java.util.List;

/**
 * 风险评估结果 DTO。
 */
@Getter
@Builder
public class RiskAssessmentResult {

    private final RiskLevel riskLevel;
    private final DecisionResult decisionResult;
    private final List<String> hitRules;
    private final String riskEventId;
}
