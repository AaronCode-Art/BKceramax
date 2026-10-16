package com.ceramax.api.config;

import com.ceramax.api.exception.CacheUnavailableException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Duration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.Cache;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.annotation.CachingConfigurer;
import org.springframework.cache.interceptor.CacheErrorHandler;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.SerializationException;
import org.springframework.data.redis.serializer.RedisSerializationContext.SerializationPair;

@Configuration
@EnableCaching
public class CacheConfig implements CachingConfigurer {

    private static final Logger LOGGER = LoggerFactory.getLogger(CacheConfig.class);
    private static final String CACHE_KEY_NAMESPACE = "ceramax:cache:v2:";

    @Bean
    RedisCacheConfiguration redisCacheConfiguration(
        @Value("${spring.cache.redis.time-to-live:10m}") Duration timeToLive
    ) {
        ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();
        return RedisCacheConfiguration.defaultCacheConfig()
            .computePrefixWith(cacheName -> CACHE_KEY_NAMESPACE + cacheName + "::")
            .entryTtl(timeToLive)
            .disableCachingNullValues()
            .serializeValuesWith(
                SerializationPair.fromSerializer(
                    GenericJackson2JsonRedisSerializer.builder()
                        .objectMapper(objectMapper)
                        .defaultTyping(true)
                        .build()
                )
            );
    }

    @Bean
    CacheErrorHandler cacheErrorHandler() {
        return new CacheErrorHandler() {
            @Override
            public void handleCacheGetError(RuntimeException exception, Cache cache, Object key) {
                if (exception instanceof SerializationException) {
                    LOGGER.warn("Removing unreadable Redis cache entry '{}' from cache '{}'", key, cache.getName());
                    cache.evict(key);
                    return;
                }
                throw unavailable(cache, exception);
            }

            @Override
            public void handleCachePutError(RuntimeException exception, Cache cache, Object key, Object value) {
                throw unavailable(cache, exception);
            }

            @Override
            public void handleCacheEvictError(RuntimeException exception, Cache cache, Object key) {
                throw unavailable(cache, exception);
            }

            @Override
            public void handleCacheClearError(RuntimeException exception, Cache cache) {
                throw unavailable(cache, exception);
            }

            private CacheUnavailableException unavailable(Cache cache, RuntimeException cause) {
                LOGGER.error("Redis cache operation failed for cache '{}'", cache.getName(), cause);
                return new CacheUnavailableException();
            }
        };
    }

    @Override
    public CacheErrorHandler errorHandler() {
        return cacheErrorHandler();
    }
}
