package com.sportspredictor.client.oddsapi;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

/** Odds data for a single event from The Odds API. */
@JsonIgnoreProperties(ignoreUnknown = true)
public record OddsResponse(
        String id,
        @JsonProperty("sport_key") String sportKey,
        @JsonProperty("sport_title") String sportTitle,
        @JsonProperty("commence_time") String commenceTime,
        @JsonProperty("home_team") String homeTeam,
        @JsonProperty("away_team") String awayTeam,
        List<Bookmaker> bookmakers) {

    /** A bookmaker's odds for the event. */
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Bookmaker(String key, String title, List<Market> markets) {}

    /** A market (e.g., h2h, spreads, totals) offered by a bookmaker. */
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Market(String key, List<Outcome> outcomes) {}

    /** An individual outcome with price and optional point spread. */
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Outcome(String name, double price, Double point) {}
}
