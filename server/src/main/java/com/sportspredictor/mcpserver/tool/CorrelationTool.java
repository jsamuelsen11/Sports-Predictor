package com.sportspredictor.mcpserver.tool;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sportspredictor.mcpserver.service.CorrelationAnalysisService;
import com.sportspredictor.mcpserver.service.CorrelationAnalysisService.CorrelationResult;
import com.sportspredictor.mcpserver.service.CorrelationAnalysisService.LegCorrelation;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Service;

/** MCP tool for parlay leg correlation analysis. */
@Service
@RequiredArgsConstructor
public class CorrelationTool {

    private final CorrelationAnalysisService correlationAnalysisService;
    private final ObjectMapper objectMapper;

    /** Response for analyze_correlations. */
    public record CorrelationResponse(
            List<LegCorrelation> correlations,
            double adjustedCombinedProbability,
            double unadjustedCombinedProbability,
            String summary) {}

    /** Analyzes correlations between parlay legs for SGP analysis. */
    @Tool(
            name = "analyze_correlations",
            description = "Analyze correlations between parlay legs for same-game parlay (SGP) pricing."
                    + " Returns correlation matrix and adjusted combined probability")
    public CorrelationResponse analyzeCorrelations(
            @ToolParam(description = "Sport key (e.g., nba, nfl)") String sport,
            @ToolParam(description = "Event ID — all legs must be from the same game") String eventId,
            @ToolParam(
                            description = "JSON array of leg descriptions"
                                    + " (e.g., [\"Lakers ML\",\"LeBron over 25.5 pts\"])")
                    String legDescriptionsJson,
            @ToolParam(description = "JSON array of American odds for each leg (e.g., [-150, -110])")
                    String legOddsJson) {

        List<String> descriptions = parseJsonArray(legDescriptionsJson, new TypeReference<>() {});
        List<Integer> odds = parseJsonArray(legOddsJson, new TypeReference<>() {});

        CorrelationResult r = correlationAnalysisService.analyzeCorrelations(sport, eventId, descriptions, odds);

        return new CorrelationResponse(
                r.correlations(), r.adjustedCombinedProbability(), r.unadjustedCombinedProbability(), r.summary());
    }

    private <T> T parseJsonArray(String json, TypeReference<T> typeRef) {
        try {
            return objectMapper.readValue(json, typeRef);
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException("Invalid JSON array: " + e.getMessage(), e);
        }
    }
}
