package com.sportspredictor.mcpserver.client.oddsapi;

import java.util.List;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.service.annotation.GetExchange;
import org.springframework.web.service.annotation.HttpExchange;

/** The Odds API client — API key injected by RestClient interceptor. */
@HttpExchange
public interface OddsApiClient {

    /** Lists all available sports. */
    @GetExchange("/sports")
    List<SportResponse> getSports();

    /** Fetches live odds for a sport with optional filters. */
    @GetExchange("/sports/{sport}/odds")
    List<OddsResponse> getOdds(
            @PathVariable("sport") String sport,
            @RequestParam("regions") String regions,
            @RequestParam(value = "markets", required = false) String markets,
            @RequestParam(value = "oddsFormat", required = false) String oddsFormat);

    /** Fetches recent scores and results for a sport. */
    @GetExchange("/sports/{sport}/scores")
    List<ScoreResponse> getScores(
            @PathVariable("sport") String sport, @RequestParam(value = "daysFrom", required = false) Integer daysFrom);

    /** Fetches historical odds for a sport at a specific date. */
    @GetExchange("/sports/{sport}/odds-history")
    OddsHistoryResponse getHistoricalOdds(
            @PathVariable("sport") String sport,
            @RequestParam("regions") String regions,
            @RequestParam(value = "markets", required = false) String markets,
            @RequestParam("date") String date);
}
