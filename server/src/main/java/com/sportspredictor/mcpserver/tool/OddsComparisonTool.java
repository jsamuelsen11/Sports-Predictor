package com.sportspredictor.mcpserver.tool;

import com.sportspredictor.mcpserver.service.OddsComparisonService;
import java.util.List;
import java.util.Locale;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Service;

/** MCP tools for comparing odds across bookmakers and finding value bets. */
@Service
@RequiredArgsConstructor
public class OddsComparisonTool {

    private final OddsComparisonService oddsComparisonService;

    /** Response for odds comparison across bookmakers. */
    public record OddsComparisonResponse(OddsComparisonService.OddsComparison comparison, String summary) {}

    /** Response for value bets scan. */
    public record ValueBetsResponse(List<OddsComparisonService.ValueBet> valueBets, int totalFound, String summary) {}

    /** Compares odds for a specific event across all available bookmakers. */
    @Tool(
            name = "compare_odds_across_books",
            description = "Compare odds for a specific event across all available bookmakers. Shows"
                    + " side-by-side lines and highlights the best available odds for each"
                    + " outcome.")
    public OddsComparisonResponse compareOddsAcrossBooks(
            @ToolParam(description = "Sport key (e.g., nfl, nba)") String sport,
            @ToolParam(description = "Event ID to compare odds for") String eventId,
            @ToolParam(description = "Market type (h2h, spreads, totals)", required = false) String market) {

        OddsComparisonService.OddsComparison comparison = oddsComparisonService.compareOdds(sport, eventId, market);
        return new OddsComparisonResponse(comparison, comparison.summary());
    }

    /** Scans all available odds to find value bets where bookmaker lines differ from consensus. */
    @Tool(
            name = "find_value_bets",
            description = "Scan all available odds to find value bets where a bookmaker's line differs"
                    + " significantly from the consensus. Identifies mispriced lines across"
                    + " the market.")
    public ValueBetsResponse findValueBets(
            @ToolParam(description = "Sport key (e.g., nfl, nba)") String sport,
            @ToolParam(description = "Minimum edge percentage to qualify as value (e.g., 3.0 for 3%)")
                    double minEdgePct,
            @ToolParam(description = "Market type (h2h, spreads, totals)", required = false) String market) {

        List<OddsComparisonService.ValueBet> valueBets = oddsComparisonService.findValueBets(sport, market, minEdgePct);

        String summary = String.format(
                Locale.ROOT, "Found %d value bets for %s with edge >= %.1f%%.", valueBets.size(), sport, minEdgePct);

        return new ValueBetsResponse(valueBets, valueBets.size(), summary);
    }
}
