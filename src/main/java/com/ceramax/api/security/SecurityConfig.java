package com.ceramax.api.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    private final JwtAuthFilter jwtAuthFilter;
    private final RateLimitFilter rateLimitFilter;
    private final UserDetailsService userDetailsService;

    public SecurityConfig(
        JwtAuthFilter jwtAuthFilter,
        RateLimitFilter rateLimitFilter,
        UserDetailsService userDetailsService
    ) {
        this.jwtAuthFilter = jwtAuthFilter;
        this.rateLimitFilter = rateLimitFilter;
        this.userDetailsService = userDetailsService;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(
        HttpSecurity http,
        CorsConfigurationSource corsConfigurationSource
    ) throws Exception {
        return http
            .csrf(AbstractHttpConfigurer::disable)
            .cors(cors -> cors.configurationSource(corsConfigurationSource))
            .authorizeHttpRequests(auth -> auth
                .requestMatchers(
                    "/api/auth/**",
                    "/api/v1/public/**",
                    "/api/v1/clientes/registro/**",
                    "/ws",
                    "/ws/**",
                    "/swagger-ui/**",
                    "/v3/api-docs/**",
                    "/swagger-ui.html"
                ).permitAll()
                .requestMatchers(HttpMethod.GET, "/actuator/health", "/actuator/health/**").permitAll()
                .requestMatchers(HttpMethod.HEAD, "/actuator/health", "/actuator/health/**").permitAll()
                .requestMatchers(
                    HttpMethod.POST,
                    "/api/v1/clientes"
                ).permitAll()
                .requestMatchers(
                    HttpMethod.GET,
                    "/api/v1/inventario/movimientos",
                    "/api/v1/inventario/reservas-activas"
                ).hasAnyRole("ADMIN", "JEFE_LOGISTICA", "LOGISTICA")
                .anyRequest().authenticated()
            )
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authenticationProvider(authenticationProvider())
            .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class)
            .addFilterAfter(rateLimitFilter, JwtAuthFilter.class)
            .build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource(
        @Value("${app.cors.allowed-origins}") String allowedOrigins
    ) {
        List<String> origins = Arrays.stream(allowedOrigins.split(","))
            .map(String::trim)
            .filter(origin -> !origin.isEmpty())
            .toList();
        if (origins.isEmpty() || origins.stream().anyMatch(origin -> origin.contains("*"))) {
            throw new IllegalStateException("app.cors.allowed-origins debe contener orígenes explícitos.");
        }

        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(origins);
        configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(List.of("Authorization", "Content-Type", "Accept"));
        configuration.setAllowCredentials(false);
        configuration.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/api/**", configuration);
        return source;
    }

    @Bean
    public AuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider(userDetailsService);
        provider.setPasswordEncoder(passwordEncoder());
        return provider;
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration configuration) throws Exception {
        return configuration.getAuthenticationManager();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
