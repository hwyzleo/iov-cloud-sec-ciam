package net.hwyz.iov.cloud.sec.ciam.service.common.security;

import net.hwyz.iov.cloud.framework.web.context.SecurityContextHolder;
import net.hwyz.iov.cloud.sec.ciam.service.domain.model.Session;
import net.hwyz.iov.cloud.sec.ciam.service.domain.service.JwkDomainService;
import net.hwyz.iov.cloud.sec.ciam.service.domain.service.JwtTokenService;
import net.hwyz.iov.cloud.sec.ciam.service.domain.service.SessionDomainService;
import net.hwyz.iov.cloud.sec.ciam.service.domain.service.AccessTokenContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.MockitoAnnotations;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.time.Instant;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * JWT 鉴权过滤器测试（SEC-CIAM-DSN-CR-003 §5.4/§9.1，TC-003-10）。
 * <p>
 * 覆盖：精确公开白名单、受保护手机端点会话状态与声明绑定校验、SecurityContextHolder 清理。
 */
class JwtAuthenticationFilterTest {

    private static final String KID = "kid-test";

    private JwtTokenService jwtTokenService;
    private SessionDomainService sessionDomainService;
    private JwtAuthenticationFilter filter;

    private RSAPrivateKey privateKey;
    private RSAPublicKey publicKey;

    @BeforeEach
    void setUp() throws Exception {
        MockitoAnnotations.openMocks(this);

        KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
        generator.initialize(2048);
        KeyPair keyPair = generator.generateKeyPair();
        privateKey = (RSAPrivateKey) keyPair.getPrivate();
        publicKey = (RSAPublicKey) keyPair.getPublic();

        JwkDomainService jwkDomainService = mock(JwkDomainService.class);
        when(jwkDomainService.getPrimaryPrivateKey()).thenReturn(privateKey);
        when(jwkDomainService.getPrimaryPublicKey()).thenReturn(publicKey);
        when(jwkDomainService.getPublicKeyByKeyId(anyString())).thenReturn(publicKey);
        when(jwkDomainService.getKeyId()).thenReturn(KID);

        jwtTokenService = new JwtTokenService(jwkDomainService);
        sessionDomainService = mock(SessionDomainService.class);
        filter = new JwtAuthenticationFilter(jwtTokenService, sessionDomainService);
    }

    private String mobileToken(String userId, String clientId, String deviceId, String sessionId) {
        return jwtTokenService.generateAccessToken(AccessTokenContext.builder()
                .userId(userId)
                .clientId(clientId)
                .clientType("MOBILE")
                .deviceId(deviceId)
                .scope("openid profile")
                .sessionId(sessionId)
                .ttlSeconds(1800)
                .build());
    }

    private Session session(String userId, String clientId, String deviceId, String sessionId) {
        return Session.builder()
                .sessionId(sessionId)
                .userId(userId)
                .clientId(clientId)
                .clientType("MOBILE")
                .deviceId(deviceId)
                .scope("openid profile")
                .sessionStatus(1)
                .expireTime(Instant.now().plusSeconds(3600))
                .build();
    }

    @Nested
    class PublicEndpoints {

        @Test
        void onlyApprovedMobileAuthEndpointsAreAnonymous() {
            assertTrue(filter.isPublicEndpoint("/api/mobile/auth/v1/sms/send"));
            assertTrue(filter.isPublicEndpoint("/api/mobile/auth/v1/email/send"));
            assertTrue(filter.isPublicEndpoint("/api/mobile/auth/v1/login/mobile"));
            assertTrue(filter.isPublicEndpoint("/api/mobile/auth/v1/login/email-code"));
            assertTrue(filter.isPublicEndpoint("/api/mobile/auth/v1/token/refresh"));
        }

        @Test
        void logoutAndProtectedMobileOperationsAreNotAnonymous() {
            assertFalse(filter.isPublicEndpoint("/api/mobile/auth/v1/logout"));
            assertFalse(filter.isPublicEndpoint("/api/mobile/auth/v1/device/language"));
            assertFalse(filter.isPublicEndpoint("/api/mobile/account/v1/profile"));
            assertFalse(filter.isPublicEndpoint("/api/mobile/risk/v1/events"));
        }

