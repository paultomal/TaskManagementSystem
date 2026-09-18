package com.example.taskmanagement.config;

import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.concurrent.ConcurrentMapCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * In-memory cache manager so the app runs without an external Redis server.
 * The cache names match the {@code @Cacheable}/{@code @CacheEvict} usages in
 * the service layer. Swap back to a Redis-backed {@code RedisCacheManager} for
 * a shared/distributed cache.
 */
@Configuration
@EnableCaching
public class CacheConfig {

    @Bean
    public CacheManager cacheManager() {
        return new ConcurrentMapCacheManager("tasks", "taskLists");
    }
}
