package com.sportspredictor.tool;

import com.sportspredictor.service.LineMovementService;
import com.sportspredictor.service.LineMovementService.ArbitrageResult;
import com.sportspredictor.service.LineMovementService.LineMovementResult;
import com.sportspredictor.service.LineMovementService.MarketConsensusResult;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Service;

/** MCP tools for line movement detection, arbitrage, and market consensus. */
@Service
@RequiredArgsConstructor
public class LineMovementTool {

    private final LineMovementService lineMovementService;

    /** Response for detect_line_movement. */
    public record LineMovementResponse(
            String eventId,
            String openingOdds,
            String currentOdds,
            String bookmaker,
            double delta,
            String direction,
            boolean isSharpMove,
            int snapshotCount,
            String summary) {}

    /** Response for find_arbitrage_opportunities. */
    public record ArbitrageResponse(int opportunityCount, int eventsScanned, String summary) {}

    /** Response for get_market_consensus. */
    public record MarketConsensusResponse(
            String eventId,
            double averageImpliedProbability,
            int bookmakerCount,
            Map<String, Double> oddsByBookmaker,
            String summary) {}

    /** Tracks line movement since open and identifies sharp vs public moves. */
    @Tool(
            name = "detect_line_movement",
            description = "Track line movement for an event since opening. Identifies sharp vs public moves"
                    + " based on movement magnitude")
    public LineMovementResponse detectLineMovement(@ToolParam(description = "Event ID to track") String eventId) {
        LineMovementResult r = lineMovementService.detectLineMovement(eventId);
        return new LineMovementResponse(
                r.eventId(),
                r.openingOddsData(),
                r.currentOddsData(),
                r.bookmaker(),
                r.delta(),
                r.direction(),
                r.isSharpMove(),
                r.snapshotCount(),
                r.summary());
    }

    /** Scans for arbitrage opportunities across bookmakers. */
    @Tool(
            name = "find_arbitrage_opportunities",
            description = "Scan for guaranteed-profit arbitrage opportunities across bookmakers for a sport")
    public ArbitrageResponse findArbitrageOpportunities(
            @ToolParam(description = "Sport key (e.g., nfl, nba)") String sport) {
        ArbitrageResult r = lineMovementService.findArbitrageOpportunities(sport);
        return new ArbitrageResponse(r.opportunities().size(), r.eventsScanned(), r.summary());
    }

    /** Gets market consensus odds across bookmakers. */
    @Tool(
            name = "get_market_consensus",
            description = "Average implied probability across all bookmakers as a true probability proxy")
    public MarketConsensusResponse getMarketConsensus(@ToolParam(description = "Event ID") String eventId) {
        MarketConsensusResult r = lineMovementService.getMarketConsensus(eventId);
        return new MarketConsensusResponse(
                r.eventId(), r.averageImpliedProbability(), r.bookmakerCount(), r.oddsByBookmaker(), r.summary());
    }
}
