package net.hwyz.iov.cloud.sec.ciam.common.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.hwyz.iov.cloud.framework.web.context.SecurityContextHolder;
import net.hwyz.iov.cloud.sec.ciam.domain.service.JwtTokenService;
import net.hwyz.iov.cloud.sec.ciam.domain.service.TokenClaims;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

/**
 * JWT 鉴权过滤器。
 * <p>
 * 从 Authorization 请求头提取 Bearer Token，校验后将 userId、clientId 写入请求属性，
 * 供下游控制器和服务使用。公开端点跳过鉴权。
 */
@Slf4j
@Component
@Order(10)
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    static final String AUTHORIZATION_HEADER = "Authorization";
    static final String BEARER_PREFIX = "Bearer ";
    static final String ATTR_USER_ID = "ciam.userId";
    static final String ATTR_CLIENT_ID = "ciam.clientId";

    private static final List<String> PUBLIC_PATTERNS = List.of(
            "/api/mobile/auth/v1/**",
            "/api/open/v1/oidc/.well-known/**",
            "/api/open/v1/oauth/token",
            "/api/open/v1/oauth/device",
            "/api/vehicle/v1/auth/**",
            "/api/service/v1/**",
            "/api/mp/admin/v1/**",
            "/swagger-ui/**",
            "/swagger-ui.html",
            "/v3/api-docs/**",
            "/swagger-resources/**",
            "/webjars/**"
    );

    private final JwtTokenService jwtTokenService;
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
            String userId = claims.getSub();
            String clientId = claims.getClientId();

            request.setAttribute(ATTR_USER_ID, userId);
            request.setAttribute(ATTR_CLIENT_ID, clientId);

            SecurityContextHolder.setUserId(userId);
            SecurityContextHolder.setUserKey(clientId);

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

    private void sendUnauthorized(HttpServletResponse response, String message) throws IOException {
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType("application/json;charset=UTF-8");
        response.getWriter().write(
                "{\"code\":\"400002\",\"message\":\"" + message + "\"}"
        );
    }
}
