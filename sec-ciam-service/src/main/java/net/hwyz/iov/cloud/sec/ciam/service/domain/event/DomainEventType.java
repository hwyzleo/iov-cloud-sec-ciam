package net.hwyz.iov.cloud.sec.ciam.service.domain.event;

import lombok.Getter;

/**
 * 领域事件类型枚举。
 * <p>
 * 覆盖用户注册、登录、绑定解绑、合并、密码修改、注销、车主认证等核心领域事件，
 * 与 design.md Kafka 事件设计对齐。
 */
@Getter
public enum DomainEventType {

    USER_REGISTERED("用户注册成功"),
    LOGIN_SUCCESS("登录成功"),
    LOGIN_FAILED("登录失败"),
    IDENTITY_BOUND("账号绑定"),
    IDENTITY_UNBOUND("账号解绑"),
    ACCOUNT_MERGED("账号合并完成"),
    PASSWORD_CHANGED("密码修改"),
    DEACTIVATION_REQUESTED("注销申请发起"),
    DEACTIVATION_COMPLETED("注销完成"),
    OWNER_CERT_CHANGED("车主认证状态变更");

    private final String description;

    DomainEventType(String description) {
        this.description = description;
    }
}
