package com.sportspredictor.mcpserver.service;

import com.sportspredictor.mcpserver.util.OddsUtil;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Evaluates parlay legs for correlation, true probability, and edge. */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class ParlayAnalysisService {

    /** Result of parlay analysis. */
    public record ParlayAnalysisResult(
            int legCount,
            double correlationScore,
            double trueProbability,
            double bookmakerImpliedProbability,
            double edgePercent,
            String recommendation,
            String summary) {}

    /** Analyzes parlay legs for correlation and edge. */
    public ParlayAnalysisResult analyzeParlayLegs(String sport, List<String> legDescriptions, List<Integer> legOdds) {

        int legCount = legOdds.size();

        // Calculate bookmaker implied probability (product of individual probs)
        double bookImplied = 1.0;
        for (int odds : legOdds) {
            bookImplied *= OddsUtil.impliedProbability(odds);
        }

        // Estimate correlation (same-sport legs are more correlated)
        double correlationScore = legCount > 2 ? 0.15 : 0.05;

        // True probability adjusts for correlation (correlated legs = higher true prob)
        double trueProbability = bookImplied * (1.0 + correlationScore);
        double edgePercent = (trueProbability - bookImplied) / bookImplied * 100;

        String recommendation;
        if (edgePercent > 5) {
            recommendation = "GOOD VALUE - positive edge detected";
        } else if (edgePercent > 0) {
            recommendation = "MARGINAL - slight edge but high variance";
        } else {
            recommendation = "AVOID - negative expected value";
        }

        log.info("Parlay analysis: {} legs, correlation={}, edge={}%", legCount, correlationScore, edgePercent);

        return new ParlayAnalysisResult(
                legCount,
                correlationScore,
                trueProbability,
                bookImplied,
                edgePercent,
                recommendation,
                String.format(
                        "%d-leg parlay: %.2f%% implied prob, %.1f%% edge. %s",
                        legCount, bookImplied * 100, edgePercent, recommendation));
    }
}
