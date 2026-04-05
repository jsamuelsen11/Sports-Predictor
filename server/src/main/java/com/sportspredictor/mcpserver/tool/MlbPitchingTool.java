package com.sportspredictor.mcpserver.tool;

import com.sportspredictor.mcpserver.service.MlbPitchingService;
import com.sportspredictor.mcpserver.service.MlbPitchingService.PitchingMatchupResult;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Service;

/** MCP tool for MLB starting pitcher matchup comparison. */
@Service
@RequiredArgsConstructor
public class MlbPitchingTool {

    private final MlbPitchingService mlbPitchingService;

    /** Response record. */
    public record PitchingMatchupResponse(String eventId, String summary) {}

    /** Gets MLB starting pitcher comparison with splits. */
    @Tool(
            name = "get_mlb_pitching_matchup",
            description = "Starting pitcher comparison with ERA, WHIP, K/9, BB/9, and recent form")
    public PitchingMatchupResponse getMlbPitchingMatchup(
            @ToolParam(description = "Sport key (mlb)") String sport,
            @ToolParam(description = "Event ID") String eventId) {
        PitchingMatchupResult r = mlbPitchingService.getPitchingMatchup(sport, eventId);
        return new PitchingMatchupResponse(r.eventId(), r.summary());
    }
}
