package net.hwyz.iov.cloud.sec.ciam.service.infrastructure.gateway.mq;

import lombok.extern.slf4j.Slf4j;
import net.hwyz.iov.cloud.sec.ciam.service.domain.event.DomainEvent;
import net.hwyz.iov.cloud.sec.ciam.service.domain.event.DomainEventPublisher;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 领域事件发布 — 桩实现（测试与开发环境使用）。
 * <p>
 * 将事件存储在内存列表中，便于单元测试断言。
 * 不发送到任何外部消息中间件。
 */
@Slf4j
public class StubDomainEventPublisher implements DomainEventPublisher {

    private final List<DomainEvent> publishedEvents = new ArrayList<>();

    @Override
    public void publish(DomainEvent event) {
        if (event == null) {
            log.warn("[DOMAIN-EVENT-STUB] 忽略空事件");
            return;
        }
        publishedEvents.add(event);
        log.info("[DOMAIN-EVENT-STUB] 记录事件: eventType={}, eventId={}, userId={}",
                event.getEventType(), event.getEventId(), event.getUserId());
    }

    /** 获取已发布事件的不可变视图 */
    public List<DomainEvent> getPublishedEvents() {
        return Collections.unmodifiableList(publishedEvents);
    }

    /** 清空已发布事件（测试辅助） */
    public void clear() {
        publishedEvents.clear();
    }
}
