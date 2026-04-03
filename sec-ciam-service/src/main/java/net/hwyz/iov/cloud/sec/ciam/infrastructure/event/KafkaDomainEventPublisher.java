package net.hwyz.iov.cloud.sec.ciam.infrastructure.event;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import lombok.extern.slf4j.Slf4j;
import net.hwyz.iov.cloud.sec.ciam.domain.event.DomainEvent;
import net.hwyz.iov.cloud.sec.ciam.domain.event.DomainEventPublisher;
import org.springframework.kafka.core.KafkaOperations;
import org.springframework.stereotype.Component;

/**
 * 基于 Kafka 的领域事件发布实现。
 * <p>
 * 将领域事件序列化为 JSON 后发送到 {@code ciam-domain-events} 主题。
 * 如果 Kafka 不可用，仅记录错误日志，不影响业务主流程。
 */
@Slf4j
@Component
public class KafkaDomainEventPublisher implements DomainEventPublisher {

    static final String TOPIC = "ciam-domain-events";

    private final KafkaOperations<String, String> kafkaOperations;
    private final ObjectMapper objectMapper;

    public KafkaDomainEventPublisher(KafkaOperations<String, String> kafkaOperations) {
        this.kafkaOperations = kafkaOperations;
        this.objectMapper = new ObjectMapper();
        this.objectMapper.registerModule(new JavaTimeModule());
    }

    @Override
    public void publish(DomainEvent event) {
        if (event == null) {
            log.warn("[DOMAIN-EVENT] 忽略空事件");
            return;
        }
        try {
            String json = objectMapper.writeValueAsString(event);
            String key = event.getUserId() != null ? event.getUserId() : event.getEventId();
            kafkaOperations.send(TOPIC, key, json);
            log.info("[DOMAIN-EVENT] 发布成功: eventType={}, eventId={}, userId={}",
                    event.getEventType(), event.getEventId(), event.getUserId());
        } catch (JsonProcessingException e) {
            log.error("[DOMAIN-EVENT] 序列化失败: eventType={}, eventId={}, error={}",
                    event.getEventType(), event.getEventId(), e.getMessage(), e);
        } catch (Exception e) {
            log.error("[DOMAIN-EVENT] 发布失败: eventType={}, eventId={}, error={}",
                    event.getEventType(), event.getEventId(), e.getMessage(), e);
        }
    }
}
