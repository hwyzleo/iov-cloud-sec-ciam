package net.hwyz.iov.cloud.sec.ciam.service.common.exception;

import lombok.AllArgsConstructor;
import lombok.Getter;
import net.hwyz.iov.cloud.framework.common.exception.ErrorCode;

/**
 * CIAM 统一错误码枚举。
 * <p>
 * 错误码格式：业务域前缀 + 分类编号，便于接口文档与日志追踪。
 */
@Getter
@AllArgsConstructor
public enum CiamErrorCode implements ErrorCode {

    // ---- 通用 ----
    INVALID_PARAM("701001", "参数校验失败"),
    INTERNAL_ERROR("701002", "系统内部错误"),
    RATE_LIMITED("701003", "请求过于频繁，请稍后重试"),
    IDEMPOTENCY_CONFLICT("701004", "重复请求"),
    CALLBACK_SIGNATURE_INVALID("701005", "回调签名校验失败"),

    // ---- 用户与身份 ----
    USER_NOT_FOUND("701101", "用户不存在"),
    IDENTITY_CONFLICT("701102", "登录标识已被其他账号绑定"),
    ACCOUNT_LOCKED("701103", "账号已锁定"),
    ACCOUNT_DISABLED("701104", "账号已禁用"),
    ILLEGAL_STATUS_TRANSITION("701105", "非法的用户状态流转"),

    // ---- 用户资料 ----
    PROFILE_NOT_FOUND("701108", "用户资料不存在"),
    SENSITIVE_FIELD_VERIFICATION_REQUIRED("701109", "敏感字段变更需要安全校验"),

    // ---- 标签 ----
    TAG_ALREADY_EXISTS("701106", "该用户已存在相同标签"),
    TAG_NOT_FOUND("701107", "标签不存在"),

    // ---- 认证与凭据 ----
    CREDENTIAL_INVALID("701201", "凭据校验失败"),
    CREDENTIAL_ALREADY_EXISTS("701205", "该用户已存在有效密码凭据"),
    VERIFICATION_CODE_INVALID("701202", "验证码无效或已过期"),
    VERIFICATION_CODE_RATE_LIMITED("701203", "验证码发送过于频繁"),
    PASSWORD_COMPLEXITY_INSUFFICIENT("701204", "密码复杂度不满足要求"),
    CAPTCHA_INVALID("701206", "图形验证码校验失败"),

    // ---- 会话与授权 ----
    SESSION_NOT_FOUND("701305", "会话不存在"),
    SESSION_EXPIRED("701301", "会话已过期"),
    UNAUTHORIZED("701302", "未认证"),
    FORBIDDEN("701303", "无权限"),
    TOKEN_INVALID("701304", "令牌无效"),

    // ---- OAuth 客户端 ----
    CLIENT_NOT_FOUND("701306", "客户端不存在"),
    CLIENT_DISABLED("701307", "客户端已停用"),
    CLIENT_SECRET_INVALID("701308", "客户端密钥校验失败"),
    REDIRECT_URI_INVALID("701309", "回调地址未注册"),

    // ---- 授权码 ----
    AUTH_CODE_EXPIRED("701310", "授权码已过期"),
    AUTH_CODE_USED("701311", "授权码已被使用"),
    AUTH_CODE_INVALID("701312", "授权码无效"),
    PKCE_CHALLENGE_REQUIRED("701313", "客户端要求 PKCE，但未提供 code_challenge"),
    PKCE_VERIFICATION_FAILED("701314", "PKCE code_verifier 校验失败"),
    AUTH_CODE_CLIENT_MISMATCH("701315", "授权码与客户端不匹配"),
    AUTH_CODE_REDIRECT_MISMATCH("701316", "授权码回调地址不匹配"),
    SCOPE_EXCEEDED("701317", "请求的授权范围超出客户端允许范围"),
    CLIENT_TYPE_NOT_SUPPORTED("701318", "客户端类型不支持该授权方式"),

    // ---- 设备授权 ----
    DEVICE_CODE_NOT_FOUND("701319", "设备授权码不存在或已过期"),
    DEVICE_CODE_PENDING("701320", "授权待确认"),
    DEVICE_CODE_DENIED("701321", "授权已被拒绝"),
    DEVICE_CODE_EXPIRED("701322", "设备授权码已过期"),
    DEVICE_USER_CODE_NOT_FOUND("701323", "用户码不存在或已过期"),

    // ---- 设备 ----
    DEVICE_NOT_FOUND("701324", "设备不存在"),

    // ---- 绑定与合并 ----
    UNBIND_LAST_IDENTITY("701401", "不能解绑最后一种登录方式"),
    MERGE_REQUEST_PENDING("701402", "存在待处理的合并申请"),

    // ---- 邀请关系 ----
    INVITATION_ALREADY_EXISTS("701403", "该用户已存在邀请关系记录"),

    // ---- 注销 ----
    DEACTIVATION_IN_PROGRESS("701501", "注销流程处理中"),
    DEACTIVATION_BLOCKED("701502", "存在未完结业务，无法注销"),

    // ---- MFA ----
    MFA_CHALLENGE_NOT_FOUND("701601", "MFA 挑战不存在"),
    MFA_CHALLENGE_EXPIRED("701602", "MFA 挑战已过期"),
    MFA_CHALLENGE_ALREADY_RESOLVED("701603", "MFA 挑战已结束"),
    MFA_VERIFY_CODE_INVALID("701604", "MFA 验证码无效"),
    MFA_SEND_FAILED("701605", "MFA 验证码发送失败"),

    // ---- 风险 ----
    RISK_EVENT_NOT_FOUND("701610", "风险事件不存在");

    private final String code;
    private final String message;

}
