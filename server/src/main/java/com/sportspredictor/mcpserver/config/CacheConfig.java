package com.sportspredictor.mcpserver.config;

import com.github.benmanes.caffeine.cache.Caffeine;
import java.time.Duration;
import java.util.List;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.caffeine.CaffeineCache;
import org.springframework.cache.support.SimpleCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** Configures named Caffeine caches with per-cache TTL policies per ADR-002. */
@Configuration
@EnableCaching
@EnableConfigurationProperties(CacheProperties.class)
public class CacheConfig {

    /** Creates a cache manager with individually configured caches for external API responses. */
    @Bean
    public CacheManager cacheManager(CacheProperties props) {
        SimpleCacheManager cacheManager = new SimpleCacheManager();
        cacheManager.setCaches(List.of(
                buildCache("odds", props.oddsTtl()),
                buildCache("schedules", props.schedulesTtl()),
                buildCache("team-stats", props.teamStatsTtl()),
                buildCache("player-stats", props.playerStatsTtl()),
                buildCache("weather", props.weatherTtl()),
                buildCache("standings", props.standingsTtl())));
        return cacheManager;
    }

    private CaffeineCache buildCache(String name, Duration ttl) {
        return new CaffeineCache(
                name, Caffeine.newBuilder().expireAfterWrite(ttl).recordStats().build());
    }
}
