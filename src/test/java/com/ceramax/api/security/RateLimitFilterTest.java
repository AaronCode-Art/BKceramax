package com.ceramax.api.security;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import java.time.Duration;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

class RateLimitFilterTest {

    private final ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void rejectsRequestsExceedingLoginLimitWith429AndRetryAfter() throws Exception {
        RateLimitFilter filter = createFilter(1, 10, 10);
        MockHttpServletRequest firstRequest = request("POST", "/api/auth/staff/login", "192.0.2.10");
        MockHttpServletResponse firstResponse = new MockHttpServletResponse();
        filter.doFilter(firstRequest, firstResponse, new MockFilterChain());
        assertEquals(200, firstResponse.getStatus());

        MockHttpServletRequest secondRequest = request("POST", "/api/auth/staff/login", "192.0.2.10");
        MockHttpServletResponse secondResponse = new MockHttpServletResponse();
        filter.doFilter(secondRequest, secondResponse, new MockFilterChain());

        assertEquals(429, secondResponse.getStatus());
        assertEquals("60", secondResponse.getHeader("Retry-After"));
        assertTrue(secondResponse.getContentAsString().contains("excedió el límite"));
    }

    @Test
    void appliesDedicatedLimitToCustomerRegistrationRoutes() throws Exception {
        RateLimitFilter filter = createFilter(1, 10, 10);
        MockHttpServletResponse firstResponse = new MockHttpServletResponse();
        filter.doFilter(request("POST", "/api/v1/clientes/registro/buscar", "192.0.2.14"), firstResponse, new MockFilterChain());
        MockHttpServletResponse secondResponse = new MockHttpServletResponse();
        filter.doFilter(request("POST", "/api/v1/clientes/registro/completar", "192.0.2.14"), secondResponse, new MockFilterChain());

        assertEquals(200, firstResponse.getStatus());
        assertEquals(429, secondResponse.getStatus());
    }

    @Test
    void appliesCheckoutLimitPerAuthenticatedUser() throws Exception {
        RateLimitFilter filter = createFilter(10, 1, 10);
        SecurityContextHolder.getContext().setAuthentication(
            UsernamePasswordAuthenticationToken.authenticated("cliente@example.test", null, java.util.List.of())
        );

        MockHttpServletResponse firstResponse = new MockHttpServletResponse();
        filter.doFilter(
            request("POST", "/api/v1/pedidos/web/checkout", "192.0.2.11"),
            firstResponse,
            new MockFilterChain()
        );
        MockHttpServletResponse secondResponse = new MockHttpServletResponse();
        filter.doFilter(
            request("POST", "/api/v1/pedidos/web/checkout", "192.0.2.12"),
            secondResponse,
            new MockFilterChain()
        );

        assertEquals(200, firstResponse.getStatus());
        assertEquals(429, secondResponse.getStatus());
    }

    @Test
    void doesNotLimitUnconfiguredRoutes() throws Exception {
        RateLimitFilter filter = createFilter(1, 1, 1);
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain chain = new MockFilterChain();

        filter.doFilter(request("GET", "/api/v1/clientes", "192.0.2.13"), response, chain);

        assertEquals(200, response.getStatus());
        assertEquals(null, response.getHeader("X-RateLimit-Limit"));
    }

    private RateLimitFilter createFilter(int authCapacity, int checkoutCapacity, int publicCapacity) {
        return new RateLimitFilter(
            objectMapper,
            authCapacity,
            checkoutCapacity,
            publicCapacity,
            Duration.ofMinutes(1)
        );
    }

    private MockHttpServletRequest request(String method, String path, String remoteAddress) {
        MockHttpServletRequest request = new MockHttpServletRequest(method, path);
        request.setRemoteAddr(remoteAddress);
        return request;
    }
}
