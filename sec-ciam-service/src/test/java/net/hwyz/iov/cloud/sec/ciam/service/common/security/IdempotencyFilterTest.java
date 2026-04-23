package net.hwyz.iov.cloud.sec.ciam.service.common.security;
import net.hwyz.iov.cloud.sec.ciam.service.application.service.*;
import net.hwyz.iov.cloud.sec.ciam.service.domain.adapter.*;

import jakarta.servlet.ServletException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.*;

class IdempotencyFilterTest {

    private IdempotencyFilter filter;

    @BeforeEach
    void setUp() {
        filter = new IdempotencyFilter();
        filter.clearCache();
    }

    @Test
    void getRequest_skipsIdempotencyCheck() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/account/profile");
        request.addHeader("X-Idempotency-Key", "key-001");
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();

        filter.doFilterInternal(request, response, chain);

        assertNotNull(chain.getRequest(), "GET requests should pass through");
        assertEquals(0, filter.cacheSize());
    }

    @Test
    void postWithoutIdempotencyKey_passesThrough() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/v1/auth/login");
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();

        filter.doFilterInternal(request, response, chain);

        assertNotNull(chain.getRequest());
        assertEquals(0, filter.cacheSize());
    }

    @Test
    void postWithIdempotencyKey_cachesResponse() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/v1/account/bind");
        request.addHeader("X-Idempotency-Key", "key-002");
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();

        filter.doFilterInternal(request, response, chain);

        assertNotNull(chain.getRequest());
        assertEquals(1, filter.cacheSize());
    }

    @Test
    void duplicatePostRequest_returnsCachedResponse() throws Exception {
        // First request
        MockHttpServletRequest request1 = new MockHttpServletRequest("POST", "/api/v1/account/bind");
        request1.addHeader("X-Idempotency-Key", "key-003");
        MockHttpServletResponse response1 = new MockHttpServletResponse();
        filter.doFilterInternal(request1, response1, new MockFilterChain());

        // Second request with same key
        MockHttpServletRequest request2 = new MockHttpServletRequest("POST", "/api/v1/account/bind");
        request2.addHeader("X-Idempotency-Key", "key-003");
        MockHttpServletResponse response2 = new MockHttpServletResponse();
        MockFilterChain chain2 = new MockFilterChain();
        filter.doFilterInternal(request2, response2, chain2);

        assertNull(chain2.getRequest(), "Duplicate request should NOT invoke filter chain");
    }

    @Test
    void putRequest_alsoChecksIdempotency() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("PUT", "/api/v1/account/profile");
        request.addHeader("X-Idempotency-Key", "key-004");
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();

        filter.doFilterInternal(request, response, chain);

        assertNotNull(chain.getRequest());
        assertEquals(1, filter.cacheSize());
    }

    @Test
    void deleteRequest_skipsIdempotencyCheck() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("DELETE", "/api/v1/account/session/1");
        request.addHeader("X-Idempotency-Key", "key-005");
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();

        filter.doFilterInternal(request, response, chain);

        assertNotNull(chain.getRequest());
        assertEquals(0, filter.cacheSize());
    }
}
