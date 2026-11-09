package net.hwyz.iov.cloud.sec.ciam.service.common.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.hwyz.iov.cloud.framework.web.context.SecurityContextHolder;
import net.hwyz.iov.cloud.sec.ciam.service.domain.model.Session;
import net.hwyz.iov.cloud.sec.ciam.service.domain.service.JwtTokenService;
import net.hwyz.iov.cloud.sec.ciam.service.domain.service.SessionDomainService;
import net.hwyz.iov.cloud.sec.ciam.service.domain.service.TokenClaims;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

/**
 * JWT 鉴权过滤器。
 * <p>
 * 从 Authorization 请求头提取 Bearer Token，校验后将用户、客户端、会话与设备上下文写入请求属性，
 * 供下游控制器和服务使用。
 * <p>
 * SEC-CIAM-DSN-CR-003 §5.4 / §9.1 要求：
 * <ul>
 *   <li>公开白名单精确到端点，不得用 {@code /api/mobile/auth/v1/**} 整体豁免；</li>
 *   <li>受保护的 {@code /api/mobile/**} 请求在 JWT 校验后必须校验持久会话状态与声明绑定；</li>
 *   <li>保留 {@code SecurityContextHolder.userId/userKey} 设置与 finally 清理；</li>
 *   <li>不对范围外 OAuth、车端和 client-credentials 流程伪造会话要求。</li>
 * </ul>
 */
@Slf4j
@Component
@Order(10)
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    static final String AUTHORIZATION_HEADER = "Authorization";
    static final String BEARER_PREFIX = "Bearer ";
    public static final String ATTR_USER_ID = "ciam.userId";
    public static final String ATTR_CLIENT_ID = "ciam.clientId";
    public static final String ATTR_CLIENT_TYPE = "ciam.clientType";
    public static final String ATTR_DEVICE_ID = "ciam.deviceId";
    public static final String ATTR_SESSION_ID = "ciam.sessionId";

    /**
     * 公开白名单（SEC-CIAM-DSN-CR-003 §9.1）：仅验证码发送、登录和 Token 刷新可匿名访问。
     * 其他既有公开路径（JWKS、OAuth token/device、service、mpt、vehicle、文档）不因本 CR 扩大或缩小。
     */
    private static final List<String> PUBLIC_PATTERNS = List.of(
            "/api/mobile/auth/v1/sms/send",
            "/api/mobile/auth/v1/email/send",
            "/api/mobile/auth/v1/login/**",
            "/api/mobile/auth/v1/token/refresh",
            "/api/open/oidc/v1/.well-known/**",
            "/api/open/oidc/v1/jwks",
            "/api/open/oauth/v1/token",
            "/api/open/oauth/v1/device",
            "/api/vehicle/auth/v1/**",
            "/api/service/**",
            "/api/mpt/**",
            "/swagger-ui/**",
            "/swagger-ui.html",
            "/v3/api-docs/**",
            "/swagger-resources/**",
            "/webjars/**"
    );

    /** 需要会话状态与声明绑定校验的受保护前缀 */
    private static final List<String> MOBILE_PROTECTED_PATTERNS = List.of("/api/mobile/**");

    private final JwtTokenService jwtTokenService;
    private final SessionDomainService sessionDomainService;
    private final AntPathMatcher pathMatcher = new AntPathMatcher();

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String path = request.getRequestURI();

        if (isPublicEndpoint(path)) {
            filterChain.doFilter(request, response);
            return;
        }

        String header = request.getHeader(AUTHORIZATION_HEADER);
        if (header == null || !header.startsWith(BEARER_PREFIX)) {
            sendUnauthorized(response, "缺少 Authorization 头");
            return;
        }

        String token = header.substring(BEARER_PREFIX.length()).trim();
        if (token.isEmpty()) {
            sendUnauthorized(response, "Bearer Token 为空");
            return;
        }

        try {
            TokenClaims claims = jwtTokenService.validateAccessToken(token);

            if (requiresMobileSession(path)) {
                // 受保护手机端点：会话必须存在、ACTIVE、未到期，且声明与会话完全一致
                Session session = sessionDomainService.requireActiveSession(claims.getSessionId());
                sessionDomainService.assertClaimBinding(session,
                        claims.getSub(), claims.getClientId(), claims.getClientType(), claims.getDeviceId());

                request.setAttribute(ATTR_CLIENT_TYPE, claims.getClientType());
                request.setAttribute(ATTR_DEVICE_ID, claims.getDeviceId());
                request.setAttribute(ATTR_SESSION_ID, claims.getSessionId());
            }

            request.setAttribute(ATTR_USER_ID, claims.getSub());
            request.setAttribute(ATTR_CLIENT_ID, claims.getClientId());

            SecurityContextHolder.setUserId(claims.getSub());
            SecurityContextHolder.setUserKey(claims.getClientId());

            try {
                filterChain.doFilter(request, response);
            } finally {
                SecurityContextHolder.remove();
            }
        } catch (Exception e) {
            log.warn("JWT 校验失败: path={}, error={}", path, e.getMessage());
            sendUnauthorized(response, "令牌无效");
        }
    }

    boolean isPublicEndpoint(String path) {
        return PUBLIC_PATTERNS.stream().anyMatch(pattern -> pathMatcher.match(pattern, path));
    }

    boolean requiresMobileSession(String path) {
        return MOBILE_PROTECTED_PATTERNS.stream().anyMatch(pattern -> pathMatcher.match(pattern, path));
    }

    private void sendUnauthorized(HttpServletResponse response, String message) throws IOException {
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType("application/json;charset=UTF-8");
        response.getWriter().write(
                "{\"code\":\"400002\",\"message\":\"" + message + "\"}"
        );
    }
}
