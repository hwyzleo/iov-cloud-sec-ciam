package net.hwyz.iov.cloud.sec.ciam.service.infrastructure.gateway.mq;

import net.hwyz.iov.cloud.sec.ciam.service.domain.event.DomainEvent;
import net.hwyz.iov.cloud.sec.ciam.service.domain.event.DomainEventType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.kafka.core.KafkaOperations;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class KafkaDomainEventPublisherTest {

    private KafkaOperations<String, String> kafkaOperations;
    private KafkaDomainEventPublisher publisher;

    @SuppressWarnings("unchecked")
    @BeforeEach
    void setUp() {
        kafkaOperations = mock(KafkaOperations.class);
        publisher = new KafkaDomainEventPublisher(kafkaOperations);
    }

    @Test
    void publish_sendsJsonToCorrectTopic() {
        DomainEvent event = DomainEvent.builder()
                .eventType(DomainEventType.USER_REGISTERED)
                .userId("U001")
                .build();

        publisher.publish(event);

        ArgumentCaptor<String> valueCaptor = ArgumentCaptor.forClass(String.class);
        verify(kafkaOperations).send(eq("ciam-domain-events"), eq("U001"), valueCaptor.capture());

        String json = valueCaptor.getValue();
        assertTrue(json.contains("USER_REGISTERED"));
        assertTrue(json.contains("U001"));
        assertTrue(json.contains(event.getEventId()));
    }

    @Test
    void publish_usesEventIdAsKey_whenUserIdIsNull() {
        DomainEvent event = DomainEvent.builder()
                .eventType(DomainEventType.LOGIN_FAILED)
                .userId(null)
                .build();

        publisher.publish(event);

        verify(kafkaOperations).send(eq("ciam-domain-events"), eq(event.getEventId()), anyString());
    }

    @Test
    void publish_nullEvent_doesNotSend() {
        publisher.publish(null);

        verifyNoInteractions(kafkaOperations);
    }

    @Test
    void publish_kafkaException_doesNotThrow() {
        when(kafkaOperations.send(anyString(), anyString(), anyString()))
                .thenThrow(new RuntimeException("Kafka unavailable"));

        DomainEvent event = DomainEvent.builder()
                .eventType(DomainEventType.DEACTIVATION_COMPLETED)
                .userId("U999")
                .build();

        assertDoesNotThrow(() -> publisher.publish(event));
    }

    @Test
    void publish_eventWithPayload_includesPayloadInJson() {
        Map<String, Object> payload = Map.of("identityType", "wechat");
        DomainEvent event = DomainEvent.builder()
                .eventType(DomainEventType.IDENTITY_BOUND)
                .userId("U002")
                .payload(payload)
                .build();

        publisher.publish(event);

        ArgumentCaptor<String> valueCaptor = ArgumentCaptor.forClass(String.class);
        verify(kafkaOperations).send(eq("ciam-domain-events"), eq("U002"), valueCaptor.capture());

        String json = valueCaptor.getValue();
        assertTrue(json.contains("wechat"));
        assertTrue(json.contains("IDENTITY_BOUND"));
    }
}
