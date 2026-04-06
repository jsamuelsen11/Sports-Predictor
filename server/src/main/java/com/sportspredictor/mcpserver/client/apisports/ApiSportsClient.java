package com.sportspredictor.mcpserver.client.apisports;

import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.service.annotation.GetExchange;
import org.springframework.web.service.annotation.HttpExchange;

/**
 * API-Sports client for structured sports data. API key injected as {@code x-apisports-key} header
 * by RestClient. Initially targets football (soccer) endpoints; other sports share the same response
 * wrapper format.
 */
@HttpExchange
public interface ApiSportsClient {

    /** Fetches fixtures (games) with optional filters. */
    @GetExchange("/fixtures")
    ApiSportsResponse<FixtureData> getFixtures(
            @RequestParam(value = "league", required = false) Integer league,
            @RequestParam(value = "season", required = false) Integer season,
            @RequestParam(value = "date", required = false) String date);

    /** Fetches teams for a league and season. */
    @GetExchange("/teams")
    ApiSportsResponse<TeamData> getTeams(@RequestParam("league") int league, @RequestParam("season") int season);

    /** Fetches league standings. */
    @GetExchange("/standings")
    ApiSportsResponse<StandingsData> getStandings(
            @RequestParam("league") int league, @RequestParam("season") int season);

    /** Fetches aggregated team statistics for a league and season. */
    @GetExchange("/teams/statistics")
    ApiSportsResponse<TeamStatisticsData> getTeamStatistics(
            @RequestParam("team") int team, @RequestParam("league") int league, @RequestParam("season") int season);

    /** Fetches player statistics with pagination. */
    @GetExchange("/players")
    ApiSportsResponse<PlayerData> getPlayers(
            @RequestParam("league") int league,
            @RequestParam("season") int season,
            @RequestParam(value = "page", required = false) Integer page);
}
