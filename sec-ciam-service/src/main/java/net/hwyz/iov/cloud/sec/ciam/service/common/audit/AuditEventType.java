package net.hwyz.iov.cloud.sec.ciam.service.common.audit;

import lombok.Getter;

/**
 * 审计事件类型枚举。
 * <p>
 * 覆盖注册、登录、验证码、绑定、合并、锁定、注销、同意撤回等关键行为，
 * 与 design.md 模块 12 审计日志范围对齐。
 */
@Getter
public enum AuditEventType {

    // ---- 注册与登录 ----
    REGISTER_SUCCESS("REGISTER", "注册成功"),
    REGISTER_FAIL("REGISTER", "注册失败"),
    LOGIN_SUCCESS("LOGIN", "登录成功"),
    LOGIN_FAIL("LOGIN", "登录失败"),
    LOGOUT("LOGOUT", "退出登录"),

    // ---- 验证码 ----
    VERIFICATION_CODE_SEND("VERIFICATION_CODE", "验证码发送"),
    VERIFICATION_CODE_VERIFY("VERIFICATION_CODE", "验证码校验"),

    // ---- 密码 ----
    PASSWORD_RESET("PASSWORD", "密码找回"),
    PASSWORD_CHANGE("PASSWORD", "密码修改"),

    // ---- MFA ----
    MFA_TRIGGER("MFA", "MFA触发"),
    MFA_VERIFY("MFA", "MFA校验"),

    // ---- 绑定与解绑 ----
    BIND("BIND", "绑定"),
    UNBIND("UNBIND", "解绑"),

    // ---- 合并 ----
    MERGE_APPLY("MERGE", "合并申请"),
    MERGE_REVIEW("MERGE", "合并审核"),
    MERGE_COMPLETE("MERGE", "合并完成"),

    // ---- 账号状态 ----
    ACCOUNT_LOCK("ACCOUNT", "账号锁定"),
    ACCOUNT_UNLOCK("ACCOUNT", "账号解锁"),
    ACCOUNT_DISABLE("ACCOUNT", "账号禁用"),
    ACCOUNT_ENABLE("ACCOUNT", "账号启用"),

    // ---- 注销 ----
    DEACTIVATION_APPLY("DEACTIVATION", "注销申请"),
    DEACTIVATION_REVIEW("DEACTIVATION", "注销审核"),
    DEACTIVATION_COMPLETE("DEACTIVATION", "注销完成"),

    // ---- 同意与合规 ----
    CONSENT_GRANT("CONSENT", "同意授予"),
    CONSENT_WITHDRAW("CONSENT", "同意撤回"),
    DATA_EXPORT_REQUEST("CONSENT", "数据导出请求"),
    DATA_DELETION_REQUEST("CONSENT", "数据删除请求"),

    // ---- 用户资料 ----
    PROFILE_UPDATE("PROFILE", "资料更新"),
    PROFILE_SENSITIVE_UPDATE("PROFILE", "敏感字段更新"),

    // ---- 车主认证 ----
    OWNER_CERT_CALLBACK("OWNER_CERT", "车主认证回调"),
    OWNER_CERT_QUERY("OWNER_CERT", "车主认证查询"),
    OWNER_CERT_COMPENSATE("OWNER_CERT", "车主认证补偿"),

    // ---- 邀请关系 ----
    INVITATION_RECORD("INVITATION", "邀请关系记录"),

    // ---- 风险与安全 ----
    RISK_DETECT("RISK", "风险识别"),
    FORCE_LOGOUT("RISK", "强制下线"),
    FORCE_RE_AUTH("RISK", "强制重认证");

    private final String category;
    private final String description;

    AuditEventType(String category, String description) {
        this.category = category;
        this.description = description;
    }
}
