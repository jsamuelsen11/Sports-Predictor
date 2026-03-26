package com.sportspredictor.tool;

import com.sportspredictor.service.DailyRankingService;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Service;

/** MCP tool for ranking today's best betting plays by expected value. */
@Service
@RequiredArgsConstructor
public class DailyRankingTool {

    private final DailyRankingService dailyRankingService;

    /** Daily card response wrapping the service result. */
    public record DailyCardResponse(
            String sport,
            String date,
            List<DailyRankingService.RankedPlay> rankedPlays,
            int totalGames,
            int qualifiedPlays,
            String summary) {}

    /** Scans all of today's games, generates predictions, and ranks by expected value. */
    @Tool(
            name = "rank_todays_plays",
            description = "Scan all of today's games for a sport, generate predictions for each, and rank"
                    + " them by expected value. Returns an ordered daily card with the best"
                    + " plays, confidence levels, and recommended bookmaker lines.")
    public DailyCardResponse rankTodaysPlays(
            @ToolParam(description = "Sport key (e.g., nfl, nba, mlb, nhl)") String sport,
            @ToolParam(description = "Minimum confidence threshold (0.0-1.0)", required = false) Double minConfidence,
            @ToolParam(description = "Minimum edge percentage (e.g., 3.0 for 3%)", required = false) Double minEdge) {

        DailyRankingService.DailyCardResult result = dailyRankingService.rankTodaysPlays(sport, minConfidence, minEdge);

        return new DailyCardResponse(
                result.sport(),
                result.date(),
                result.rankedPlays(),
                result.totalGames(),
                result.qualifiedPlays(),
                result.summary());
    }
}
