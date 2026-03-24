package com.sportspredictor.client.oddsapi;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

/** Score/result data for a single event from The Odds API. */
@JsonIgnoreProperties(ignoreUnknown = true)
public record ScoreResponse(
        String id,
        @JsonProperty("sport_key") String sportKey,
        @JsonProperty("sport_title") String sportTitle,
        @JsonProperty("commence_time") String commenceTime,
        boolean completed,
        @JsonProperty("home_team") String homeTeam,
        @JsonProperty("away_team") String awayTeam,
        List<Score> scores) {

    /** A team's score in the event. */
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Score(String name, String score) {}
}
