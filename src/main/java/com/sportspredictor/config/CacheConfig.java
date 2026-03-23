package com.sportspredictor.config;

import com.github.benmanes.caffeine.cache.Caffeine;
import java.time.Duration;
import java.util.List;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.caffeine.CaffeineCache;
import org.springframework.cache.support.SimpleCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** Configures named Caffeine caches with per-cache TTL policies per ADR-002. */
@Configuration
@EnableCaching
public class CacheConfig {

    /** Creates a cache manager with individually configured caches for external API responses. */
    @Bean
    public CacheManager cacheManager() {
        SimpleCacheManager cacheManager = new SimpleCacheManager();
        cacheManager.setCaches(List.of(
                buildCache("odds", Duration.ofMinutes(5)),
                buildCache("schedules", Duration.ofHours(1)),
                buildCache("team-stats", Duration.ofHours(6)),
                buildCache("player-stats", Duration.ofHours(6)),
                buildCache("weather", Duration.ofMinutes(30)),
                buildCache("standings", Duration.ofHours(1))));
        return cacheManager;
    }

    private CaffeineCache buildCache(String name, Duration ttl) {
        return new CaffeineCache(
                name, Caffeine.newBuilder().expireAfterWrite(ttl).recordStats().build());
    }
}
