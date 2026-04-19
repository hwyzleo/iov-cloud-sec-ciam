package net.hwyz.iov.cloud.sec.ciam.service.controller.mobile;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import net.hwyz.iov.cloud.framework.common.bean.ApiResponse;
import net.hwyz.iov.cloud.framework.web.context.SecurityContextHolder;
import net.hwyz.iov.cloud.sec.ciam.service.controller.mobile.dto.TriggerMfaRequest;
import net.hwyz.iov.cloud.sec.ciam.service.controller.mobile.dto.VerifyMfaRequest;
import net.hwyz.iov.cloud.sec.ciam.service.domain.enums.ChallengeScene;
import net.hwyz.iov.cloud.sec.ciam.service.domain.enums.ChallengeType;
import net.hwyz.iov.cloud.sec.ciam.service.domain.repository.CiamRiskEventRepository;
import net.hwyz.iov.cloud.sec.ciam.service.domain.service.MfaDomainService;
import net.hwyz.iov.cloud.sec.ciam.service.infrastructure.repository.dao.dataobject.CiamRiskEventDo;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * 风控控制器 — MFA 挑战触发/校验、风险事件查询。
 */
@RestController
@RequestMapping("/api/mobile/v1/risk")
@RequiredArgsConstructor
public class MobileRiskController {

    private final MfaDomainService mfaDomainService;
    private final CiamRiskEventRepository riskEventRepository;

    /** 触发 MFA 挑战 */
    @PostMapping("/mfa/trigger")
    public ApiResponse<Map<String, String>> triggerMfa(@RequestBody @Valid TriggerMfaRequest request) {
        String challengeId = mfaDomainService.createChallenge(
                request.getUserId(), request.getSessionId(),
                ChallengeType.fromCode(request.getChallengeType()),
                ChallengeScene.fromCode(request.getChallengeScene()),
                request.getReceiverMask(), request.getRiskEventId());
        return ApiResponse.ok(Map.of("challengeId", challengeId));
    }

    /** 校验 MFA 挑战 */
    @PostMapping("/mfa/verify")
    public ApiResponse<Map<String, Boolean>> verifyMfa(@RequestBody @Valid VerifyMfaRequest request) {
        boolean passed = mfaDomainService.verifyChallenge(request.getChallengeId(), request.getCode());
        return ApiResponse.ok(Map.of("passed", passed));
    }

    /** 查询用户风险事件 */
    @GetMapping("/events")
    public ApiResponse<List<CiamRiskEventDo>> queryRiskEvents(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startTime,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endTime) {
        String userId = SecurityContextHolder.getUserId();
        List<CiamRiskEventDo> events = riskEventRepository.findByUserIdAndTimeRange(userId, startTime, endTime);
        return ApiResponse.ok(events);
    }
}
