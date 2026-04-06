package com.sportspredictor.mcpserver.client.apisports;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.List;

/** Player data from API-Sports. */
@JsonIgnoreProperties(ignoreUnknown = true)
public record PlayerData(Player player, List<PlayerStatistic> statistics) {

    /** Player identity. */
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Player(int id, String name, String firstname, String lastname, int age, String nationality) {}

    /** Player statistics for a specific team and league. */
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record PlayerStatistic(TeamData.TeamInfo team, FixtureData.League league, Games games, Goals goals) {}

    /** Games played breakdown. */
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Games(int appearences, int lineups, int minutes, String position, String rating) {}

    /** Goal and assist statistics. */
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Goals(Integer total, Integer assists) {}
}
