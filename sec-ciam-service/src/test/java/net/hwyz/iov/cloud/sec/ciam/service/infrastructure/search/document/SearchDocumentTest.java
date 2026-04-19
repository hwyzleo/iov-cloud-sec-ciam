package net.hwyz.iov.cloud.sec.ciam.service.infrastructure.search.document;

import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;

import static org.junit.jupiter.api.Assertions.*;

class SearchDocumentTest {

    @Test
    void userSearchDocument_builderSetsAllFields() {
        OffsetDateTime now = ZonedDateTime.of(2026, 3, 30, 12, 0, 0, 0, ZoneId.systemDefault()).toOffsetDateTime();
        UserSearchDocument doc = UserSearchDocument.builder()
                .userId("U001")
                .userStatus(1)
                .registerSource("mobile")
                .registerChannel("app_store")
                .lastLoginTime(now)
                .createTime(now.minusDays(30))
                .build();

        assertEquals("U001", doc.getUserId());
        assertEquals(1, doc.getUserStatus());
        assertEquals("mobile", doc.getRegisterSource());
        assertEquals("app_store", doc.getRegisterChannel());
        assertEquals(now, doc.getLastLoginTime());
        assertEquals(now.minusDays(30), doc.getCreateTime());
    }

    @Test
    void auditLogSearchDocument_builderSetsAllFields() {
        OffsetDateTime eventTime = ZonedDateTime.of(2026, 3, 30, 14, 30, 0, 0, ZoneId.systemDefault()).toOffsetDateTime();
        AuditLogSearchDocument doc = AuditLogSearchDocument.builder()
                .auditId("AUD001")
                .userId("U001")
                .eventType("LOGIN")
                .eventName("用户登录")
                .operationResult(1)
                .ipAddress("192.168.1.1")
                .eventTime(eventTime)
                .build();

        assertEquals("AUD001", doc.getAuditId());
        assertEquals("U001", doc.getUserId());
        assertEquals("LOGIN", doc.getEventType());
        assertEquals("用户登录", doc.getEventName());
        assertEquals(1, doc.getOperationResult());
        assertEquals("192.168.1.1", doc.getIpAddress());
        assertEquals(eventTime, doc.getEventTime());
    }

    @Test
    void riskEventSearchDocument_builderSetsAllFields() {
        OffsetDateTime eventTime = ZonedDateTime.of(2026, 3, 30, 15, 0, 0, 0, ZoneId.systemDefault()).toOffsetDateTime();
        RiskEventSearchDocument doc = RiskEventSearchDocument.builder()
                .riskEventId("RISK001")
                .userId("U001")
                .riskScene("login")
                .riskType("brute_force")
                .riskLevel(2)
                .decisionResult("block")
                .eventTime(eventTime)
                .build();

        assertEquals("RISK001", doc.getRiskEventId());
        assertEquals("U001", doc.getUserId());
        assertEquals("login", doc.getRiskScene());
        assertEquals("brute_force", doc.getRiskType());
        assertEquals(2, doc.getRiskLevel());
        assertEquals("block", doc.getDecisionResult());
        assertEquals(eventTime, doc.getEventTime());
    }

    @Test
    void userSearchDocument_noArgsConstructor_defaultsToNull() {
        UserSearchDocument doc = new UserSearchDocument();
        assertNull(doc.getUserId());
        assertNull(doc.getUserStatus());
        assertNull(doc.getRegisterSource());
    }

    @Test
    void auditLogSearchDocument_noArgsConstructor_defaultsToNull() {
        AuditLogSearchDocument doc = new AuditLogSearchDocument();
        assertNull(doc.getAuditId());
        assertNull(doc.getEventType());
    }

    @Test
    void riskEventSearchDocument_noArgsConstructor_defaultsToNull() {
        RiskEventSearchDocument doc = new RiskEventSearchDocument();
        assertNull(doc.getRiskEventId());
        assertNull(doc.getRiskLevel());
    }
}
