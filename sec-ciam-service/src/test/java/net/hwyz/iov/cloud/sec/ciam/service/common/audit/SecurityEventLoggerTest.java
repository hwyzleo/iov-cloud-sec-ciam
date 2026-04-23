package net.hwyz.iov.cloud.sec.ciam.service.common.audit;

import net.hwyz.iov.cloud.sec.ciam.service.domain.model.RiskEvent;
import net.hwyz.iov.cloud.sec.ciam.service.domain.repository.RiskEventRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class SecurityEventLoggerTest {

    private RiskEventRepository riskEventRepository;
    private SecurityEventLogger logger;

    @BeforeEach
    void setUp() {
        riskEventRepository = mock(RiskEventRepository.class);
        when(riskEventRepository.insert(any())).thenReturn(1);
        logger = new SecurityEventLogger(riskEventRepository);
    }

    @Nested
    class LogWithSecurityEvent {

        @Test
        void log_mfaTriggerEvent_persistsToDatabase() {
            SecurityEvent event = buildMfaTriggerEvent();

            logger.log(event);

            ArgumentCaptor<RiskEvent> captor = ArgumentCaptor.forClass(RiskEvent.class);
            verify(riskEventRepository).insert(captor.capture());
            RiskEvent saved = captor.getValue();

            assertEquals("user-001", saved.getUserId());
            assertEquals("session-001", saved.getSessionId());
            assertEquals("device-001", saved.getDeviceId());
            assertEquals("login", saved.getRiskScene());
            assertEquals("MFA_TRIGGER", saved.getRiskType());
            assertEquals(1, saved.getRiskLevel());
            assertEquals("app", saved.getClientType());
            assertEquals("10.0.0.1", saved.getIpAddress());
            assertEquals("CN", saved.getRegionCode());
            assertEquals("challenge", saved.getDecisionResult());
            assertEquals("new_device", saved.getHitRules());
            assertEquals("新设备登录触发MFA", saved.getDescription());
            assertEquals(0, saved.getHandledFlag());
            assertNotNull(saved.getRiskEventId());
            assertEquals(32, saved.getRiskEventId().length());
        }

        @Test
        void log_abnormalLoginEvent_persistsCorrectly() {
            SecurityEvent event = SecurityEvent.builder()
                    .eventType("ABNORMAL_LOGIN")
                    .userId("user-002")
                    .riskScene("login")
                    .riskLevel(2)
                    .ipAddress("192.168.1.100")
                    .regionCode("US")
                    .decisionResult("block")
                    .hitRules("new_device_and_geo_change")
                    .detail("异地新设备登录")
                    .build();

            logger.log(event);

            ArgumentCaptor<RiskEvent> captor = ArgumentCaptor.forClass(RiskEvent.class);
            verify(riskEventRepository).insert(captor.capture());
            RiskEvent saved = captor.getValue();

            assertEquals("ABNORMAL_LOGIN", saved.getRiskType());
            assertEquals("login", saved.getRiskScene());
            assertEquals(2, saved.getRiskLevel());
            assertEquals("block", saved.getDecisionResult());
        }

        @Test
        void log_codeAntiAbuseEvent_persistsCorrectly() {
            SecurityEvent event = SecurityEvent.builder()
                    .eventType("CODE_ANTI_ABUSE")
                    .userId("user-003")
                    .riskScene("code_verify")
                    .riskLevel(1)
                    .ipAddress("172.16.0.1")
                    .decisionResult("challenge")
                    .detail("验证码发送频率超限")
                    .build();

            logger.log(event);

            ArgumentCaptor<RiskEvent> captor = ArgumentCaptor.forClass(RiskEvent.class);
            verify(riskEventRepository).insert(captor.capture());
            RiskEvent saved = captor.getValue();

            assertEquals("CODE_ANTI_ABUSE", saved.getRiskType());
            assertEquals("code_verify", saved.getRiskScene());
        }

        @Test
        void log_highRiskDispositionEvent_persistsCorrectly() {
            SecurityEvent event = SecurityEvent.builder()
                    .eventType("HIGH_RISK_DISPOSITION")
                    .userId("user-004")
                    .sessionId("session-004")
                    .riskScene("login")
                    .riskLevel(2)
                    .decisionResult("kickout")
                    .hitRules("new_device_and_geo_change")
                    .detail("高风险处置：强制下线所有会话")
                    .build();

            logger.log(event);

            ArgumentCaptor<RiskEvent> captor = ArgumentCaptor.forClass(RiskEvent.class);
            verify(riskEventRepository).insert(captor.capture());
            RiskEvent saved = captor.getValue();

            assertEquals("HIGH_RISK_DISPOSITION", saved.getRiskType());
            assertEquals("kickout", saved.getDecisionResult());
        }

        @Test
        void log_nullEvent_doesNotPersist() {
            logger.log((SecurityEvent) null);

            verify(riskEventRepository, never()).insert(any());
        }

        @Test
        void log_dbFailure_fallsBackToSlf4jOnly() {
            when(riskEventRepository.insert(any())).thenThrow(new RuntimeException("DB connection lost"));
            SecurityEvent event = buildMfaTriggerEvent();

            assertDoesNotThrow(() -> logger.log(event));

            verify(riskEventRepository).insert(any());
        }

        @Test
        void log_eventWithNullEventTime_usesCurrentTime() {
            SecurityEvent event = SecurityEvent.builder()
                    .eventType("MFA_TRIGGER")
                    .userId("user-005")
                    .build();

            logger.log(event);

            ArgumentCaptor<RiskEvent> captor = ArgumentCaptor.forClass(RiskEvent.class);
            verify(riskEventRepository).insert(captor.capture());
            assertNotNull(captor.getValue().getEventTime());
        }

        @Test
        void log_eventWithNullRiskScene_usesEventTypeAsScene() {
            SecurityEvent event = SecurityEvent.builder()
                    .eventType("MFA_TRIGGER")
                    .build();

            logger.log(event);

            ArgumentCaptor<RiskEvent> captor = ArgumentCaptor.forClass(RiskEvent.class);
            verify(riskEventRepository).insert(captor.capture());
            assertEquals("MFA_TRIGGER", captor.getValue().getRiskScene());
        }

        @Test
        void log_eventWithNullRiskLevel_defaultsToZero() {
            SecurityEvent event = SecurityEvent.builder()
                    .eventType("MFA_TRIGGER")
                    .build();

            logger.log(event);

            ArgumentCaptor<RiskEvent> captor = ArgumentCaptor.forClass(RiskEvent.class);
            verify(riskEventRepository).insert(captor.capture());
            assertEquals(0, captor.getValue().getRiskLevel());
        }

        @Test
        void log_eventWithNullDecisionResult_defaultsToLog() {
            SecurityEvent event = SecurityEvent.builder()
                    .eventType("MFA_TRIGGER")
                    .build();

            logger.log(event);

            ArgumentCaptor<RiskEvent> captor = ArgumentCaptor.forClass(RiskEvent.class);
            verify(riskEventRepository).insert(captor.capture());
            assertEquals("log", captor.getValue().getDecisionResult());
        }
    }

    @Nested
    class LogWithSimpleParams {

        @Test
        void log_simpleParams_persistsToDatabase() {
            logger.log("ACCOUNT_LOCK", "user-010", "10.0.0.1", "trace-abc", "管理员锁定账号");

            ArgumentCaptor<RiskEvent> captor = ArgumentCaptor.forClass(RiskEvent.class);
            verify(riskEventRepository).insert(captor.capture());
            RiskEvent saved = captor.getValue();

            assertEquals("user-010", saved.getUserId());
            assertEquals("ACCOUNT_LOCK", saved.getRiskType());
            assertEquals("ACCOUNT_LOCK", saved.getRiskScene());
            assertEquals("10.0.0.1", saved.getIpAddress());
            assertEquals("管理员锁定账号", saved.getDescription());
        }

        @Test
        void log_simpleParams_dbFailure_doesNotThrow() {
            when(riskEventRepository.insert(any())).thenThrow(new RuntimeException("DB error"));

            assertDoesNotThrow(() ->
                    logger.log("ACCOUNT_LOCK", "user-010", "10.0.0.1", "trace-abc", "管理员锁定账号"));
        }
    }

    @Nested
    class NoRepositoryFallback {

        @Test
        void log_withoutRepository_doesNotThrow() {
            SecurityEventLogger noRepoLogger = new SecurityEventLogger();
            SecurityEvent event = buildMfaTriggerEvent();

            assertDoesNotThrow(() -> noRepoLogger.log(event));
        }

        @Test
        void log_simpleParams_withoutRepository_doesNotThrow() {
            SecurityEventLogger noRepoLogger = new SecurityEventLogger();

            assertDoesNotThrow(() ->
                    noRepoLogger.log("MFA_TRIGGER", "user-001", "10.0.0.1", "trace-abc", "MFA触发"));
        }
    }

    @Nested
    class MapToDomainModel {

        @Test
        void mapsAllFieldsCorrectly() {
            Instant eventTime = ZonedDateTime.of(2024, 6, 1, 12, 0, 0, 0, ZoneId.systemDefault()).toInstant();
            SecurityEvent event = SecurityEvent.builder()
                    .eventType("MFA_TRIGGER")
                    .userId("user-100")
                    .sessionId("session-100")
                    .deviceId("device-100")
                    .riskScene("login")
                    .riskLevel(1)
                    .clientType("web")
                    .ipAddress("192.168.1.100")
                    .regionCode("CN")
                    .decisionResult("challenge")
                    .hitRules("new_device")
                    .eventTime(eventTime)
                    .detail("新设备登录触发MFA")
                    .build();

            RiskEvent result = logger.mapToDomainModel(event);

            assertEquals("user-100", result.getUserId());
            assertEquals("session-100", result.getSessionId());
            assertEquals("device-100", result.getDeviceId());
            assertEquals("login", result.getRiskScene());
            assertEquals("MFA_TRIGGER", result.getRiskType());
            assertEquals(1, result.getRiskLevel());
            assertEquals("web", result.getClientType());
            assertEquals("192.168.1.100", result.getIpAddress());
            assertEquals("CN", result.getRegionCode());
            assertEquals("challenge", result.getDecisionResult());
            assertEquals("new_device", result.getHitRules());
            assertEquals(eventTime, result.getEventTime());
            assertEquals("新设备登录触发MFA", result.getDescription());
            assertNotNull(result.getRiskEventId());
            assertEquals(32, result.getRiskEventId().length());
            assertEquals(0, result.getHandledFlag());
        }

        @Test
        void mapsNullOptionalFieldsGracefully() {
            SecurityEvent event = SecurityEvent.builder()
                    .eventType("MFA_TRIGGER")
                    .build();

            RiskEvent result = logger.mapToDomainModel(event);

            assertNull(result.getUserId());
            assertNull(result.getSessionId());
            assertNull(result.getDeviceId());
            assertNull(result.getClientType());
            assertNull(result.getIpAddress());
            assertNull(result.getRegionCode());
            assertNull(result.getHitRules());
            assertEquals("MFA_TRIGGER", result.getRiskType());
            assertEquals("MFA_TRIGGER", result.getRiskScene());
            assertEquals(0, result.getRiskLevel());
            assertEquals("log", result.getDecisionResult());
        }
    }

    private SecurityEvent buildMfaTriggerEvent() {
        return SecurityEvent.builder()
                .eventType("MFA_TRIGGER")
                .userId("user-001")
                .sessionId("session-001")
                .deviceId("device-001")
                .riskScene("login")
                .riskLevel(1)
                .clientType("app")
                .ipAddress("10.0.0.1")
                .regionCode("CN")
                .decisionResult("challenge")
                .hitRules("new_device")
                .eventTime(ZonedDateTime.of(2024, 6, 1, 12, 0, 0, 0, ZoneId.systemDefault()).toInstant())
                .traceId("trace-001")
                .detail("新设备登录触发MFA")
                .build();
    }
}
