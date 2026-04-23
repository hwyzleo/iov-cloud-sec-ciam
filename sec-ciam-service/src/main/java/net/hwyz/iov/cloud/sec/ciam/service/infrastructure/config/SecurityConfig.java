package net.hwyz.iov.cloud.sec.ciam.service.infrastructure.config;

import net.hwyz.iov.cloud.sec.ciam.service.common.security.CallbackSignatureVerifier;
import net.hwyz.iov.cloud.sec.ciam.service.common.security.FieldEncryptor;
import net.hwyz.iov.cloud.sec.ciam.service.common.security.PasswordEncoder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 安全组件 Spring 配置。
 * <p>
 * 将 {@link FieldEncryptor}、{@link PasswordEncoder} 和 {@link CallbackSignatureVerifier}
 * 注册为 Spring Bean。
 * <ul>
 *   <li>AES-256-GCM 密钥通过 {@code ciam.security.field-encrypt-key} 外部化配置</li>
 *   <li>BCrypt 强度通过 {@code ciam.security.bcrypt-strength} 外部化配置，默认 10</li>
 *   <li>回调签名密钥通过 {@code ciam.security.callback-secret} 外部化配置</li>
 * </ul>
 * <p>
 * 过滤器链执行顺序：
 * <ol>
 *   <li>{@code TraceIdFilter} (HIGHEST_PRECEDENCE) — 追踪 ID 透传</li>
 *   <li>{@code RateLimitFilter} (Order=5) — 限流</li>
 *   <li>{@code IdempotencyFilter} (Order=6) — 幂等</li>
 *   <li>{@code JwtAuthenticationFilter} (Order=10) — JWT 鉴权</li>
 * </ol>
 */
@Configuration
public class SecurityConfig {

    /**
     * 创建 {@link FieldEncryptor} Bean。
     */
    @Bean
    public FieldEncryptor fieldEncryptor(
            @Value("${ciam.security.field-encrypt-key}") String aesKeyBase64) {
        return new FieldEncryptor(aesKeyBase64);
    }

    /**
     * 创建 {@link PasswordEncoder} Bean。
     */
    @Bean
    public PasswordEncoder ciamPasswordEncoder(
            @Value("${ciam.security.bcrypt-strength:10}") int strength) {
        return new PasswordEncoder(strength);
    }

    /**
     * 创建 {@link CallbackSignatureVerifier} Bean。
     * <p>
     * 用于第三方回调接口的 HMAC-SHA256 签名校验、时间戳重放防护和 Nonce 去重。
     */
    @Bean
    public CallbackSignatureVerifier callbackSignatureVerifier(
            @Value("${ciam.security.callback-secret:default-callback-secret}") String secret) {
        return new CallbackSignatureVerifier(secret);
    }
}
