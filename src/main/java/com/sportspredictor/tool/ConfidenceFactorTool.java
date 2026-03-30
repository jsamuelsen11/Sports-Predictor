package com.sportspredictor.tool;

import com.sportspredictor.service.ConfidenceFactorService;
import com.sportspredictor.service.ConfidenceFactorService.ConfidenceFactor;
import com.sportspredictor.service.ConfidenceFactorService.ConfidenceFactorsResult;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Service;

/** MCP tool for prediction confidence factor analysis. */
@Service
@RequiredArgsConstructor
public class ConfidenceFactorTool {

    private final ConfidenceFactorService confidenceFactorService;

    /** Response for get_prediction_confidence_factors. */
    public record ConfidenceFactorsResponse(
            String eventId, List<ConfidenceFactor> factors, double overallConfidence, String summary) {}

    /** Returns a factor-by-factor breakdown of prediction confidence. */
    @Tool(
            name = "get_prediction_confidence_factors",
            description = "Get a factor-by-factor breakdown of what increases/decreases"
                    + " prediction confidence: data quality, sample size, injury impact,"
                    + " weather uncertainty, model agreement, historical accuracy")
    public ConfidenceFactorsResponse getConfidenceFactors(
            @ToolParam(description = "Event ID (provide this or predictionId)", required = false) String eventId,
            @ToolParam(description = "Prediction ID (provide this or eventId)", required = false) String predictionId) {

        ConfidenceFactorsResult r = confidenceFactorService.getConfidenceFactors(eventId, predictionId);
        return new ConfidenceFactorsResponse(r.eventId(), r.factors(), r.overallConfidence(), r.summary());
    }
}
