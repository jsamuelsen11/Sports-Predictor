package com.sportspredictor.mcpserver.client.espn;

import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.service.annotation.GetExchange;
import org.springframework.web.service.annotation.HttpExchange;

/** ESPN hidden API client — no authentication required. */
@HttpExchange
public interface EspnApiClient {

    /** Fetches live and recent scoreboard for the given sport and league. */
    @GetExchange("/apis/site/v2/sports/{sport}/{league}/scoreboard")
    ScoreboardResponse getScoreboard(@PathVariable("sport") String sport, @PathVariable("league") String league);

    /** Fetches scoreboard for a specific date range. */
    @GetExchange("/apis/site/v2/sports/{sport}/{league}/scoreboard")
    ScoreboardResponse getSchedule(
            @PathVariable("sport") String sport,
            @PathVariable("league") String league,
            @RequestParam("dates") String dates);

    /** Fetches team statistics. */
    @GetExchange("/apis/site/v2/sports/{sport}/{league}/teams/{teamId}/statistics")
    TeamStatsResponse getTeamStats(
            @PathVariable("sport") String sport,
            @PathVariable("league") String league,
            @PathVariable("teamId") String teamId);

    /** Fetches individual player statistics. */
    @GetExchange("/apis/site/v2/sports/{sport}/{league}/athletes/{athleteId}/statistics")
    PlayerStatsResponse getPlayerStats(
            @PathVariable("sport") String sport,
            @PathVariable("league") String league,
            @PathVariable("athleteId") String athleteId);

    /** Fetches injury reports for the given sport and league. */
    @GetExchange("/apis/site/v2/sports/{sport}/{league}/injuries")
    InjuryResponse getInjuries(@PathVariable("sport") String sport, @PathVariable("league") String league);

    /** Fetches current standings. */
    @GetExchange("/apis/site/v2/sports/{sport}/{league}/standings")
    StandingsResponse getStandings(@PathVariable("sport") String sport, @PathVariable("league") String league);

    /** Fetches team roster. */
    @GetExchange("/apis/site/v2/sports/{sport}/{league}/teams/{teamId}/roster")
    RosterResponse getRoster(
            @PathVariable("sport") String sport,
            @PathVariable("league") String league,
            @PathVariable("teamId") String teamId);
}
