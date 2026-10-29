package net.hwyz.iov.cloud.sec.ciam.service.infrastructure.gateway.mq;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import net.hwyz.iov.cloud.sec.ciam.service.domain.event.DomainEvent;
import net.hwyz.iov.cloud.sec.ciam.service.domain.event.DomainEventPublisher;
import org.springframework.kafka.core.KafkaOperations;
import org.springframework.stereotype.Component;

/**
 * 基于 Kafka 的领域事件发布实现。
 * <p>
 * 将领域事件序列化为 JSON 后发送到 {@code ciam-domain-events} 主题。
 * 如果 Kafka 不可用，仅记录错误日志，不影响业务主流程。
 * <p>
 * SEC-CIAM-DSN-CR-001：注入 Spring 管理的 ObjectMapper（自动应用框架
 * iov.web.jackson 双读 + epoch-millis 写出策略），替换原手工 new ObjectMapper()
 * + JavaTimeModule（默认 WRITE_DATES_AS_TIMESTAMPS 会把 Instant 输出为 epoch
 * 秒小数，属于组织无效契约）。wire 变化：秒小数 → 13 位 epoch 毫秒；已登记。
 */
@Slf4j
@Component
public class KafkaDomainEventPublisher implements DomainEventPublisher {

    static final String TOPIC = "ciam-domain-events";

    private final KafkaOperations<String, String> kafkaOperations;
    private final ObjectMapper objectMapper;

    public KafkaDomainEventPublisher(KafkaOperations<String, String> kafkaOperations, ObjectMapper objectMapper) {
        this.kafkaOperations = kafkaOperations;
        this.objectMapper = objectMapper;
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
