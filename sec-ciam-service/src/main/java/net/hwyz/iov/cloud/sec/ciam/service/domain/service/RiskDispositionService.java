package net.hwyz.iov.cloud.sec.ciam.service.domain.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.hwyz.iov.cloud.framework.common.exception.BusinessException;
import net.hwyz.iov.cloud.sec.ciam.service.common.audit.AuditEvent;
import net.hwyz.iov.cloud.sec.ciam.service.common.audit.AuditEventType;
import net.hwyz.iov.cloud.sec.ciam.service.common.audit.AuditLogger;
import net.hwyz.iov.cloud.sec.ciam.service.common.exception.CiamErrorCode;
import net.hwyz.iov.cloud.framework.common.util.DateTimeUtil;
import net.hwyz.iov.cloud.sec.ciam.service.domain.enums.ChallengeScene;
import net.hwyz.iov.cloud.sec.ciam.service.domain.enums.ChallengeType;
import net.hwyz.iov.cloud.sec.ciam.service.domain.enums.DecisionResult;
import net.hwyz.iov.cloud.sec.ciam.service.domain.model.RiskEvent;
import net.hwyz.iov.cloud.sec.ciam.service.domain.model.Session;
import net.hwyz.iov.cloud.sec.ciam.service.domain.repository.RiskEventRepository;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 高风险处置领域服务。
 * <p>
 * 职责：
 * <ul>
 *   <li>根据风险评估结果分派处置动作（放行、挑战、阻断、强制下线）</li>
 *   <li>强制重新认证（失效指定会话）</li>
 *   <li>标记风险事件为已处理</li>
 * </ul>
 * 对应 design.md 模块 7（MFA 与风控模块）与模块 12（审计日志与安全事件模块）。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RiskDispositionService {

    private final SessionDomainService sessionDomainService;
    private final MfaDomainService mfaDomainService;
    private final RiskEventRepository riskEventRepository;
    private final AuditLogger auditLogger;

    /**
     * 根据风险评估结果执行处置动作。
     *
     * @param result    风险评估结果
     * @param userId    用户业务唯一标识
     * @param sessionId 当前会话业务唯一标识
     */
    public void handleRiskDecision(RiskAssessmentResult result, String userId, String sessionId) {
        DecisionResult decision = result.getDecisionResult();
        log.info("执行风险处置: userId={}, sessionId={}, decision={}, riskEventId={}",
                userId, sessionId, decision, result.getRiskEventId());

        switch (decision) {
            case ALLOW:
                // 放行，无需额外动作
                break;
            case CHALLENGE:
                triggerMfaChallenge(result, userId, sessionId);
                break;
            case BLOCK:
                blockSession(result, userId, sessionId);
                break;
            case KICKOUT:
                kickoutAllSessions(result, userId);
                break;
            default:
                log.warn("未知的处置结果: {}", decision);
        }
    }

    /**
     * 强制重新认证 — 失效指定会话，迫使用户重新登录。
     *
     * @param sessionId 会话业务唯一标识
     */
    public void forceReAuthentication(String sessionId) {
        sessionDomainService.invalidateSession(sessionId);
        log.info("强制重新认证完成: sessionId={}", sessionId);

        auditLogger.log(AuditEvent.builder()
                .sessionId(sessionId)
                .eventType(AuditEventType.FORCE_RE_AUTH.getCategory())
                .eventName(AuditEventType.FORCE_RE_AUTH.getDescription())
                .success(true)
                .eventTime(DateTimeUtil.getNowInstant())
                .build());
    }

    /**
     * 标记风险事件为已处理。
     *
     * @param riskEventId 风险事件业务唯一标识
     */
    public void markRiskEventHandled(String riskEventId) {
        RiskEvent event = riskEventRepository.findByRiskEventId(riskEventId)
                .orElseThrow(() -> new BusinessException(CiamErrorCode.RISK_EVENT_NOT_FOUND));

        event.setHandledFlag(1);
        riskEventRepository.updateByRiskEventId(event);

        log.info("风险事件标记为已处理: riskEventId={}", riskEventId);
    }

    // ---- 内部方法 ----

    private void triggerMfaChallenge(RiskAssessmentResult result, String userId, String sessionId) {
        mfaDomainService.createChallenge(
                userId, sessionId,
                ChallengeType.SMS,
                ChallengeScene.HIGH_RISK,
                null, result.getRiskEventId());

        auditLogger.log(AuditEvent.builder()
                .userId(userId)
                .sessionId(sessionId)
                .eventType(AuditEventType.MFA_TRIGGER.getCategory())
                .eventName(AuditEventType.MFA_TRIGGER.getDescription())
                .success(true)
                .eventTime(DateTimeUtil.getNowInstant())
                .build());
    }

    private void blockSession(RiskAssessmentResult result, String userId, String sessionId) {
        sessionDomainService.invalidateSession(sessionId);

        auditLogger.log(AuditEvent.builder()
                .userId(userId)
                .sessionId(sessionId)
                .eventType(AuditEventType.FORCE_RE_AUTH.getCategory())
                .eventName(AuditEventType.FORCE_RE_AUTH.getDescription())
                .success(true)
                .requestSnapshot("riskEventId=" + result.getRiskEventId())
                .eventTime(DateTimeUtil.getNowInstant())
                .build());

        log.info("阻断处置完成: userId={}, sessionId={}, riskEventId={}",
                userId, sessionId, result.getRiskEventId());
    }

    private void kickoutAllSessions(RiskAssessmentResult result, String userId) {
        List<Session> activeSessions = sessionDomainService.findUserSessions(userId);
        for (Session session : activeSessions) {
            sessionDomainService.invalidateSession(session.getSessionId());
        }

        auditLogger.log(AuditEvent.builder()
                .userId(userId)
                .eventType(AuditEventType.FORCE_LOGOUT.getCategory())
                .eventName(AuditEventType.FORCE_LOGOUT.getDescription())
                .success(true)
                .requestSnapshot("riskEventId=" + result.getRiskEventId()
                        + ",invalidatedSessions=" + activeSessions.size())
                .eventTime(DateTimeUtil.getNowInstant())
                .build());

        log.info("强制下线所有会话完成: userId={}, riskEventId={}, invalidatedSessions={}",
                userId, result.getRiskEventId(), activeSessions.size());
    }
}
