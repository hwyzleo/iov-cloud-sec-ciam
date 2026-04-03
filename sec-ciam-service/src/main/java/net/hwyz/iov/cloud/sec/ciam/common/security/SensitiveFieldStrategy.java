package net.hwyz.iov.cloud.sec.ciam.common.security;

/**
 * 敏感字段存储策略常量与文档。
 * <p>
 * 本类集中定义 CIAM 系统中各类字段的安全存储策略，
 * 确保开发团队对"哪些字段加密、哪些字段哈希、哪些字段明文"有统一认知。
 *
 * <h3>一、加密存储 + 哈希查重（原值可恢复）</h3>
 * <p>适用于需要还原展示但又需要唯一性校验的身份标识字段：</p>
 * <ul>
 *   <li>{@code ciam_user_identity.identity_value} — AES-256-GCM 加密存储原值</li>
 *   <li>{@code ciam_user_identity.identity_hash} — SHA-256 哈希，用于唯一查重</li>
 * </ul>
 * <p>涉及标识类型：手机号、邮箱、微信 OpenID/UnionID、Apple Subject、Google Subject、本机手机号认证标识</p>
 *
 * <h3>二、强哈希存储（不可逆）</h3>
 * <p>适用于密码类凭据，永远不落明文：</p>
 * <ul>
 *   <li>{@code ciam_user_credential.credential_hash} — BCrypt 哈希</li>
 *   <li>{@code ciam_oauth_client.client_secret_hash} — BCrypt 哈希</li>
 * </ul>
 *
 * <h3>三、SHA-256 指纹存储（不可逆）</h3>
 * <p>适用于授权码、验证码、Refresh Token 等短期凭据：</p>
 * <ul>
 *   <li>{@code ciam_auth_code.code_hash} — 授权码 SHA-256 指纹</li>
 *   <li>{@code ciam_refresh_token.token_fingerprint} — Refresh Token SHA-256 指纹</li>
 *   <li>{@code ciam_mfa_challenge.verify_code_hash} — 验证码 SHA-256 指纹</li>
 *   <li>{@code ciam_merge_request.conflict_identity_hash} — 冲突标识 SHA-256 哈希</li>
 * </ul>
 *
 * <h3>四、长文本字段存储格式</h3>
 * <ul>
 *   <li>{@code ciam_audit_log.request_snapshot} — JSON 格式（脱敏后的请求上下文快照）</li>
 *   <li>{@code ciam_risk_event.hit_rules} — 逗号分隔字符串，如 {@code "rule_new_device,rule_geo_change"}</li>
 *   <li>{@code ciam_oauth_client.redirect_uris} — JSON 数组，如 {@code ["https://a.com/cb","https://b.com/cb"]}</li>
 *   <li>{@code ciam_oauth_client.scopes} — 空格分隔字符串，如 {@code "openid profile email"}</li>
 *   <li>{@code ciam_oauth_client.grant_types} — 逗号分隔字符串，如 {@code "authorization_code,refresh_token"}</li>
 * </ul>
 */
public final class SensitiveFieldStrategy {

    private SensitiveFieldStrategy() {
    }

    // ========== 存储策略类型 ==========

    /** 加密存储 + 哈希查重 */
    public static final String STRATEGY_ENCRYPT_AND_HASH = "ENCRYPT_AND_HASH";

    /** BCrypt 强哈希（不可逆） */
    public static final String STRATEGY_BCRYPT = "BCRYPT";

    /** SHA-256 指纹（不可逆） */
    public static final String STRATEGY_SHA256_FINGERPRINT = "SHA256_FINGERPRINT";

    /** 明文存储 */
    public static final String STRATEGY_PLAINTEXT = "PLAINTEXT";

    // ========== 长文本存储格式 ==========

    /** JSON 格式 — 审计快照 */
    public static final String FORMAT_JSON = "JSON";

    /** 逗号分隔 — 命中规则、授权类型 */
    public static final String FORMAT_COMMA_SEPARATED = "COMMA_SEPARATED";

    /** JSON 数组 — 回调地址 */
    public static final String FORMAT_JSON_ARRAY = "JSON_ARRAY";

    /** 空格分隔 — OAuth scopes */
    public static final String FORMAT_SPACE_SEPARATED = "SPACE_SEPARATED";

    // ========== 哈希算法标识 ==========

    /** BCrypt 算法标识，写入 credential 表 hash_algorithm 列 */
    public static final String HASH_ALG_BCRYPT = "BCRYPT";

    /** SHA-256 算法标识 */
    public static final String HASH_ALG_SHA256 = "SHA-256";

    /** AES-256-GCM 加密算法标识 */
    public static final String ENCRYPT_ALG_AES256_GCM = "AES-256-GCM";
}
