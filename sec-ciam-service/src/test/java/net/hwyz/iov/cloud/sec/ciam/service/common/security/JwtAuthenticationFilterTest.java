package net.hwyz.iov.cloud.sec.ciam.service.common.security;
import net.hwyz.iov.cloud.sec.ciam.service.application.service.*;
import net.hwyz.iov.cloud.sec.ciam.service.domain.adapter.*;

import net.hwyz.iov.cloud.sec.ciam.service.domain.service.JwkDomainService;
import net.hwyz.iov.cloud.sec.ciam.service.domain.service.JwtTokenService;
import net.hwyz.iov.cloud.sec.ciam.service.domain.service.TokenClaims;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class JwtAuthenticationFilterTest {

    private JwtTokenService jwtTokenService;
    private JwtAuthenticationFilter filter;

    @Mock
    private JwkDomainService jwkDomainService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        jwtTokenService = new JwtTokenService(jwkDomainService);
        filter = new JwtAuthenticationFilter(jwtTokenService);
    }

    @Nested
    class PublicEndpoints {

        @Test
        void authEndpoint_skipsAuthentication() throws Exception {
            MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/v1/auth/login");
            MockHttpServletResponse response = new MockHttpServletResponse();
            MockFilterChain chain = new MockFilterChain();

            filter.doFilterInternal(request, response, chain);

            assertEquals(200, response.getStatus());
            assertNotNull(chain.getRequest(), "Filter chain should have been invoked");
        }

        @Test
        void jwksEndpoint_skipsAuthentication() throws Exception {
            MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/oidc/jwks");
            MockHttpServletResponse response = new MockHttpServletResponse();
            MockFilterChain chain = new MockFilterChain();

            filter.doFilterInternal(request, response, chain);

            assertEquals(200, response.getStatus());
            assertNotNull(chain.getRequest());
        }

        @Test
        void oauthTokenEndpoint_skipsAuthentication() throws Exception {
            MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/v1/oauth/token");
            MockHttpServletResponse response = new MockHttpServletResponse();
            MockFilterChain chain = new MockFilterChain();

            filter.doFilterInternal(request, response, chain);

            assertEquals(200, response.getStatus());
            assertNotNull(chain.getRequest());
        }

        @Test
        void deviceEndpoint_skipsAuthentication() throws Exception {
            MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/v1/oauth/device");
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
            MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/account/profile");
            MockHttpServletResponse response = new MockHttpServletResponse();
            MockFilterChain chain = new MockFilterChain();

            filter.doFilterInternal(request, response, chain);

            assertEquals(401, response.getStatus());
            assertNull(chain.getRequest(), "Filter chain should NOT have been invoked");
        }

        @Test
        void invalidBearerPrefix_returns401() throws Exception {
            MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/account/profile");
            request.addHeader("Authorization", "Basic abc123");
            MockHttpServletResponse response = new MockHttpServletResponse();
            MockFilterChain chain = new MockFilterChain();

            filter.doFilterInternal(request, response, chain);

            assertEquals(401, response.getStatus());
        }

        @Test
        void emptyBearerToken_returns401() throws Exception {
            MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/account/profile");
            request.addHeader("Authorization", "Bearer ");
            MockHttpServletResponse response = new MockHttpServletResponse();
            MockFilterChain chain = new MockFilterChain();

            filter.doFilterInternal(request, response, chain);

            assertEquals(401, response.getStatus());
        }

        @Test
        void invalidToken_returns401() throws Exception {
            MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/account/profile");
            request.addHeader("Authorization", "Bearer invalid.jwt.token");
            MockHttpServletResponse response = new MockHttpServletResponse();
            MockFilterChain chain = new MockFilterChain();

            filter.doFilterInternal(request, response, chain);

            assertEquals(401, response.getStatus());
        }

        @Test
        void validToken_setsAttributesAndContinues() throws Exception {
            String token = jwtTokenService.generateAccessToken(
                    "user-001", "client-app", "openid profile", "session-001", 3600);

            MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/account/profile");
            request.addHeader("Authorization", "Bearer " + token);
            MockHttpServletResponse response = new MockHttpServletResponse();
            MockFilterChain chain = new MockFilterChain();

            filter.doFilterInternal(request, response, chain);

            assertEquals(200, response.getStatus());
            assertNotNull(chain.getRequest());
            assertEquals("user-001", request.getAttribute(JwtAuthenticationFilter.ATTR_USER_ID));
            assertEquals("client-app", request.getAttribute(JwtAuthenticationFilter.ATTR_CLIENT_ID));
        }
    }

    @Test
    void isPublicEndpoint_matchesCorrectly() {
        assertTrue(filter.isPublicEndpoint("/api/v1/auth/login"));
        assertTrue(filter.isPublicEndpoint("/api/v1/auth/register"));
        assertTrue(filter.isPublicEndpoint("/api/v1/oidc/.well-known/openid-configuration"));
        assertTrue(filter.isPublicEndpoint("/api/v1/oidc/jwks"));
        assertTrue(filter.isPublicEndpoint("/api/v1/oauth/token"));
        assertTrue(filter.isPublicEndpoint("/api/v1/oauth/device"));

        assertFalse(filter.isPublicEndpoint("/api/v1/account/profile"));
        assertFalse(filter.isPublicEndpoint("/api/v1/admin/users"));
    }
}
