package com.sportspredictor.mcpserver.client.apisports;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

/** League standings data from API-Sports. */
@JsonIgnoreProperties(ignoreUnknown = true)
public record StandingsData(League league) {

    /** League with embedded standings table. */
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record League(int id, String name, String country, int season, List<List<StandingEntry>> standings) {}

    /** A single team's standing entry. */
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record StandingEntry(int rank, TeamRef team, int points, String form, StandingStats all) {}

    /** Team reference in standings. */
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record TeamRef(int id, String name, String logo) {}

    /** Aggregated stats for standing calculation. */
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record StandingStats(int played, int win, int draw, int lose, GoalStats goals) {}

    /** Goals for and against. */
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record GoalStats(@JsonProperty("for") int forGoals, int against) {}
}
