package net.hwyz.iov.cloud.sec.ciam.service.common.security;
import net.hwyz.iov.cloud.sec.ciam.service.application.service.*;
import net.hwyz.iov.cloud.sec.ciam.service.domain.adapter.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import static org.junit.jupiter.api.Assertions.*;

class RateLimitFilterTest {

    private RateLimitFilter filter;

    @BeforeEach
    void setUp() {
        filter = new RateLimitFilter();
        filter.clearCounters();
    }

    @Test
    void generalEndpoint_allowsUpToDefaultLimit() {
        String key = "127.0.0.1:general";
        for (int i = 0; i < RateLimitFilter.DEFAULT_LIMIT; i++) {
            assertTrue(filter.tryAcquire(key, RateLimitFilter.DEFAULT_LIMIT),
                    "Request " + (i + 1) + " should be allowed");
        }
        assertFalse(filter.tryAcquire(key, RateLimitFilter.DEFAULT_LIMIT),
                "Request beyond limit should be rejected");
    }

    @Test
    void authEndpoint_allowsUpToAuthLimit() {
        String key = "127.0.0.1:auth";
        for (int i = 0; i < RateLimitFilter.AUTH_LIMIT; i++) {
            assertTrue(filter.tryAcquire(key, RateLimitFilter.AUTH_LIMIT));
        }
        assertFalse(filter.tryAcquire(key, RateLimitFilter.AUTH_LIMIT));
    }

    @Test
    void isAuthEndpoint_matchesAuthPaths() {
        assertTrue(filter.isAuthEndpoint("/api/v1/auth/login"));
        assertTrue(filter.isAuthEndpoint("/api/v1/auth/register"));
        assertTrue(filter.isAuthEndpoint("/api/v1/oauth/token"));

        assertFalse(filter.isAuthEndpoint("/api/v1/account/profile"));
        assertFalse(filter.isAuthEndpoint("/api/v1/admin/users"));
    }

    @Test
    void resolveLimit_returnsCorrectLimits() {
        assertEquals(RateLimitFilter.AUTH_LIMIT, filter.resolveLimit("/api/v1/auth/login"));
        assertEquals(RateLimitFilter.DEFAULT_LIMIT, filter.resolveLimit("/api/v1/account/profile"));
    }

    @Test
    void exceededLimit_returns429() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/v1/auth/login");
        request.setRemoteAddr("10.0.0.1");

        // Exhaust the limit
        for (int i = 0; i < RateLimitFilter.AUTH_LIMIT; i++) {
            MockHttpServletResponse resp = new MockHttpServletResponse();
            filter.doFilterInternal(request, resp, new MockFilterChain());
            assertEquals(200, resp.getStatus());
        }

        // Next request should be rate limited
        MockHttpServletResponse response = new MockHttpServletResponse();
        filter.doFilterInternal(request, response, new MockFilterChain());
        assertEquals(429, response.getStatus());
        assertTrue(response.getContentAsString().contains("100003"));
    }

    @Test
    void getClientIp_prefersXForwardedFor() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("X-Forwarded-For", "1.2.3.4, 5.6.7.8");
        request.setRemoteAddr("127.0.0.1");

        assertEquals("1.2.3.4", RateLimitFilter.getClientIp(request));
    }

    @Test
    void getClientIp_fallsBackToXRealIp() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("X-Real-IP", "9.8.7.6");
        request.setRemoteAddr("127.0.0.1");

        assertEquals("9.8.7.6", RateLimitFilter.getClientIp(request));
    }

    @Test
    void getClientIp_fallsBackToRemoteAddr() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRemoteAddr("192.168.1.1");

        assertEquals("192.168.1.1", RateLimitFilter.getClientIp(request));
    }
}