        @Test
        void outOfScopePublicPathsUnchanged() {
            assertTrue(filter.isPublicEndpoint("/api/open/oidc/v1/jwks"));
            assertTrue(filter.isPublicEndpoint("/api/open/oidc/v1/.well-known/openid-configuration"));
            assertTrue(filter.isPublicEndpoint("/api/open/oauth/v1/token"));
            assertTrue(filter.isPublicEndpoint("/api/open/oauth/v1/device"));
            assertTrue(filter.isPublicEndpoint("/api/vehicle/auth/v1/login"));
            assertTrue(filter.isPublicEndpoint("/api/service/session/v1/session/validate"));
            assertTrue(filter.isPublicEndpoint("/api/mpt/account/v1/create"));
            assertTrue(filter.isPublicEndpoint("/swagger-ui/index.html"));
        }

        @Test
        void smsSendEndpoint_skipsAuthentication() throws Exception {
            MockHttpServletRequest request =
                    new MockHttpServletRequest("POST", "/api/mobile/auth/v1/sms/send");
            MockHttpServletResponse response = new MockHttpServletResponse();
            MockFilterChain chain = new MockFilterChain();

            filter.doFilterInternal(request, response, chain);

            assertEquals(200, response.getStatus());
            assertNotNull(chain.getRequest());
        }
    }

    @Nested
    class ProtectedEndpoints {

        @Test
        void missingAuthorizationHeader_returns401() throws Exception {
            MockHttpServletRequest request =
                    new MockHttpServletRequest("POST", "/api/mobile/auth/v1/logout");
            MockHttpServletResponse response = new MockHttpServletResponse();
            MockFilterChain chain = new MockFilterChain();

            filter.doFilterInternal(request, response, chain);

            assertEquals(401, response.getStatus());
            assertNull(chain.getRequest(), "Filter chain should NOT have been invoked");
        }

        @Test
        void invalidBearerPrefix_returns401() throws Exception {
            MockHttpServletRequest request =
                    new MockHttpServletRequest("POST", "/api/mobile/auth/v1/logout");
            request.addHeader("Authorization", "Basic abc123");
            MockHttpServletResponse response = new MockHttpServletResponse();

            filter.doFilterInternal(request, response, new MockFilterChain());

            assertEquals(401, response.getStatus());
        }

        @Test
        void emptyBearerToken_returns401() throws Exception {
            MockHttpServletRequest request =
                    new MockHttpServletRequest("POST", "/api/mobile/auth/v1/logout");
            request.addHeader("Authorization", "Bearer ");
            MockHttpServletResponse response = new MockHttpServletResponse();

            filter.doFilterInternal(request, response, new MockFilterChain());

            assertEquals(401, response.getStatus());
        }

        @Test
        void invalidToken_returns401() throws Exception {
            MockHttpServletRequest request =
                    new MockHttpServletRequest("POST", "/api/mobile/auth/v1/logout");
            request.addHeader("Authorization", "Bearer invalid.jwt.token");
            MockHttpServletResponse response = new MockHttpServletResponse();

            filter.doFilterInternal(request, response, new MockFilterChain());

            assertEquals(401, response.getStatus());
        }

        @Test
        void outOfScopeProtectedPath_doesNotRequireSession() throws Exception {
            String token = jwtTokenService.generateAccessToken(
                    "user-001", null, "client-app", "openid profile", "session-001", 3600);

            MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/other/v1/profile");
            request.addHeader("Authorization", "Bearer " + token);
            MockHttpServletResponse response = new MockHttpServletResponse();
            MockFilterChain chain = new MockFilterChain();

            filter.doFilterInternal(request, response, chain);

            assertEquals(200, response.getStatus());
            assertEquals("user-001", request.getAttribute(JwtAuthenticationFilter.ATTR_USER_ID));
            assertEquals("client-app", request.getAttribute(JwtAuthenticationFilter.ATTR_CLIENT_ID));
        }

