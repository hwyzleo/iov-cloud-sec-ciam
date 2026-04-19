package net.hwyz.iov.cloud.sec.ciam.service.common.audit;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.time.LocalDateTime;

/**
 * 审计事件基础模型。
 * <p>
 * 用于记录关键操作的审计日志，包含用户、事件类型、操作结果、IP、追踪 ID 等信息。
 * 与 design.md 中 ciam_audit_log 表结构对齐。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuditEvent {

    /** 用户业务唯一标识 */
    private String userId;

    /** 会话 ID */
    private String sessionId;

    /** 设备标识 */
    private String deviceId;

    /** 客户端标识 */
    private String clientId;

    /** 客户端类型 */
    private String clientType;

    /** 事件类型，如 LOGIN, REGISTER, BIND, UNBIND, DEACTIVATION 等 */
    private String eventType;

    /** 事件名称，如 手机号验证码登录、邮箱密码登录 */
    private String eventName;

    /** 操作结果：true-成功，false-失败 */
    private boolean success;

    /** 请求 URI */
    private String requestUri;

    /** 请求方法 */
    private String requestMethod;

    /** 响应码 */
    private String responseCode;

    /** 请求 IP */
    private String ip;

    /** 链路追踪 ID */
    private String traceId;

    /** 请求快照（脱敏后的关键参数） */
    private String requestSnapshot;

    /** 事件时间 */
    private Instant eventTime;
}
