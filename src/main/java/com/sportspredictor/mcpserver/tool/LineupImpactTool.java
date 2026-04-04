package com.sportspredictor.mcpserver.tool;

import com.sportspredictor.mcpserver.service.LineupImpactService;
import com.sportspredictor.mcpserver.service.LineupImpactService.LineupImpactResult;
import com.sportspredictor.mcpserver.service.LineupImpactService.LineupPlayer;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Service;

/** MCP tool for NBA lineup impact analysis. */
@Service
@RequiredArgsConstructor
public class LineupImpactTool {

    private final LineupImpactService lineupImpactService;

    /** Response for get_nba_lineup_impact. */
    public record LineupImpactResponse(
            String teamId,
            List<LineupPlayer> lineup,
            double projectedImpact,
            double offensiveRating,
            double defensiveRating,
            String summary) {}

    /** Analyzes NBA team lineup impact based on roster composition. */
    @Tool(
            name = "get_nba_lineup_impact",
            description = "Analyze NBA team lineup impact. Evaluates player roles and contributions"
                    + " to projected offensive and defensive ratings")
    public LineupImpactResponse getNbaLineupImpact(@ToolParam(description = "NBA team ID") String teamId) {

        LineupImpactResult r = lineupImpactService.getNbaLineupImpact(teamId);
        return new LineupImpactResponse(
                r.teamId(), r.lineup(), r.projectedImpact(), r.offensiveRating(), r.defensiveRating(), r.summary());
    }
}
