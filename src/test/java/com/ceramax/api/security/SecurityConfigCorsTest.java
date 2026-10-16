package com.ceramax.api.security;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;

class SecurityConfigCorsTest {

    private final SecurityConfig securityConfig = new SecurityConfig(
        mock(JwtAuthFilter.class),
        mock(RateLimitFilter.class),
        mock(UserDetailsService.class)
    );

    @Test
    void usaLosOrigenesConfiguradosPorEntornoComoListaExacta() {
        CorsConfigurationSource source = securityConfig.corsConfigurationSource(
            " https://tienda.example.test, https://admin.example.test "
        );
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/public/catalogo");

        CorsConfiguration configuration = source.getCorsConfiguration(request);

        assertEquals(
            List.of("https://tienda.example.test", "https://admin.example.test"),
            configuration.getAllowedOrigins()
        );
    }

    @Test
    void rechazaConfiguracionCorsVaciaOWildcard() {
        assertThrows(IllegalStateException.class, () -> securityConfig.corsConfigurationSource(" , "));
        assertThrows(IllegalStateException.class, () -> securityConfig.corsConfigurationSource("*"));
        assertThrows(
            IllegalStateException.class,
            () -> securityConfig.corsConfigurationSource("https://*.example.test")
        );
    }
}
