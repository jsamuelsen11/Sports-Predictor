package com.sportspredictor.mcpserver.config;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;

/** Externalized cache TTL configuration, bound to {@code app.cache} properties. */
@ConfigurationProperties(prefix = "app.cache")
public record CacheProperties(
        Duration oddsTtl,
        Duration schedulesTtl,
        Duration teamStatsTtl,
        Duration playerStatsTtl,
        Duration weatherTtl,
        Duration standingsTtl) {

    /** Creates properties with ADR-002 default TTLs when values are not specified. */
    public CacheProperties {
        if (oddsTtl == null) {
            oddsTtl = Duration.ofMinutes(5);
        }
        if (schedulesTtl == null) {
            schedulesTtl = Duration.ofHours(1);
        }
        if (teamStatsTtl == null) {
            teamStatsTtl = Duration.ofHours(6);
        }
        if (playerStatsTtl == null) {
            playerStatsTtl = Duration.ofHours(6);
        }
        if (weatherTtl == null) {
            weatherTtl = Duration.ofMinutes(30);
        }
        if (standingsTtl == null) {
            standingsTtl = Duration.ofHours(1);
        }
    }
}
