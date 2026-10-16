package com.ceramax.api.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import io.github.resilience4j.circuitbreaker.CallNotPermittedException;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import java.time.Duration;
import org.junit.jupiter.api.Test;

class CircuitBreakerRecoveryTest {

    @Test
    void abreTrasFallosRechazaLlamadasYPasaAMediaAbiertoParaRecuperarse() throws InterruptedException {
        CircuitBreakerConfig config = CircuitBreakerConfig.custom()
            .failureRateThreshold(50)
            .minimumNumberOfCalls(2)
            .slidingWindowSize(2)
            .permittedNumberOfCallsInHalfOpenState(2)
            .waitDurationInOpenState(Duration.ofMillis(100))
            .build();
        CircuitBreaker breaker = CircuitBreakerRegistry.of(config).circuitBreaker("external-service-test");

        assertThrows(IllegalStateException.class, () ->
            breaker.executeRunnable(() -> { throw new IllegalStateException("external service failed"); })
        );
        assertThrows(IllegalStateException.class, () ->
            breaker.executeRunnable(() -> { throw new IllegalStateException("external service failed"); })
        );
        assertEquals(CircuitBreaker.State.OPEN, breaker.getState());
        assertThrows(CallNotPermittedException.class, () -> breaker.executeRunnable(() -> {}));

        Thread.sleep(150);

        breaker.executeRunnable(() -> {});
        breaker.executeRunnable(() -> {});

        assertEquals(CircuitBreaker.State.CLOSED, breaker.getState());
    }
}
