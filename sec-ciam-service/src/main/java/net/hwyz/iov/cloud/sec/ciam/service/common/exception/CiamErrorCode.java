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
    SUCCESS("000000", "成功"),
    INVALID_PARAM("100001", "参数校验失败"),
    INTERNAL_ERROR("100002", "系统内部错误"),
    RATE_LIMITED("100003", "请求过于频繁，请稍后重试"),
    IDEMPOTENCY_CONFLICT("100004", "重复请求"),
    CALLBACK_SIGNATURE_INVALID("100005", "回调签名校验失败"),

    // ---- 用户与身份 ----
    USER_NOT_FOUND("200001", "用户不存在"),
    IDENTITY_CONFLICT("200002", "登录标识已被其他账号绑定"),
    ACCOUNT_LOCKED("200003", "账号已锁定"),
    ACCOUNT_DISABLED("200004", "账号已禁用"),
    ILLEGAL_STATUS_TRANSITION("200005", "非法的用户状态流转"),

    // ---- 认证与凭据 ----
    CREDENTIAL_INVALID("300001", "凭据校验失败"),
    CREDENTIAL_ALREADY_EXISTS("300005", "该用户已存在有效密码凭据"),
    VERIFICATION_CODE_INVALID("300002", "验证码无效或已过期"),
    VERIFICATION_CODE_RATE_LIMITED("300003", "验证码发送过于频繁"),
    PASSWORD_COMPLEXITY_INSUFFICIENT("300004", "密码复杂度不满足要求"),
    CAPTCHA_INVALID("300006", "图形验证码校验失败"),

    // ---- 会话与授权 ----
    SESSION_NOT_FOUND("400005", "会话不存在"),
    SESSION_EXPIRED("400001", "会话已过期"),
    UNAUTHORIZED("400002", "未认证"),
    FORBIDDEN("400003", "无权限"),
    TOKEN_INVALID("400004", "令牌无效"),

    // ---- OAuth 客户端 ----
    CLIENT_NOT_FOUND("400006", "客户端不存在"),
    CLIENT_DISABLED("400007", "客户端已停用"),
    CLIENT_SECRET_INVALID("400008", "客户端密钥校验失败"),
    REDIRECT_URI_INVALID("400009", "回调地址未注册"),

    // ---- 授权码 ----
    AUTH_CODE_EXPIRED("400010", "授权码已过期"),
    AUTH_CODE_USED("400011", "授权码已被使用"),
    AUTH_CODE_INVALID("400012", "授权码无效"),
    PKCE_CHALLENGE_REQUIRED("400013", "客户端要求 PKCE，但未提供 code_challenge"),
    PKCE_VERIFICATION_FAILED("400014", "PKCE code_verifier 校验失败"),
    AUTH_CODE_CLIENT_MISMATCH("400015", "授权码与客户端不匹配"),
    AUTH_CODE_REDIRECT_MISMATCH("400016", "授权码回调地址不匹配"),
    SCOPE_EXCEEDED("400017", "请求的授权范围超出客户端允许范围"),
    CLIENT_TYPE_NOT_SUPPORTED("400018", "客户端类型不支持该授权方式"),

    // ---- 设备授权 ----
    DEVICE_CODE_NOT_FOUND("400019", "设备授权码不存在或已过期"),
    DEVICE_CODE_PENDING("400020", "授权待确认"),
    DEVICE_CODE_DENIED("400021", "授权已被拒绝"),
    DEVICE_CODE_EXPIRED("400022", "设备授权码已过期"),
    DEVICE_USER_CODE_NOT_FOUND("400023", "用户码不存在或已过期"),

    // ---- 设备 ----
    DEVICE_NOT_FOUND("400024", "设备不存在"),

    // ---- 绑定与合并 ----
    UNBIND_LAST_IDENTITY("500001", "不能解绑最后一种登录方式"),
    MERGE_REQUEST_PENDING("500002", "存在待处理的合并申请"),

    // ---- 用户资料 ----
    PROFILE_NOT_FOUND("200008", "用户资料不存在"),
    SENSITIVE_FIELD_VERIFICATION_REQUIRED("200009", "敏感字段变更需要安全校验"),

    // ---- 标签 ----
    TAG_ALREADY_EXISTS("200006", "该用户已存在相同标签"),
    TAG_NOT_FOUND("200007", "标签不存在"),

    // ---- MFA ----
    MFA_CHALLENGE_NOT_FOUND("700001", "MFA 挑战不存在"),
    MFA_CHALLENGE_EXPIRED("700002", "MFA 挑战已过期"),
    MFA_CHALLENGE_ALREADY_RESOLVED("700003", "MFA 挑战已结束"),
    MFA_VERIFY_CODE_INVALID("700004", "MFA 验证码无效"),
    MFA_SEND_FAILED("700005", "MFA 验证码发送失败"),

    // ---- 风险 ----
    RISK_EVENT_NOT_FOUND("700010", "风险事件不存在"),

    // ---- 邀请关系 ----
    INVITATION_ALREADY_EXISTS("500003", "该用户已存在邀请关系记录"),

    // ---- 注销 ----
    DEACTIVATION_IN_PROGRESS("600001", "注销流程处理中"),
    DEACTIVATION_BLOCKED("600002", "存在未完结业务，无法注销");

    private final String code;
    private final String message;

}
