package com.ceramax.api.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.ceramax.api.ApiApplication;
import com.ceramax.api.dto.response.CategoriaResponse;
import java.nio.charset.StandardCharsets;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.connection.RedisConnectionFactory;

@SpringBootTest(classes = ApiApplication.class)
@EnabledIfSystemProperty(named = "redis.integration", matches = "true")
class RedisCloudCacheIntegrationTest {

    @Autowired
    private CacheManager cacheManager;

    @Autowired
    private RedisConnectionFactory redisConnectionFactory;

    @Test
    void almacenaRecuperaVerificaTtlYEliminaUnaEntradaEnRedisReal() {
        Cache cache = cacheManager.getCache("categorias");
        assertNotNull(cache);
        String key = "phase3-integration-" + UUID.randomUUID();
        CategoriaResponse expected = new CategoriaResponse(
            UUID.randomUUID(),
            "TEST",
            "Redis integration test",
            null,
            true,
            OffsetDateTime.now(ZoneOffset.UTC),
            OffsetDateTime.now(ZoneOffset.UTC)
        );

        try {
            cache.put(key, expected);

            CategoriaResponse actual = cache.get(key, CategoriaResponse.class);
            assertEquals(expected, actual);

            RedisConnection connection = redisConnectionFactory.getConnection();
            try {
                Object ttl = connection.execute(
                    "TTL",
                    ("ceramax:cache:v2:categorias::" + key).getBytes(StandardCharsets.UTF_8)
                );
                assertNotNull(ttl);
                assertTrue(((Number) ttl).longValue() > 0);
                assertTrue(((Number) ttl).longValue() <= 600);
            } finally {
                connection.close();
            }
        } finally {
            cache.evict(key);
        }

        assertNull(cache.get(key));
    }

    @Test
    void redisExpiraClavesConTtl() throws InterruptedException {
        String key = "ceramax:phase3:expiry:" + UUID.randomUUID();
        byte[] keyBytes = key.getBytes(StandardCharsets.UTF_8);
        RedisConnection connection = redisConnectionFactory.getConnection();
        try {
            connection.execute("SET", keyBytes, "expires-soon".getBytes(StandardCharsets.UTF_8), "EX".getBytes(StandardCharsets.UTF_8), "1".getBytes(StandardCharsets.UTF_8));
        } finally {
            connection.close();
        }

        Thread.sleep(1500);

        connection = redisConnectionFactory.getConnection();
        try {
            Object exists = connection.execute("EXISTS", keyBytes);
            assertEquals(Boolean.FALSE, exists);
        } finally {
            connection.close();
        }
    }
}
