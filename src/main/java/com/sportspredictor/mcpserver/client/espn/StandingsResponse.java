package com.sportspredictor.mcpserver.client.espn;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.List;

/** ESPN standings API response. */
@JsonIgnoreProperties(ignoreUnknown = true)
public record StandingsResponse(List<StandingsGroup> children) {

    /** A conference or division grouping in the standings. */
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record StandingsGroup(String name, List<Standing> standings) {}

    /** Standings entries within a group. */
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Standing(List<Entry> entries) {}

    /** A single team's standing entry. */
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Entry(String team, List<Stat> stats) {}

    /** A standings statistic (wins, losses, win percentage, etc.). */
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Stat(String name, double value, String displayValue) {}
}
