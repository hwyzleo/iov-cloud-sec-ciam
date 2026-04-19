package net.hwyz.iov.cloud.sec.ciam.service.domain.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.hwyz.iov.cloud.framework.common.util.DateTimeUtil;
import net.hwyz.iov.cloud.sec.ciam.service.domain.enums.DecisionResult;
import net.hwyz.iov.cloud.sec.ciam.service.domain.enums.RiskLevel;
import net.hwyz.iov.cloud.sec.ciam.service.domain.repository.CiamRiskEventRepository;
import net.hwyz.iov.cloud.sec.ciam.service.infrastructure.repository.dao.dataobject.CiamRiskEventDo;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * 登录风险评估领域服务。
 * <p>
 * 基于账号、设备、IP、地域、客户端维度实现初版规则引擎。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RiskAssessmentService {

    private final CiamRiskEventRepository riskEventRepository;

    /**
     * 评估登录风险。
     *
     * @param userId       用户业务唯一标识
     * @param deviceId     设备业务唯一标识
     * @param ipAddress    登录 IP
     * @param regionCode   地区编码
     * @param clientType   客户端类型
     * @param isNewDevice  是否新设备
     * @param isGeoChanged 是否异地登录
     * @return 风险评估结果
     */
    public RiskAssessmentResult assessLoginRisk(String userId, String deviceId,
                                                String ipAddress, String regionCode,
                                                String clientType,
                                                boolean isNewDevice, boolean isGeoChanged) {
        List<String> hitRules = new ArrayList<>();
        RiskLevel riskLevel = RiskLevel.LOW;
        DecisionResult decision = DecisionResult.ALLOW;

        if (isNewDevice && isGeoChanged) {
            hitRules.add("new_device_and_geo_change");
            riskLevel = RiskLevel.HIGH;
            decision = DecisionResult.BLOCK;
        } else if (isNewDevice) {
            hitRules.add("new_device");
            riskLevel = RiskLevel.MEDIUM;
            decision = DecisionResult.CHALLENGE;
        } else if (isGeoChanged) {
            hitRules.add("geo_change");
            riskLevel = RiskLevel.MEDIUM;
            decision = DecisionResult.CHALLENGE;
        }

        String riskEventId = UUID.randomUUID().toString();
        Instant now = DateTimeUtil.getNowInstant();

        CiamRiskEventDo event = new CiamRiskEventDo();
        event.setRiskEventId(riskEventId);
        event.setUserId(userId);
        event.setDeviceId(deviceId);
        event.setRiskScene("login");
        event.setRiskType(hitRules.isEmpty() ? "normal" : String.join(",", hitRules));
        event.setRiskLevel(riskLevel.getCode());
        event.setClientType(clientType);
        event.setIpAddress(ipAddress);
        event.setRegionCode(regionCode);
        event.setDecisionResult(decision.getCode());
        event.setHitRules(hitRules.isEmpty() ? null : String.join(",", hitRules));
        event.setEventTime(now);
        event.setHandledFlag(0);
        event.setRowVersion(1);
        event.setRowValid(1);
        event.setCreateTime(now);
        event.setModifyTime(now);
        riskEventRepository.insert(event);

        log.info("登录风险评估完成: userId={}, riskLevel={}, decision={}, hitRules={}",
                userId, riskLevel, decision, hitRules);

        return RiskAssessmentResult.builder()
                .riskLevel(riskLevel)
                .decisionResult(decision)
                .hitRules(hitRules)
                .riskEventId(riskEventId)
                .build();
    }
}
