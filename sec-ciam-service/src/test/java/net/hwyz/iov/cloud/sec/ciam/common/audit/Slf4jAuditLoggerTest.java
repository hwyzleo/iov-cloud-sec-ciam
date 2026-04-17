package net.hwyz.iov.cloud.sec.ciam.common.audit;

import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;

import static org.junit.jupiter.api.Assertions.*;

class Slf4jAuditLoggerTest {

    private final Slf4jAuditLogger auditLogger = new Slf4jAuditLogger();

    @Test
    void log_validEvent_doesNotThrow() {
        AuditEvent event = AuditEvent.builder()
                .userId("user-001")
                .eventType(AuditEventType.LOGIN_SUCCESS.getCategory())
                .eventName(AuditEventType.LOGIN_SUCCESS.getDescription())
                .success(true)
                .ip("192.168.1.1")
                .traceId("abc123")
                .clientType("app")
                .sessionId("session-001")
                .clientId("client-001")
                .requestUri("/api/v1/auth/login")
                .requestMethod("POST")
                .responseCode("000000")
                .eventTime(ZonedDateTime.of(2024, 6, 1, 12, 0, 0, 0, ZoneId.systemDefault()).toInstant())
                .build();

        assertDoesNotThrow(() -> auditLogger.log(event));
    }

    @Test
    void log_nullEvent_doesNotThrow() {
        assertDoesNotThrow(() -> auditLogger.log(null));
    }

    @Test
    void log_eventWithNullFields_doesNotThrow() {
        AuditEvent event = AuditEvent.builder()
                .eventType("LOGIN")
                .eventName("登录成功")
                .success(true)
                .build();

        assertDoesNotThrow(() -> auditLogger.log(event));
    }
}
