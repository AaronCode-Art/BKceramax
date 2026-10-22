package com.ceramax.api.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.ConsumptionProbe;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.Map;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

public class RateLimitFilter extends OncePerRequestFilter {

    private static final String AUTH_STAFF_PATH = "/api/auth/staff/login";
    private static final String AUTH_CLIENT_PATH = "/api/auth/cliente/login";
    private static final String CLIENT_PASSWORD_RECOVERY_PATH = "/api/auth/cliente/recuperacion";
    private static final String CLIENT_PASSWORD_RESET_PATH = "/api/auth/cliente/restablecer-contrasena";
    private static final String CLIENT_REGISTRATION_PATH = "/api/v1/clientes/registro";
    private static final String CLIENTS_PATH = "/api/v1/clientes";
    private static final String CHECKOUT_PATH = "/api/v1/pedidos/web/checkout";
    private static final String CATALOG_PATH = "/api/v1/public/catalogo";
    private static final String UBIGEO_PATH = "/api/v1/public/ubigeos";
    private static final int CLEANUP_INTERVAL = 256;

    private final ObjectMapper objectMapper;
    private final int authCapacity;
    private final int checkoutCapacity;
    private final int publicCapacity;
    private final Duration refillPeriod;
    private final ConcurrentHashMap<String, BucketEntry> buckets = new ConcurrentHashMap<>();
    private final AtomicLong requests = new AtomicLong();

    public RateLimitFilter(
        ObjectMapper objectMapper,
        int authCapacity,
        int checkoutCapacity,
        int publicCapacity,
        Duration refillPeriod
    ) {
        if (authCapacity < 1 || checkoutCapacity < 1 || publicCapacity < 1) {
            throw new IllegalArgumentException("Rate-limit capacities must be positive");
        }
        if (refillPeriod.isZero() || refillPeriod.isNegative()) {
            throw new IllegalArgumentException("Rate-limit refill period must be positive");
        }
        this.objectMapper = objectMapper;
        this.authCapacity = authCapacity;
        this.checkoutCapacity = checkoutCapacity;
        this.publicCapacity = publicCapacity;
        this.refillPeriod = refillPeriod;
    }

    @Override
    protected void doFilterInternal(
        HttpServletRequest request,
        HttpServletResponse response,
        FilterChain filterChain
    ) throws ServletException, IOException {
        Policy policy = policyFor(request);
        if (policy == null) {
            filterChain.doFilter(request, response);
            return;
        }

        String clientKey = clientKey(request, policy);
        BucketEntry entry = getBucket(policy, clientKey);
        ConsumptionProbe probe = entry.bucket().tryConsumeAndReturnRemaining(1);
        response.setHeader("X-RateLimit-Limit", Integer.toString(policy.capacity()));
        response.setHeader("X-RateLimit-Remaining", Long.toString(probe.getRemainingTokens()));

        if (probe.isConsumed()) {
            filterChain.doFilter(request, response);
            return;
        }

        long retryAfterSeconds = Math.max(
            1,
            (probe.getNanosToWaitForRefill() + 999_999_999L) / 1_000_000_000L
        );
        response.setStatus(429);
        response.setHeader("Retry-After", Long.toString(retryAfterSeconds));
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        objectMapper.writeValue(
            response.getOutputStream(),
            Map.of(
                "timestamp", OffsetDateTime.now(ZoneOffset.UTC).toString(),
                "status", 429,
                "error", "Too Many Requests",
                "message", "Se excedió el límite de solicitudes. Inténtalo nuevamente más tarde.",
                "path", request.getRequestURI()
            )
        );
    }

    private Policy policyFor(HttpServletRequest request) {
        String method = request.getMethod();
        String path = request.getRequestURI();

        if ("POST".equals(method) && (AUTH_STAFF_PATH.equals(path) || AUTH_CLIENT_PATH.equals(path))) {
            return new Policy("auth", authCapacity, false);
        }
        if ("POST".equals(method)
            && (CLIENT_PASSWORD_RECOVERY_PATH.equals(path) || CLIENT_PASSWORD_RESET_PATH.equals(path))) {
            return new Policy("client-password-recovery", Math.min(authCapacity, 5), false);
        }
        if ("POST".equals(method) && (
            isPathOrChild(path, CLIENT_REGISTRATION_PATH) || CLIENTS_PATH.equals(path)
        )) {
            return new Policy("client-registration", Math.min(authCapacity, 5), false);
        }
        if ("POST".equals(method) && CHECKOUT_PATH.equals(path)) {
            return new Policy("checkout", checkoutCapacity, true);
        }
        if ("GET".equals(method) && (
            isPathOrChild(path, CATALOG_PATH) || isPathOrChild(path, UBIGEO_PATH)
        )) {
            return new Policy("public", publicCapacity, false);
        }
        return null;
    }

    private String clientKey(HttpServletRequest request, Policy policy) {
        if (policy.byAuthenticatedUser()) {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            if (authentication != null
                && authentication.isAuthenticated()
                && !(authentication instanceof AnonymousAuthenticationToken)) {
                return policy.name() + ":" + authentication.getName();
            }
        }
        String remoteAddress = request.getRemoteAddr();
        return policy.name() + ":" + (remoteAddress == null ? "unknown" : remoteAddress);
    }

    private BucketEntry getBucket(Policy policy, String key) {
        long now = System.nanoTime();
        BucketEntry entry = buckets.compute(key, (ignored, current) -> {
            if (current == null) {
                return new BucketEntry(createBucket(policy.capacity()), now);
            }
            current.setLastAccessNanos(now);
            return current;
        });
        if (requests.incrementAndGet() % CLEANUP_INTERVAL == 0) {
            removeExpiredBuckets(now);
        }
        return entry;
    }

    private Bucket createBucket(int capacity) {
        Bandwidth limit = Bandwidth.builder()
            .capacity(capacity)
            .refillGreedy(capacity, refillPeriod)
            .build();
        return Bucket.builder().addLimit(limit).build();
    }

    private void removeExpiredBuckets(long now) {
        long expirationNanos = refillPeriod.multipliedBy(2).toNanos();
        buckets.entrySet().removeIf(entry -> now - entry.getValue().lastAccessNanos() > expirationNanos);
    }

    private boolean isPathOrChild(String path, String basePath) {
        return path.equals(basePath) || path.startsWith(basePath + "/");
    }

    private record Policy(String name, int capacity, boolean byAuthenticatedUser) {}

    private static final class BucketEntry {
        private final Bucket bucket;
        private volatile long lastAccessNanos;

        private BucketEntry(Bucket bucket, long lastAccessNanos) {
            this.bucket = bucket;
            this.lastAccessNanos = lastAccessNanos;
        }

        private Bucket bucket() {
            return bucket;
        }

        private long lastAccessNanos() {
            return lastAccessNanos;
        }

        private void setLastAccessNanos(long value) {
            lastAccessNanos = value;
        }
    }
}
