package com.ceramax.api.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import com.ceramax.api.dto.response.CategoriaResponse;
import com.ceramax.api.exception.CacheUnavailableException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.cache.Cache;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.SerializationException;

class CacheConfigTest {

    private final CacheConfig cacheConfig = new CacheConfig();

    @Test
    void configuraTtlYSerializacionDeRespuestas() {
        ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();
        RedisCacheConfiguration configuration = cacheConfig.redisCacheConfiguration(Duration.ofMinutes(10));
        CategoriaResponse response = new CategoriaResponse(
            UUID.randomUUID(),
            "DEC",
            "Decoración",
            null,
            true,
            OffsetDateTime.now(ZoneOffset.UTC),
            OffsetDateTime.now(ZoneOffset.UTC)
        );
        GenericJackson2JsonRedisSerializer serializer = GenericJackson2JsonRedisSerializer.builder()
            .objectMapper(objectMapper)
            .defaultTyping(true)
            .build();

        assertEquals(
            Duration.ofMinutes(10),
            configuration.getTtlFunction().getTimeToLive("test", response)
        );
        assertEquals("ceramax:cache:v2:categorias::", configuration.getKeyPrefixFor("categorias"));
        assertEquals(
            response,
            assertInstanceOf(CategoriaResponse.class, serializer.deserialize(serializer.serialize(response)))
        );
    }

    @Test
    void fallaExplicitamenteSiRedisNoPuedeLeerLaCache() {
        Cache cache = mock(Cache.class);

        assertThrows(
            CacheUnavailableException.class,
            () -> cacheConfig.cacheErrorHandler().handleCacheGetError(
                new IllegalStateException("Redis desconectado"),
                cache,
                "categorias"
            )
        );
    }

    @Test
    void eliminaSoloLaEntradaCuandoRedisNoPuedeDeserializarla() {
        Cache cache = mock(Cache.class);

        cacheConfig.errorHandler().handleCacheGetError(
            new SerializationException("Formato de caché incompatible"),
            cache,
            "listar"
        );

        verify(cache).evict("listar");
    }

    @Test
    void fallaExplicitamenteSiRedisNoPuedeInvalidarLaCache() {
        Cache cache = mock(Cache.class);

        assertThrows(
            CacheUnavailableException.class,
            () -> cacheConfig.cacheErrorHandler().handleCacheEvictError(
                new IllegalStateException("Redis desconectado"),
                cache,
                "categorias"
            )
        );
    }
}
