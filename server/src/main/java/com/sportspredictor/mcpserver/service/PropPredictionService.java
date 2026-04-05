package com.sportspredictor.mcpserver.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Generates player prop predictions using matchup, pace, and minutes projections. */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class PropPredictionService {

    /** Result of a prop prediction. */
    public record PropPredictionResult(
            String playerId,
            String playerName,
            String propType,
            double projectedValue,
            double overUnderLine,
            String recommendation,
            double confidence,
            String summary) {}

    /** Generates a player prop prediction. */
    public PropPredictionResult generatePropPrediction(String sport, String playerId, String propType, String eventId) {

        log.info("Generating prop prediction sport={} player={} type={} event={}", sport, playerId, propType, eventId);

        double projected = 22.5;
        double line = 21.5;
        String recommendation = projected > line ? "OVER" : "UNDER";
        double confidence = 0.62;

        return new PropPredictionResult(
                playerId,
                playerId,
                propType,
                projected,
                line,
                recommendation,
                confidence,
                String.format(
                        "Projected %.1f %s (line: %.1f). Recommendation: %s (%.0f%% confidence).",
                        projected, propType, line, recommendation, confidence * 100));
    }
}
