package com.sportspredictor.tool;

import com.sportspredictor.service.PropPredictionService;
import com.sportspredictor.service.PropPredictionService.PropPredictionResult;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Service;

/** MCP tool for player prop predictions. */
@Service
@RequiredArgsConstructor
public class PropPredictionTool {

    private final PropPredictionService propPredictionService;

    /** Response record. */
    public record PropPredictionResponse(
            String playerId,
            String playerName,
            String propType,
            double projectedValue,
            double overUnderLine,
            String recommendation,
            double confidence,
            String summary) {}

    /** Generates a player prop prediction using matchup and pace data. */
    @Tool(
            name = "generate_prop_prediction",
            description = "Generate a player prop prediction using matchup, pace, and minutes projection")
    public PropPredictionResponse generatePropPrediction(
            @ToolParam(description = "Sport key") String sport,
            @ToolParam(description = "Player ID") String playerId,
            @ToolParam(description = "Prop type (e.g., points, rebounds, assists)") String propType,
            @ToolParam(description = "Event ID") String eventId) {
        PropPredictionResult r = propPredictionService.generatePropPrediction(sport, playerId, propType, eventId);
        return new PropPredictionResponse(
                r.playerId(),
                r.playerName(),
                r.propType(),
                r.projectedValue(),
                r.overUnderLine(),
                r.recommendation(),
                r.confidence(),
                r.summary());
    }
}
