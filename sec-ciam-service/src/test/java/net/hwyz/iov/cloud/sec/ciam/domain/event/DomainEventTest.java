package net.hwyz.iov.cloud.sec.ciam.domain.event;

import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class DomainEventTest {

    @Test
    void builder_generatesEventIdAndTimestampByDefault() {
        DomainEvent event = DomainEvent.builder()
                .eventType(DomainEventType.USER_REGISTERED)
                .userId("U001")
                .build();

        assertNotNull(event.getEventId());
        assertFalse(event.getEventId().isEmpty());
        assertNotNull(event.getTimestamp());
        assertEquals(DomainEventType.USER_REGISTERED, event.getEventType());
        assertEquals("U001", event.getUserId());
        assertNull(event.getPayload());
    }

    @Test
    void builder_acceptsCustomPayload() {
        Map<String, Object> payload = Map.of("ip", "10.0.0.1", "device", "iPhone");
        DomainEvent event = DomainEvent.builder()
                .eventType(DomainEventType.LOGIN_SUCCESS)
                .userId("U002")
                .payload(payload)
                .build();

        assertNotNull(event.getPayload());
        assertEquals("10.0.0.1", event.getPayload().get("ip"));
        assertEquals("iPhone", event.getPayload().get("device"));
    }

    @Test
    void builder_acceptsCustomEventIdAndTimestamp() {
        Instant ts = Instant.parse("2026-01-01T00:00:00Z");
        DomainEvent event = DomainEvent.builder()
                .eventId("custom-id")
                .eventType(DomainEventType.PASSWORD_CHANGED)
                .userId("U003")
                .timestamp(ts)
                .build();

        assertEquals("custom-id", event.getEventId());
        assertEquals(ts, event.getTimestamp());
    }

    @Test
    void twoEvents_haveDifferentEventIds() {
        DomainEvent e1 = DomainEvent.builder().eventType(DomainEventType.LOGIN_FAILED).userId("U1").build();
        DomainEvent e2 = DomainEvent.builder().eventType(DomainEventType.LOGIN_FAILED).userId("U1").build();

        assertNotEquals(e1.getEventId(), e2.getEventId());
    }
}
