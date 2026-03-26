package com.sportspredictor.tool;

import com.sportspredictor.service.PowerRankingsService;
import com.sportspredictor.service.PowerRankingsService.PowerRankingsResult;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Service;

/** MCP tool for quantitative power rankings. */
@Service
@RequiredArgsConstructor
public class PowerRankingsTool {

    private final PowerRankingsService powerRankingsService;

    /** Response record. */
    public record PowerRankingsResponse(String sport, String method, int teamCount, String summary) {}

    /** Runs power rankings using SRS, Elo, net rating, or all methods. */
    @Tool(name = "run_power_rankings", description = "Quantitative power rankings using SRS, Elo, or net rating")
    public PowerRankingsResponse runPowerRankings(
            @ToolParam(description = "Sport key") String sport,
            @ToolParam(description = "Method: srs, elo, net_rating, or all") String method) {
        PowerRankingsResult r = powerRankingsService.runPowerRankings(sport, method);
        return new PowerRankingsResponse(r.sport(), r.method(), r.rankings().size(), r.summary());
    }
}
