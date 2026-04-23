package net.hwyz.iov.cloud.sec.ciam.service.infrastructure.gateway.mq;

import net.hwyz.iov.cloud.sec.ciam.service.domain.event.DomainEvent;
import net.hwyz.iov.cloud.sec.ciam.service.domain.event.DomainEventType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class StubDomainEventPublisherTest {

    private StubDomainEventPublisher publisher;

    @BeforeEach
    void setUp() {
        publisher = new StubDomainEventPublisher();
    }

    @Test
    void publish_storesEventInMemory() {
        DomainEvent event = DomainEvent.builder()
                .eventType(DomainEventType.USER_REGISTERED)
                .userId("U001")
                .build();

        publisher.publish(event);

        assertEquals(1, publisher.getPublishedEvents().size());
        assertEquals(DomainEventType.USER_REGISTERED, publisher.getPublishedEvents().get(0).getEventType());
        assertEquals("U001", publisher.getPublishedEvents().get(0).getUserId());
    }

    @Test
    void publish_multipleEvents_allStored() {
        publisher.publish(DomainEvent.builder().eventType(DomainEventType.LOGIN_SUCCESS).userId("U1").build());
        publisher.publish(DomainEvent.builder().eventType(DomainEventType.LOGIN_FAILED).userId("U2").build());
        publisher.publish(DomainEvent.builder().eventType(DomainEventType.IDENTITY_BOUND).userId("U1").build());

        assertEquals(3, publisher.getPublishedEvents().size());
        assertEquals(DomainEventType.LOGIN_SUCCESS, publisher.getPublishedEvents().get(0).getEventType());
        assertEquals(DomainEventType.LOGIN_FAILED, publisher.getPublishedEvents().get(1).getEventType());
        assertEquals(DomainEventType.IDENTITY_BOUND, publisher.getPublishedEvents().get(2).getEventType());
    }

    @Test
    void publish_nullEvent_doesNotThrow() {
        assertDoesNotThrow(() -> publisher.publish(null));
        assertTrue(publisher.getPublishedEvents().isEmpty());
    }

    @Test
    void clear_removesAllEvents() {
        publisher.publish(DomainEvent.builder().eventType(DomainEventType.PASSWORD_CHANGED).userId("U1").build());
        publisher.publish(DomainEvent.builder().eventType(DomainEventType.ACCOUNT_MERGED).userId("U2").build());
        assertEquals(2, publisher.getPublishedEvents().size());

        publisher.clear();

        assertTrue(publisher.getPublishedEvents().isEmpty());
    }

    @Test
    void getPublishedEvents_returnsUnmodifiableList() {
        publisher.publish(DomainEvent.builder().eventType(DomainEventType.DEACTIVATION_REQUESTED).userId("U1").build());

        assertThrows(UnsupportedOperationException.class, () ->
                publisher.getPublishedEvents().add(
                        DomainEvent.builder().eventType(DomainEventType.LOGIN_SUCCESS).userId("U2").build()));
    }

    @Test
    void publish_eventWithPayload_preservesPayload() {
        Map<String, Object> payload = Map.of("certStatus", "VERIFIED", "vin", "VIN123");
        DomainEvent event = DomainEvent.builder()
                .eventType(DomainEventType.OWNER_CERT_CHANGED)
                .userId("U001")
                .payload(payload)
                .build();

        publisher.publish(event);

        DomainEvent stored = publisher.getPublishedEvents().get(0);
        assertEquals("VERIFIED", stored.getPayload().get("certStatus"));
        assertEquals("VIN123", stored.getPayload().get("vin"));
    }
}
