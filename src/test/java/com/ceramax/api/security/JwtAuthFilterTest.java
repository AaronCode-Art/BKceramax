package com.ceramax.api.security;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import java.io.IOException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;

class JwtAuthFilterTest {

    private final JwtService jwtService = org.mockito.Mockito.mock(JwtService.class);
    private final CeramaxUserDetailsService userDetailsService =
        org.mockito.Mockito.mock(CeramaxUserDetailsService.class);
    private final ExposedJwtAuthFilter filter = new ExposedJwtAuthFilter(jwtService, userDetailsService);

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void noAutenticaJwtDeUnaCuentaDesactivada() throws Exception {
        MockHttpServletRequest request = requestWithToken();
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain chain = org.mockito.Mockito.mock(FilterChain.class);
        UserDetails disabledUser = User.withUsername("staff@example.test")
            .password("encoded-password")
            .authorities("ROLE_ADMIN")
            .disabled(true)
            .build();
        when(jwtService.isTokenValid("valid-token")).thenReturn(true);
        when(jwtService.extractEmail("valid-token")).thenReturn("staff@example.test");
        when(userDetailsService.loadUserByUsername("staff@example.test")).thenReturn(disabledUser);

        filter.filter(request, response, chain);

        assertNull(SecurityContextHolder.getContext().getAuthentication());
        verify(chain).doFilter(request, response);
    }

    @Test
    void autenticaJwtDeUnaCuentaActiva() throws Exception {
        MockHttpServletRequest request = requestWithToken();
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain chain = org.mockito.Mockito.mock(FilterChain.class);
        UserDetails enabledUser = User.withUsername("staff@example.test")
            .password("encoded-password")
            .authorities("ROLE_ADMIN")
            .build();
        when(jwtService.isTokenValid("valid-token")).thenReturn(true);
        when(jwtService.extractEmail("valid-token")).thenReturn("staff@example.test");
        when(userDetailsService.loadUserByUsername("staff@example.test")).thenReturn(enabledUser);

        filter.filter(request, response, chain);

        assertNotNull(SecurityContextHolder.getContext().getAuthentication());
        verify(chain).doFilter(request, response);
    }

    private MockHttpServletRequest requestWithToken() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer valid-token");
        return request;
    }

    private static class ExposedJwtAuthFilter extends JwtAuthFilter {

        private ExposedJwtAuthFilter(JwtService jwtService, CeramaxUserDetailsService userDetailsService) {
            super(jwtService, userDetailsService);
        }

        private void filter(MockHttpServletRequest request, MockHttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
            doFilterInternal(request, response, chain);
        }
    }
}
