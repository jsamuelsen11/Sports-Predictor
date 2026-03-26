package com.sportspredictor.tool;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sportspredictor.service.ParlayAnalysisService;
import com.sportspredictor.service.ParlayAnalysisService.ParlayAnalysisResult;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Service;

/** MCP tool for parlay analysis and correlation assessment. */
@Service
@RequiredArgsConstructor
public class ParlayAnalysisTool {

    private final ParlayAnalysisService parlayAnalysisService;
    private final ObjectMapper objectMapper;

    /** Response record. */
    public record ParlayAnalysisResponse(
            int legCount,
            double correlationScore,
            double trueProbability,
            double bookmakerImpliedProbability,
            double edgePercent,
            String recommendation,
            String summary) {}

    /** Evaluates parlay legs for correlation and true combined probability. */
    @Tool(
            name = "generate_parlay_analysis",
            description = "Evaluate parlay legs for correlation, true combined probability, and edge")
    public ParlayAnalysisResponse generateParlayAnalysis(
            @ToolParam(description = "Sport key") String sport,
            @ToolParam(description = "JSON array of leg descriptions") String legDescriptionsJson,
            @ToolParam(description = "JSON array of American odds per leg") String legOddsJson) {
        List<String> descriptions = parseStringList(legDescriptionsJson);
        List<Integer> odds = parseIntList(legOddsJson);
        ParlayAnalysisResult r = parlayAnalysisService.analyzeParlayLegs(sport, descriptions, odds);
        return new ParlayAnalysisResponse(
                r.legCount(),
                r.correlationScore(),
                r.trueProbability(),
                r.bookmakerImpliedProbability(),
                r.edgePercent(),
                r.recommendation(),
                r.summary());
    }

    private List<String> parseStringList(String json) {
        try {
            return objectMapper.readValue(json, new TypeReference<>() {});
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException("Invalid JSON array: " + e.getMessage(), e);
        }
    }

    private List<Integer> parseIntList(String json) {
        try {
            return objectMapper.readValue(json, new TypeReference<>() {});
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException("Invalid JSON array: " + e.getMessage(), e);
        }
    }
}
