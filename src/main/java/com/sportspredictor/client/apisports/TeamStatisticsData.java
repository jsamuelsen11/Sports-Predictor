package com.sportspredictor.client.apisports;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/** Aggregated team statistics from API-Sports. */
@JsonIgnoreProperties(ignoreUnknown = true)
public record TeamStatisticsData(
        TeamData.TeamInfo team, FixtureData.League league, FixtureStats fixtures, GoalStatistics goals) {

    /** Fixture win/draw/loss breakdown. */
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record FixtureStats(WdlStats played, WdlStats wins, WdlStats draws, WdlStats loses) {}

    /** Home/away/total breakdown for a single stat. */
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record WdlStats(int home, int away, int total) {}

    /** Goal statistics with totals and averages. */
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record GoalStatistics(GoalBreakdown forGoals, GoalBreakdown against) {}

    /** Total and average goals broken down by venue. */
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record GoalBreakdown(GoalTotal total, GoalAverage average) {}

    /** Goal totals by venue. */
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record GoalTotal(int home, int away, int total) {}

    /** Goal averages by venue. */
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record GoalAverage(String home, String away, String total) {}
}
