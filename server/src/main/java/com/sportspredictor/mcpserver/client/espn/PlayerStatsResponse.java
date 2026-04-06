package com.sportspredictor.mcpserver.client.espn;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.List;

/** ESPN player statistics API response. */
@JsonIgnoreProperties(ignoreUnknown = true)
public record PlayerStatsResponse(List<Split> splits) {

    /** A statistical split (e.g., career, by season). */
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Split(String name, List<StatCategory> categories) {}

    /** A category of statistics. */
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record StatCategory(String name, List<Stat> stats) {}

    /** An individual statistic. */
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Stat(String name, double value, String displayValue) {}
}
