package com.sportspredictor.client.apisports;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/** Fixture (game) data from API-Sports. */
@JsonIgnoreProperties(ignoreUnknown = true)
public record FixtureData(Fixture fixture, League league, Teams teams, Goals goals, Score score) {

    /** Core fixture metadata. */
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Fixture(int id, String timezone, String date, long timestamp, Status status) {}

    /** Fixture status information. */
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Status(
            @JsonProperty("long") String longName,
            @JsonProperty("short") String shortName,
            Integer elapsed) {}

    /** League context for the fixture. */
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record League(int id, String name, String country, int season, String round) {}

    /** Home and away team information. */
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Teams(TeamInfo home, TeamInfo away) {}

    /** Individual team identity. */
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record TeamInfo(int id, String name, boolean winner) {}

    /** Full-time goal totals. */
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Goals(Integer home, Integer away) {}

    /** Score breakdown by period. */
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Score(ScorePeriod halftime, ScorePeriod fulltime, ScorePeriod extratime, ScorePeriod penalty) {}

    /** Score for a single period. */
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record ScorePeriod(Integer home, Integer away) {}
}
