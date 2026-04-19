package net.hwyz.iov.cloud.sec.ciam.service.domain.event;

import lombok.Builder;
import lombok.Getter;
import lombok.ToString;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

/**
 * 领域事件基类。
 * <p>
 * 事件体包含事件 ID、事件类型、用户 ID、时间戳、扩展载荷，
 * 与 design.md Kafka 事件设计原则对齐。
 */
@Getter
@Builder
@ToString
public class DomainEvent {

    /** 事件唯一标识 */
    @Builder.Default
    private final String eventId = UUID.randomUUID().toString();

    /** 事件类型 */
    private final DomainEventType eventType;

    /** 用户业务唯一标识 */
    private final String userId;

    /** 事件发生时间 */
    @Builder.Default
    private final Instant timestamp = Instant.now();

    /** 扩展载荷（可选） */
    private final Map<String, Object> payload;
}
