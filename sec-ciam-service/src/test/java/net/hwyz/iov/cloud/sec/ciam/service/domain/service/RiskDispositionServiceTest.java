package net.hwyz.iov.cloud.sec.ciam.service.domain.service;

import net.hwyz.iov.cloud.framework.common.exception.BusinessException;
import net.hwyz.iov.cloud.sec.ciam.service.common.audit.AuditEvent;
import net.hwyz.iov.cloud.sec.ciam.service.common.audit.AuditEventType;
import net.hwyz.iov.cloud.sec.ciam.service.common.audit.AuditLogger;
import net.hwyz.iov.cloud.sec.ciam.service.common.exception.CiamErrorCode;
import net.hwyz.iov.cloud.sec.ciam.service.domain.adapter.AdapterResult;
import net.hwyz.iov.cloud.sec.ciam.service.domain.adapter.EmailAdapter;
import net.hwyz.iov.cloud.sec.ciam.service.domain.adapter.SmsAdapter;
import net.hwyz.iov.cloud.sec.ciam.service.domain.enums.DecisionResult;
import net.hwyz.iov.cloud.sec.ciam.service.domain.enums.RiskLevel;
import net.hwyz.iov.cloud.sec.ciam.service.domain.enums.SessionStatus;
import net.hwyz.iov.cloud.sec.ciam.service.domain.repository.CiamDeviceRepository;
import net.hwyz.iov.cloud.sec.ciam.service.domain.repository.CiamMfaChallengeRepository;
import net.hwyz.iov.cloud.sec.ciam.service.domain.repository.CiamRefreshTokenRepository;
import net.hwyz.iov.cloud.sec.ciam.service.domain.repository.CiamRiskEventRepository;
import net.hwyz.iov.cloud.sec.ciam.service.domain.repository.CiamSessionRepository;
import net.hwyz.iov.cloud.sec.ciam.service.infrastructure.repository.dao.dataobject.RiskEventPo;
import net.hwyz.iov.cloud.sec.ciam.service.infrastructure.repository.dao.dataobject.SessionPo;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

class RiskDispositionServiceTest {

    private CiamSessionRepository sessionRepository;
    private CiamRefreshTokenRepository refreshTokenRepository;
    private CiamMfaChallengeRepository challengeRepository;
    private CiamRiskEventRepository riskEventRepository;
    private AuditLogger auditLogger;
    private SmsAdapter smsAdapter;
    private EmailAdapter emailAdapter;

    private RiskDispositionService service;

    private static final String USER_ID = "user-001";
    private static final String SESSION_ID = "session-001";
    private static final String RISK_EVENT_ID = "risk-001";

    @BeforeEach
    void setUp() {
        sessionRepository = mock(CiamSessionRepository.class);
        refreshTokenRepository = mock(CiamRefreshTokenRepository.class);
        CiamDeviceRepository deviceRepository = mock(CiamDeviceRepository.class);
        challengeRepository = mock(CiamMfaChallengeRepository.class);
        riskEventRepository = mock(CiamRiskEventRepository.class);
        auditLogger = mock(AuditLogger.class);
        smsAdapter = mock(SmsAdapter.class);
        emailAdapter = mock(EmailAdapter.class);

        when(sessionRepository.updateBySessionId(any())).thenReturn(1);
        when(refreshTokenRepository.revokeAllBySessionId(anyString())).thenReturn(0);
        when(challengeRepository.insert(any())).thenReturn(1);
        when(smsAdapter.sendVerificationCode(any(), anyString(), anyString()))
                .thenReturn(AdapterResult.ok());
        when(emailAdapter.sendVerificationCode(anyString(), anyString()))
                .thenReturn(AdapterResult.ok());

        SessionDomainService sessionDomainService = new SessionDomainService(
                sessionRepository, refreshTokenRepository, deviceRepository);
        MfaDomainService mfaDomainService = new MfaDomainService(
                challengeRepository, smsAdapter, emailAdapter);

        service = new RiskDispositionService(sessionDomainService, mfaDomainService,
                riskEventRepository, auditLogger);
    }

    private RiskAssessmentResult buildResult(DecisionResult decision) {
        return RiskAssessmentResult.builder()
                .riskLevel(decision == DecisionResult.ALLOW ? RiskLevel.LOW : RiskLevel.HIGH)
                .decisionResult(decision)
                .hitRules(List.of("test_rule"))
                .riskEventId(RISK_EVENT_ID)
                .build();
    }

    private SessionPo activeSession(String sessionId) {
        SessionPo session = new SessionPo();
        session.setSessionId(sessionId);
        session.setUserId(USER_ID);
        session.setSessionStatus(SessionStatus.ACTIVE.getCode());
        session.setRowValid(1);
        return session;
    }

    // ---- handleRiskDecision ----

    @Nested
    class HandleRiskDecisionTests {

        @Test
        void allow_noActionTaken() {
            RiskAssessmentResult result = buildResult(DecisionResult.ALLOW);

            service.handleRiskDecision(result, USER_ID, SESSION_ID);

            verifyNoInteractions(sessionRepository);
            verifyNoInteractions(challengeRepository);
            verifyNoInteractions(auditLogger);
        }

        @Test
        void challenge_triggersMfaAndLogsAudit() {
            RiskAssessmentResult result = buildResult(DecisionResult.CHALLENGE);

            service.handleRiskDecision(result, USER_ID, SESSION_ID);

            // MFA challenge should be created
            verify(challengeRepository).insert(any());
            verify(smsAdapter).sendVerificationCode(any(), anyString(), anyString());
            // Session should NOT be invalidated
            verify(sessionRepository, never()).updateBySessionId(any());

            ArgumentCaptor<AuditEvent> captor = ArgumentCaptor.forClass(AuditEvent.class);
            verify(auditLogger).log(captor.capture());
            AuditEvent event = captor.getValue();
            assertEquals(AuditEventType.MFA_TRIGGER.getCategory(), event.getEventType());
            assertEquals(USER_ID, event.getUserId());
            assertTrue(event.isSuccess());
        }

