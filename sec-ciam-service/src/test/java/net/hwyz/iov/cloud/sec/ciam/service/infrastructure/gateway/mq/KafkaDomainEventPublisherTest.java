package net.hwyz.iov.cloud.sec.ciam.service.infrastructure.gateway.mq;

import com.fasterxml.jackson.databind.ObjectMapper;
import net.hwyz.iov.cloud.framework.web.jackson.IovEpochMillisWriteModule;
import net.hwyz.iov.cloud.sec.ciam.service.domain.event.DomainEvent;
import net.hwyz.iov.cloud.sec.ciam.service.domain.event.DomainEventType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.kafka.core.KafkaOperations;

import java.time.ZoneId;
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
        // 注入与生产等效的 mapper：框架 epoch-millis 写出模块（SEC-CIAM-DSN-CR-001）
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.registerModule(new IovEpochMillisWriteModule(ZoneId.of("Asia/Shanghai")));
        publisher = new KafkaDomainEventPublisher(kafkaOperations, objectMapper);
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

    @Test
    void publish_timestamp输出13位epoch毫秒整数_golden() {
        // SEC-CIAM-DSN-CR-001：Instant 事件时间必须为 epoch 毫秒数字，禁止 epoch 秒小数/数组
        DomainEvent event = DomainEvent.builder()
                .eventType(DomainEventType.USER_REGISTERED)
                .userId("U003")
                .build();

        publisher.publish(event);

        ArgumentCaptor<String> valueCaptor = ArgumentCaptor.forClass(String.class);
        verify(kafkaOperations).send(eq("ciam-domain-events"), eq("U003"), valueCaptor.capture());

        String json = valueCaptor.getValue();
        long expectedMillis = event.getTimestamp().toEpochMilli();
        String marker = "\"timestamp\":" + expectedMillis;
        assertTrue(json.contains(marker),
                "timestamp 应为 13 位 epoch 毫秒整数，实际 JSON: " + json);
        // 拒绝秒小数：数字后必须紧跟逗号或右花括号（整数结尾，无小数点）
        int idx = json.indexOf(marker);
        char after = json.charAt(idx + marker.length());
        assertTrue(after == ',' || after == '}', "timestamp 不允许小数尾随，实际: " + json);
    }
}
