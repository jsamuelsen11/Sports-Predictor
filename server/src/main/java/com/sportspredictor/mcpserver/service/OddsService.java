package com.sportspredictor.mcpserver.service;

import com.sportspredictor.mcpserver.client.oddsapi.OddsApiClient;
import com.sportspredictor.mcpserver.client.oddsapi.OddsHistoryResponse;
import com.sportspredictor.mcpserver.client.oddsapi.OddsResponse;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

/** Fetches live and historical odds from The Odds API with caching. */
@Service
@RequiredArgsConstructor
@Slf4j
public class OddsService {

    private static final String DEFAULT_REGIONS = "us";
    private static final String DEFAULT_MARKET = "h2h";
    private static final String DEFAULT_ODDS_FORMAT = "american";

    private final OddsApiClient oddsApiClient;
    private final SportLeagueMapping sportLeagueMapping;

    /** Result record for live odds queries. */
    public record LiveOddsResult(String sport, String eventId, String market, List<EventOdds> events, int eventCount) {}

    /** Odds for a single event across multiple bookmakers. */
    public record EventOdds(
            String id, String homeTeam, String awayTeam, String commenceTime, List<BookmakerOdds> bookmakers) {}

    /** A single bookmaker's odds for an event. */
    public record BookmakerOdds(String key, String title, List<MarketOdds> markets) {}

    /** A market with outcomes from a bookmaker. */
    public record MarketOdds(String key, List<OutcomeOdds> outcomes) {}

    /** A single outcome with price and optional point. */
    public record OutcomeOdds(String name, double price, Double point) {}

    /** Result record for historical odds queries. */
    public record HistoricalOddsResult(String sport, String date, String timestamp, List<EventOdds> events) {}

    /** Returns cached live odds for the given sport, event, and market type. */
    @Cacheable(value = "odds", key = "#sport + '-' + #eventId + '-' + #market")
    public LiveOddsResult getLiveOdds(String sport, String eventId, String market) {
        var info = sportLeagueMapping.resolve(sport);
        String resolvedMarket = (market != null && !market.isBlank()) ? market : DEFAULT_MARKET;

        try {
            List<OddsResponse> response =
                    oddsApiClient.getOdds(info.oddsApiKey(), DEFAULT_REGIONS, resolvedMarket, DEFAULT_ODDS_FORMAT);

            List<EventOdds> events = response.stream()
                    .filter(e -> eventId == null || eventId.isBlank() || e.id().equals(eventId))
                    .map(this::toEventOdds)
                    .toList();

            return new LiveOddsResult(sport, eventId, resolvedMarket, events, events.size());
        } catch (Exception e) {
            log.warn("Failed to fetch live odds for sport={}: {}", sport, e.getMessage());
            return new LiveOddsResult(sport, eventId, resolvedMarket, List.of(), 0);
        }
    }

    /** Returns a cached historical odds snapshot for the given sport and date. */
    @Cacheable(value = "odds", key = "'hist-' + #sport + '-' + #date")
    public HistoricalOddsResult getHistoricalOdds(String sport, String date) {
        var info = sportLeagueMapping.resolve(sport);

        try {
            OddsHistoryResponse response =
                    oddsApiClient.getHistoricalOdds(info.oddsApiKey(), DEFAULT_REGIONS, DEFAULT_MARKET, date);

            List<EventOdds> events =
                    response.data().stream().map(this::toEventOdds).toList();

            return new HistoricalOddsResult(sport, date, response.timestamp(), events);
        } catch (Exception e) {
            log.warn("Failed to fetch historical odds for sport={}, date={}: {}", sport, date, e.getMessage());
            return new HistoricalOddsResult(sport, date, null, List.of());
        }
    }

    private EventOdds toEventOdds(OddsResponse r) {
        List<BookmakerOdds> bookmakers = r.bookmakers().stream()
                .map(b -> new BookmakerOdds(
                        b.key(),
                        b.title(),
                        b.markets().stream()
                                .map(m -> new MarketOdds(
                                        m.key(),
                                        m.outcomes().stream()
                                                .map(o -> new OutcomeOdds(o.name(), o.price(), o.point()))
                                                .toList()))
                                .toList()))
                .toList();
        return new EventOdds(r.id(), r.homeTeam(), r.awayTeam(), r.commenceTime(), bookmakers);
    }
}
