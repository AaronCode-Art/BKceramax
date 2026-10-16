package com.ceramax.api.config;

import com.ceramax.api.security.RateLimitFilter;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Duration;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RateLimitConfig {

    @Bean
    public RateLimitFilter rateLimitFilter(
        @Value("${app.rate-limit.auth.capacity:20}") int authCapacity,
        @Value("${app.rate-limit.checkout.capacity:10}") int checkoutCapacity,
        @Value("${app.rate-limit.public.capacity:100}") int publicCapacity,
        @Value("${app.rate-limit.refill-period:1m}") Duration refillPeriod
    ) {
        return new RateLimitFilter(
            new ObjectMapper(),
            authCapacity,
            checkoutCapacity,
            publicCapacity,
            refillPeriod
        );
    }

    @Bean
    public FilterRegistrationBean<RateLimitFilter> rateLimitFilterRegistration(RateLimitFilter filter) {
        FilterRegistrationBean<RateLimitFilter> registration = new FilterRegistrationBean<>(filter);
        registration.setEnabled(false);
        return registration;
    }
}