        @Test
        void block_invalidatesSessionAndLogsAudit() {
            SessionPo session = activeSession(SESSION_ID);
            when(sessionRepository.findBySessionId(SESSION_ID)).thenReturn(Optional.of(session));

            RiskAssessmentResult result = buildResult(DecisionResult.BLOCK);

            service.handleRiskDecision(result, USER_ID, SESSION_ID);

            assertEquals(SessionStatus.INVALID.getCode(), session.getSessionStatus());
            verify(sessionRepository).updateBySessionId(session);
            verify(refreshTokenRepository).revokeAllBySessionId(SESSION_ID);
            // No MFA challenge
            verify(challengeRepository, never()).insert(any());

            ArgumentCaptor<AuditEvent> captor = ArgumentCaptor.forClass(AuditEvent.class);
            verify(auditLogger).log(captor.capture());
            AuditEvent event = captor.getValue();
            assertEquals(AuditEventType.FORCE_RE_AUTH.getCategory(), event.getEventType());
            assertEquals(USER_ID, event.getUserId());
            assertTrue(event.getRequestSnapshot().contains(RISK_EVENT_ID));
        }

        @Test
        void kickout_invalidatesAllUserSessionsAndLogsAudit() {
            SessionPo s1 = activeSession("s1");
            SessionPo s2 = activeSession("s2");
            when(sessionRepository.findByUserIdAndStatus(USER_ID, SessionStatus.ACTIVE.getCode()))
                    .thenReturn(List.of(s1, s2));
            when(sessionRepository.findBySessionId("s1")).thenReturn(Optional.of(s1));
            when(sessionRepository.findBySessionId("s2")).thenReturn(Optional.of(s2));

            RiskAssessmentResult result = buildResult(DecisionResult.KICKOUT);

            service.handleRiskDecision(result, USER_ID, SESSION_ID);

            assertEquals(SessionStatus.INVALID.getCode(), s1.getSessionStatus());
            assertEquals(SessionStatus.INVALID.getCode(), s2.getSessionStatus());
            verify(sessionRepository).updateBySessionId(s1);
            verify(sessionRepository).updateBySessionId(s2);

            ArgumentCaptor<AuditEvent> captor = ArgumentCaptor.forClass(AuditEvent.class);
            verify(auditLogger).log(captor.capture());
            AuditEvent event = captor.getValue();
            assertEquals(AuditEventType.FORCE_LOGOUT.getCategory(), event.getEventType());
            assertEquals(USER_ID, event.getUserId());
            assertTrue(event.getRequestSnapshot().contains("invalidatedSessions=2"));
        }

        @Test
        void kickout_handlesNoActiveSessionsGracefully() {
            when(sessionRepository.findByUserIdAndStatus(USER_ID, SessionStatus.ACTIVE.getCode()))
                    .thenReturn(Collections.emptyList());

            RiskAssessmentResult result = buildResult(DecisionResult.KICKOUT);

            service.handleRiskDecision(result, USER_ID, SESSION_ID);

            verify(sessionRepository, never()).updateBySessionId(any());
            verify(auditLogger).log(any(AuditEvent.class));
        }
    }

    // ---- forceReAuthentication ----

    @Nested
    class ForceReAuthenticationTests {

        @Test
        void forceReAuthentication_invalidatesSessionAndLogsAudit() {
            SessionPo session = activeSession(SESSION_ID);
            when(sessionRepository.findBySessionId(SESSION_ID)).thenReturn(Optional.of(session));

            service.forceReAuthentication(SESSION_ID);

            assertEquals(SessionStatus.INVALID.getCode(), session.getSessionStatus());
            verify(sessionRepository).updateBySessionId(session);

            ArgumentCaptor<AuditEvent> captor = ArgumentCaptor.forClass(AuditEvent.class);
            verify(auditLogger).log(captor.capture());
            AuditEvent event = captor.getValue();
            assertEquals(AuditEventType.FORCE_RE_AUTH.getCategory(), event.getEventType());
            assertEquals(AuditEventType.FORCE_RE_AUTH.getDescription(), event.getEventName());
            assertEquals(SESSION_ID, event.getSessionId());
            assertTrue(event.isSuccess());
            assertNotNull(event.getEventTime());
        }
    }

    // ---- markRiskEventHandled ----

    @Nested
    class MarkRiskEventHandledTests {

        @Test
        void markRiskEventHandled_updatesHandledFlag() {
            RiskEventPo event = new RiskEventPo();
            event.setRiskEventId(RISK_EVENT_ID);
            event.setHandledFlag(0);
            when(riskEventRepository.findByRiskEventId(RISK_EVENT_ID)).thenReturn(Optional.of(event));
            when(riskEventRepository.updateByRiskEventId(any())).thenReturn(1);

            service.markRiskEventHandled(RISK_EVENT_ID);

            assertEquals(1, event.getHandledFlag());
            assertNotNull(event.getModifyTime());
            verify(riskEventRepository).updateByRiskEventId(event);
        }

        @Test
        void markRiskEventHandled_throwsWhenNotFound() {
            when(riskEventRepository.findByRiskEventId("unknown")).thenReturn(Optional.empty());

            BusinessException ex = assertThrows(BusinessException.class,
                    () -> service.markRiskEventHandled("unknown"));
            assertEquals(CiamErrorCode.RISK_EVENT_NOT_FOUND, ex.getErrorCode());
            verify(riskEventRepository, never()).updateByRiskEventId(any());
        }
    }
}
