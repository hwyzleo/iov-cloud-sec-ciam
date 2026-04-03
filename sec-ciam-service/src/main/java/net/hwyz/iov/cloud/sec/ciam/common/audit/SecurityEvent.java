package net.hwyz.iov.cloud.sec.ciam.common.audit;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 安全事件模型。
 * <p>
 * 用于记录 MFA 触发、异常登录、验证码防刷、高风险处置等安全事件，
 * 与 design.md 中 ciam_risk_event 表结构对齐。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SecurityEvent {

    /** 事件类型，如 MFA_TRIGGER, ABNORMAL_LOGIN, CODE_ANTI_ABUSE, HIGH_RISK_DISPOSITION */
    private String eventType;

    /** 用户业务唯一标识 */
    private String userId;

    /** 会话 ID */
    private String sessionId;

    /** 设备 ID */
    private String deviceId;

    /** 风险场景，如 login, mfa, code_verify */
    private String riskScene;

    /** 风险等级：0-低，1-中，2-高 */
    private Integer riskLevel;

    /** 客户端类型 */
    private String clientType;

    /** IP 地址 */
    private String ipAddress;

    /** 地区编码 */
    private String regionCode;

    /** 处置结果：allow, challenge, block, kickout, log */
    private String decisionResult;

    /** 命中规则列表 */
    private String hitRules;

    /** 事件时间 */
    private LocalDateTime eventTime;

    /** 链路追踪 ID */
    private String traceId;

    /** 事件详情 */
    private String detail;
}
