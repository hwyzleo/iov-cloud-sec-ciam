package net.hwyz.iov.cloud.sec.ciam.service.adapter.web.controller.mobile;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import net.hwyz.iov.cloud.framework.common.bean.ApiResponse;
import net.hwyz.iov.cloud.framework.web.context.SecurityContextHolder;
import net.hwyz.iov.cloud.framework.web.controller.BaseController;
import net.hwyz.iov.cloud.sec.ciam.service.adapter.web.vo.RiskEventVo;
import net.hwyz.iov.cloud.sec.ciam.service.application.service.MfaAppService;
import net.hwyz.iov.cloud.sec.ciam.service.application.service.RiskEventAppService;
import net.hwyz.iov.cloud.sec.ciam.service.application.assembler.RiskEventAssembler;
import net.hwyz.iov.cloud.sec.ciam.service.adapter.web.controller.mobile.vo.TriggerMfaRequest;
import net.hwyz.iov.cloud.sec.ciam.service.adapter.web.controller.mobile.vo.VerifyMfaRequest;
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
import java.util.stream.Collectors;

/**
 * 风控控制器 — MFA 挑战触发/校验、风险事件查询。
 */
@RestController
@RequestMapping("/api/mobile/v1/risk")
@RequiredArgsConstructor
public class MobileRiskController extends BaseController {

    private final MfaAppService mfaAppService;
    private final RiskEventAppService riskEventAppService;
    
    private final RiskEventAssembler riskEventAssembler = RiskEventAssembler.INSTANCE;

    /** 触发 MFA 挑战 */
    @PostMapping("/mfa/trigger")
    public ApiResponse<Map<String, String>> triggerMfa(@RequestBody @Valid TriggerMfaRequest request) {
        String challengeId = mfaAppService.triggerMfa(
                request.getUserId(), request.getSessionId(),
                request.getChallengeType(),
                request.getChallengeScene(),
                request.getReceiverMask(), request.getRiskEventId());
        return ApiResponse.ok(Map.of("challengeId", challengeId));
    }

    /** 校验 MFA 挑战 */
    @PostMapping("/mfa/verify")
    public ApiResponse<Map<String, Boolean>> verifyMfa(@RequestBody @Valid VerifyMfaRequest request) {
        boolean passed = mfaAppService.verifyMfa(request.getChallengeId(), request.getCode());
        return ApiResponse.ok(Map.of("passed", passed));
    }

    /** 查询用户风险事件 */
    @GetMapping("/events")
    public ApiResponse<List<RiskEventVo>> queryRiskEvents(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startTime,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endTime) {
        String userId = SecurityContextHolder.getUserId();
        List<RiskEventVo> voList = riskEventAppService.queryUserRiskEvents(userId, startTime, endTime).stream()
            .map(riskEventAssembler::toVo)
            .collect(Collectors.toList());
        return ApiResponse.ok(voList);
    }
}
