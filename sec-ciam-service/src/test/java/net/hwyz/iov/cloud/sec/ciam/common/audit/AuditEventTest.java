package net.hwyz.iov.cloud.sec.ciam.common.audit;

import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;

import static org.junit.jupiter.api.Assertions.*;

class AuditEventTest {

    @Test
    void builder_setsAllFields() {
        Instant now = ZonedDateTime.of(2024, 6, 1, 12, 0, 0, 0, ZoneId.systemDefault()).toInstant();
        AuditEvent event = AuditEvent.builder()
                .userId("user-001")
                .sessionId("session-001")
                .clientId("client-001")
                .clientType("app")
                .eventType("LOGIN")
                .eventName("手机号验证码登录")
                .success(true)
                .requestUri("/api/v1/auth/login")
                .requestMethod("POST")
                .responseCode("000000")
                .ip("10.0.0.1")
                .traceId("trace-abc")
                .requestSnapshot("{\"phone\":\"138****1234\"}")
                .eventTime(now)
                .build();

        assertEquals("user-001", event.getUserId());
        assertEquals("session-001", event.getSessionId());
        assertEquals("client-001", event.getClientId());
        assertEquals("app", event.getClientType());
        assertEquals("LOGIN", event.getEventType());
        assertEquals("手机号验证码登录", event.getEventName());
        assertTrue(event.isSuccess());
        assertEquals("/api/v1/auth/login", event.getRequestUri());
        assertEquals("POST", event.getRequestMethod());
        assertEquals("000000", event.getResponseCode());
        assertEquals("10.0.0.1", event.getIp());
        assertEquals("trace-abc", event.getTraceId());
        assertEquals("{\"phone\":\"138****1234\"}", event.getRequestSnapshot());
        assertEquals(now, event.getEventTime());
    }

    @Test
    void defaultConstructor_createsEmptyEvent() {
        AuditEvent event = new AuditEvent();
        assertNull(event.getUserId());
        assertNull(event.getEventType());
        assertFalse(event.isSuccess());
    }
}
