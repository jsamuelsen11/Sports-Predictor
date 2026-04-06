package com.sportspredictor.mcpserver.config;

import com.github.benmanes.caffeine.cache.Cache;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.binder.cache.CaffeineCacheMetrics;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.cache.CacheManager;
import org.springframework.cache.caffeine.CaffeineCache;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/** Binds Caffeine cache statistics (already recorded via {@code .recordStats()}) to Micrometer. */
@Component
public class CacheMetricsConfig {

    private final CacheManager cacheManager;
    private final MeterRegistry meterRegistry;

    /** Creates a cache metrics config binding cache stats to the meter registry. */
    public CacheMetricsConfig(CacheManager cacheManager, MeterRegistry meterRegistry) {
        this.cacheManager = cacheManager;
        this.meterRegistry = meterRegistry;
    }

    /** Registers all Caffeine caches with Micrometer after application startup. */
    @EventListener(ApplicationReadyEvent.class)
    public void bindCacheMetrics() {
        for (String name : cacheManager.getCacheNames()) {
            org.springframework.cache.Cache springCache = cacheManager.getCache(name);
            if (springCache instanceof CaffeineCache caffeineCache) {
                Cache<Object, Object> nativeCache = caffeineCache.getNativeCache();
                CaffeineCacheMetrics.monitor(meterRegistry, nativeCache, name);
            }
        }
    }
}
