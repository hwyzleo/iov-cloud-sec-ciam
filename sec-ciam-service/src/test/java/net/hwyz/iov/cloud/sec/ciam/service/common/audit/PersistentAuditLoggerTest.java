package net.hwyz.iov.cloud.sec.ciam.service.common.audit;

import net.hwyz.iov.cloud.sec.ciam.service.domain.repository.CiamAuditLogRepository;
import net.hwyz.iov.cloud.sec.ciam.service.infrastructure.persistence.po.AuditLogPo;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class PersistentAuditLoggerTest {

    private CiamAuditLogRepository auditLogRepository;
    private AuditLogger delegateLogger;
    private PersistentAuditLogger persistentAuditLogger;

    @BeforeEach
    void setUp() {
        auditLogRepository = mock(CiamAuditLogRepository.class);
        delegateLogger = mock(AuditLogger.class);
        when(auditLogRepository.insert(any())).thenReturn(1);
        persistentAuditLogger = new PersistentAuditLogger(auditLogRepository, delegateLogger);
    }

    @Nested
    class LogMethod {

        @Test
        void log_validEvent_persistsToDatabase() {
            AuditEvent event = buildLoginEvent(true);

            persistentAuditLogger.log(event);

            ArgumentCaptor<AuditLogPo> captor = ArgumentCaptor.forClass(AuditLogPo.class);
            verify(auditLogRepository).insert(captor.capture());
            AuditLogPo saved = captor.getValue();

            assertEquals("user-001", saved.getUserId());
            assertEquals("session-001", saved.getSessionId());
            assertEquals("client-001", saved.getClientId());
            assertEquals("app", saved.getClientType());
            assertEquals("LOGIN", saved.getEventType());
            assertEquals("登录成功", saved.getEventName());
            assertEquals(1, saved.getOperationResult());
            assertEquals("/api/v1/auth/login", saved.getRequestUri());
            assertEquals("POST", saved.getRequestMethod());
            assertEquals("000000", saved.getResponseCode());
            assertEquals("10.0.0.1", saved.getIpAddress());
            assertEquals("trace-abc", saved.getTraceId());
            assertEquals("{\"phone\":\"138****1234\"}", saved.getRequestSnapshot());
            assertNotNull(saved.getAuditId());
            assertEquals(32, saved.getAuditId().length());
            assertEquals(1, saved.getRowValid());
            assertEquals(1, saved.getRowVersion());
        }

        @Test
        void log_validEvent_alsoLogsToSlf4j() {
            AuditEvent event = buildLoginEvent(true);

            persistentAuditLogger.log(event);

            verify(delegateLogger).log(event);
        }

        @Test
        void log_failedEvent_setsOperationResultToZero() {
            AuditEvent event = buildLoginEvent(false);

            persistentAuditLogger.log(event);

            ArgumentCaptor<AuditLogPo> captor = ArgumentCaptor.forClass(AuditLogPo.class);
            verify(auditLogRepository).insert(captor.capture());
            assertEquals(0, captor.getValue().getOperationResult());
        }

        @Test
        void log_nullEvent_doesNotPersistOrLog() {
            persistentAuditLogger.log(null);

            verify(auditLogRepository, never()).insert(any());
            verify(delegateLogger, never()).log(any());
        }

        @Test
        void log_dbFailure_fallsBackToSlf4jOnly() {
            when(auditLogRepository.insert(any())).thenThrow(new RuntimeException("DB connection lost"));
            AuditEvent event = buildLoginEvent(true);

            assertDoesNotThrow(() -> persistentAuditLogger.log(event));

            verify(delegateLogger).log(event);
            verify(auditLogRepository).insert(any());
        }

        @Test
        void log_eventWithNullEventTime_usesCurrentTime() {
            AuditEvent event = AuditEvent.builder()
                    .eventType("LOGIN")
                    .eventName("登录成功")
                    .success(true)
                    .build();

            persistentAuditLogger.log(event);

            ArgumentCaptor<AuditLogPo> captor = ArgumentCaptor.forClass(AuditLogPo.class);
            verify(auditLogRepository).insert(captor.capture());
            assertNotNull(captor.getValue().getEventTime());
        }
    }

    @Nested
    class MapToDataObject {

        @Test
        void mapsAllFieldsCorrectly() {
            Instant eventTime = ZonedDateTime.of(2024, 6, 1, 12, 0, 0, 0, ZoneId.systemDefault()).toInstant();
            AuditEvent event = AuditEvent.builder()
                    .userId("user-002")
                    .sessionId("session-002")
                    .clientId("client-002")
                    .clientType("web")
                    .eventType("REGISTER")
                    .eventName("邮箱注册")
                    .success(true)
                    .requestUri("/api/v1/auth/register")
                    .requestMethod("POST")
                    .responseCode("000000")
                    .ip("192.168.1.100")
                    .traceId("trace-xyz")
                    .requestSnapshot("{\"email\":\"t***@example.com\"}")
                    .eventTime(eventTime)
                    .build();

            AuditLogPo result = persistentAuditLogger.mapToDataObject(event);

            assertEquals("user-002", result.getUserId());
            assertEquals("session-002", result.getSessionId());
            assertEquals("client-002", result.getClientId());
            assertEquals("web", result.getClientType());
            assertEquals("REGISTER", result.getEventType());
            assertEquals("邮箱注册", result.getEventName());
            assertEquals(1, result.getOperationResult());
            assertEquals("/api/v1/auth/register", result.getRequestUri());
            assertEquals("POST", result.getRequestMethod());
            assertEquals("000000", result.getResponseCode());
            assertEquals("192.168.1.100", result.getIpAddress());
            assertEquals("trace-xyz", result.getTraceId());
            assertEquals("{\"email\":\"t***@example.com\"}", result.getRequestSnapshot());
            assertEquals(eventTime, result.getEventTime());
            assertNotNull(result.getAuditId());
            assertNotNull(result.getCreateTime());
            assertEquals(1, result.getRowValid());
            assertEquals(1, result.getRowVersion());
        }

        @Test
        void mapsNullOptionalFieldsGracefully() {
            AuditEvent event = AuditEvent.builder()
                    .eventType("LOGOUT")
                    .eventName("退出登录")
                    .success(true)
                    .build();

            AuditLogPo result = persistentAuditLogger.mapToDataObject(event);

            assertNull(result.getUserId());
            assertNull(result.getSessionId());
            assertNull(result.getClientId());
            assertNull(result.getClientType());
            assertNull(result.getRequestUri());
            assertNull(result.getIpAddress());
            assertEquals("LOGOUT", result.getEventType());
            assertEquals("退出登录", result.getEventName());
        }
    }

    private AuditEvent buildLoginEvent(boolean success) {
        return AuditEvent.builder()
                .userId("user-001")
                .sessionId("session-001")
                .clientId("client-001")
                .clientType("app")
                .eventType("LOGIN")
                .eventName("登录成功")
                .success(success)
                .requestUri("/api/v1/auth/login")
                .requestMethod("POST")
                .responseCode("000000")
                .ip("10.0.0.1")
                .traceId("trace-abc")
                .requestSnapshot("{\"phone\":\"138****1234\"}")
                .eventTime(ZonedDateTime.of(2024, 6, 1, 12, 0, 0, 0, ZoneId.systemDefault()).toInstant())
                .build();
    }
}