        @Test
        void mobileProtectedRequest_requiresActiveSessionAndBindsClaims() throws Exception {
            when(sessionDomainService.requireActiveSession("session-001"))
                    .thenReturn(session("user-001", "ciam-mobile-app", "device-001", "session-001"));

            String token = mobileToken("user-001", "ciam-mobile-app", "device-001", "session-001");
            MockHttpServletRequest request =
                    new MockHttpServletRequest("POST", "/api/mobile/auth/v1/logout");
            request.addHeader("Authorization", "Bearer " + token);
            MockHttpServletResponse response = new MockHttpServletResponse();
            MockFilterChain chain = new MockFilterChain();

            filter.doFilterInternal(request, response, chain);

            assertEquals(200, response.getStatus());
            assertEquals("user-001", request.getAttribute(JwtAuthenticationFilter.ATTR_USER_ID));
            assertEquals("ciam-mobile-app", request.getAttribute(JwtAuthenticationFilter.ATTR_CLIENT_ID));
            assertEquals("MOBILE", request.getAttribute(JwtAuthenticationFilter.ATTR_CLIENT_TYPE));
            assertEquals("device-001", request.getAttribute(JwtAuthenticationFilter.ATTR_DEVICE_ID));
            assertEquals("session-001", request.getAttribute(JwtAuthenticationFilter.ATTR_SESSION_ID));
        }

        @Test
        void mobileProtectedRequest_rejectsInactiveSession() throws Exception {
            when(sessionDomainService.requireActiveSession("session-001"))
                    .thenThrow(new net.hwyz.iov.cloud.framework.common.exception.BusinessException(
                            net.hwyz.iov.cloud.sec.ciam.service.common.exception.CiamErrorCode.TOKEN_INVALID,
                            "会话已非活跃状态"));

            String token = mobileToken("user-001", "ciam-mobile-app", "device-001", "session-001");
            MockHttpServletRequest request =
                    new MockHttpServletRequest("POST", "/api/mobile/auth/v1/logout");
            request.addHeader("Authorization", "Bearer " + token);
            MockHttpServletResponse response = new MockHttpServletResponse();
            MockFilterChain chain = new MockFilterChain();

            filter.doFilterInternal(request, response, chain);

            assertEquals(401, response.getStatus());
            assertNull(chain.getRequest());
        }

        @Test
        void mobileProtectedRequest_rejectsClaimBindingMismatch() throws Exception {
            // 会话中的设备与 Token 声明不一致 -> TOKEN_INVALID
            when(sessionDomainService.requireActiveSession("session-001"))
                    .thenReturn(session("user-001", "ciam-mobile-app", "device-001", "session-001"));
            org.mockito.Mockito.doThrow(new net.hwyz.iov.cloud.framework.common.exception.BusinessException(
                            net.hwyz.iov.cloud.sec.ciam.service.common.exception.CiamErrorCode.TOKEN_INVALID,
                            "令牌与会话绑定不一致"))
                    .when(sessionDomainService)
                    .assertClaimBinding(org.mockito.ArgumentMatchers.any(),
                            org.mockito.ArgumentMatchers.eq("user-001"),
                            org.mockito.ArgumentMatchers.eq("ciam-mobile-app"),
                            org.mockito.ArgumentMatchers.eq("MOBILE"),
                            org.mockito.ArgumentMatchers.eq("device-002"));

            String token = mobileToken("user-001", "ciam-mobile-app", "device-002", "session-001");
            MockHttpServletRequest request =
                    new MockHttpServletRequest("POST", "/api/mobile/auth/v1/logout");
            request.addHeader("Authorization", "Bearer " + token);
            MockHttpServletResponse response = new MockHttpServletResponse();

            filter.doFilterInternal(request, response, new MockFilterChain());

            assertEquals(401, response.getStatus());
        }

        @Test
        void securityContextIsClearedAfterRequest() throws Exception {
            when(sessionDomainService.requireActiveSession("session-001"))
                    .thenReturn(session("user-001", "ciam-mobile-app", "device-001", "session-001"));

            String token = mobileToken("user-001", "ciam-mobile-app", "device-001", "session-001");
            MockHttpServletRequest request =
                    new MockHttpServletRequest("POST", "/api/mobile/auth/v1/logout");
            request.addHeader("Authorization", "Bearer " + token);

            filter.doFilterInternal(request, new MockHttpServletResponse(), new MockFilterChain());

            assertTrue(SecurityContextHolder.getUserId() == null
                    || SecurityContextHolder.getUserId().isEmpty(), "SecurityContextHolder 必须清理");
        }
    }
}
