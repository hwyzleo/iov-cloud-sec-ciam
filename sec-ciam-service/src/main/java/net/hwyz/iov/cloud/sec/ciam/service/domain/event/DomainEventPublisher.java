package net.hwyz.iov.cloud.sec.ciam.service.domain.event;

/**
 * 领域事件发布接口。
 * <p>
 * 领域层仅依赖此抽象接口，具体实现由基础设施层提供。
 */
public interface DomainEventPublisher {

    /**
     * 发布领域事件。
     *
     * @param event 领域事件
     */
    void publish(DomainEvent event);
}
