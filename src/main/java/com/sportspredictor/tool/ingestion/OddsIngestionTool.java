package com.sportspredictor.tool.ingestion;

import com.sportspredictor.service.OddsService;
import java.util.List;
import java.util.Locale;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Service;

/** MCP tools for fetching live and historical odds data. */
@Service
@RequiredArgsConstructor
public class OddsIngestionTool {

    private final OddsService oddsService;

    /** Response for live odds queries. */
    public record LiveOddsResponse(
            String sport,
            String eventId,
            String market,
            List<OddsService.EventOdds> events,
            int eventCount,
            String summary) {}

    /** Response for historical odds queries. */
    public record HistoricalOddsResponse(
            String sport, String date, String timestamp, List<OddsService.EventOdds> events, String summary) {}

    /** Fetches live betting odds for a sport with optional event and market filtering. */
    @Tool(
            name = "get_live_odds",
            description = "Fetch current betting odds for a sport from multiple bookmakers. Returns odds for all"
                    + " upcoming events, optionally filtered by event ID and market type.")
    public LiveOddsResponse getLiveOdds(
            @ToolParam(description = "Sport key (e.g., nfl, nba, mlb, nhl, epl)") String sport,
            @ToolParam(description = "Specific event ID to filter results", required = false) String eventId,
            @ToolParam(description = "Market type: h2h, spreads, or totals (default: h2h)", required = false)
                    String market) {
        var result = oddsService.getLiveOdds(sport, eventId, market);

        String summary;
        if (result.eventCount() == 0) {
            summary = String.format(Locale.ROOT, "No odds found for %s.", sport);
        } else {
            long totalBookmakers = result.events().stream()
                    .mapToLong(e -> e.bookmakers().size())
                    .sum();
            summary = String.format(
                    Locale.ROOT,
                    "Found odds for %d %s events across %d bookmaker entries. Market: %s.",
                    result.eventCount(),
                    sport.toUpperCase(Locale.ROOT),
                    totalBookmakers,
                    result.market());
        }

        return new LiveOddsResponse(
                result.sport(), result.eventId(), result.market(), result.events(), result.eventCount(), summary);
    }

    /** Fetches a historical odds snapshot for a sport at a specific date for backtesting analysis. */
    @Tool(
            name = "get_historical_odds",
            description = "Fetch historical odds snapshot for a sport at a specific date. Useful for backtesting"
                    + " and closing line value analysis.")
    public HistoricalOddsResponse getHistoricalOdds(
            @ToolParam(description = "Sport key (e.g., nfl, nba)") String sport,
            @ToolParam(description = "Date in ISO-8601 format (e.g., 2026-03-20)") String date) {
        var result = oddsService.getHistoricalOdds(sport, date);

        String summary;
        if (result.events().isEmpty()) {
            summary = String.format(Locale.ROOT, "No historical odds found for %s on %s.", sport, date);
        } else {
            summary = String.format(
                    Locale.ROOT,
                    "Found historical odds for %d %s events on %s (snapshot: %s).",
                    result.events().size(),
                    sport.toUpperCase(Locale.ROOT),
                    date,
                    result.timestamp());
        }

        return new HistoricalOddsResponse(result.sport(), result.date(), result.timestamp(), result.events(), summary);
    }
}
