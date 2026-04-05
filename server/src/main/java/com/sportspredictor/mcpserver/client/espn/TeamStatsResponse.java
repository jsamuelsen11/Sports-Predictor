package com.sportspredictor.mcpserver.client.espn;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.List;

/** ESPN team statistics API response. */
@JsonIgnoreProperties(ignoreUnknown = true)
public record TeamStatsResponse(List<Split> splits) {

    /** A statistical split (e.g., overall, home, away). */
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Split(String name, List<StatCategory> categories) {}

    /** A category of statistics (e.g., passing, rushing). */
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record StatCategory(String name, List<Stat> stats) {}

    /** An individual statistic with name, value, and display value. */
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Stat(String name, double value, String displayValue) {}
}
