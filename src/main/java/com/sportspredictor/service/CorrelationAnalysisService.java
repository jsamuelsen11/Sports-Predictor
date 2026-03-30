package com.sportspredictor.service;

import com.sportspredictor.util.OddsUtil;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/** Analyzes correlations between parlay legs for SGP (Same-Game Parlay) pricing. */
@Service
@RequiredArgsConstructor
@Slf4j
public class CorrelationAnalysisService {

    private static final double STRONG_POSITIVE = 0.6;
    private static final double MODERATE_POSITIVE = 0.3;
    private static final double WEAK_THRESHOLD = 0.1;

    /** Correlation between two legs. */
    public record LegCorrelation(String leg1, String leg2, double correlationCoefficient, String explanation) {}

    /** Full correlation analysis result. */
    public record CorrelationResult(
            List<LegCorrelation> correlations,
            List<List<Double>> correlationMatrix,
            double adjustedCombinedProbability,
            double unadjustedCombinedProbability,
            String summary) {}

    /**
     * Analyzes correlations between legs in the same game. Correlated legs reduce the
     * independence assumption, adjusting the true combined probability.
     */
    public CorrelationResult analyzeCorrelations(
            String sport, String eventId, List<String> legDescriptions, List<Integer> legOdds) {

        if (legDescriptions == null || legDescriptions.size() < 2) {
            throw new IllegalArgumentException("Need at least 2 legs for correlation analysis");
        }
        if (legOdds == null || legOdds.size() != legDescriptions.size()) {
            throw new IllegalArgumentException("Leg descriptions and odds must have the same length");
        }

        int n = legDescriptions.size();
        double[][] matrix = new double[n][n];
        List<LegCorrelation> correlations = new ArrayList<>();

        for (int i = 0; i < n; i++) {
            matrix[i][i] = 1.0;
            for (int j = i + 1; j < n; j++) {
                double corr = estimateCorrelation(legDescriptions.get(i), legDescriptions.get(j));
                matrix[i][j] = corr;
                matrix[j][i] = corr;

                String explanation = describeCorrelation(corr);
                correlations.add(new LegCorrelation(legDescriptions.get(i), legDescriptions.get(j), corr, explanation));
            }
        }

        double unadjustedProb = 1.0;
        for (int odds : legOdds) {
            unadjustedProb *= OddsUtil.impliedProbability(odds);
        }

        double adjustedProb = adjustForCorrelation(legOdds, matrix);

        List<List<Double>> correlationMatrix = new ArrayList<>();
        for (double[] row : matrix) {
            List<Double> rowList = new ArrayList<>();
            for (double val : row) {
                rowList.add(val);
            }
            correlationMatrix.add(rowList);
        }

        String summary = String.format(
                Locale.ROOT,
                "Analyzed %d legs: unadjusted probability %.4f, correlation-adjusted %.4f (%.1f%% adjustment)",
                n,
                unadjustedProb,
                adjustedProb,
                ((adjustedProb / unadjustedProb) - 1) * 100);

        log.info("Correlation analysis completed for {} legs, event {}", n, eventId);

        return new CorrelationResult(correlations, correlationMatrix, adjustedProb, unadjustedProb, summary);
    }

    private double estimateCorrelation(String leg1, String leg2) {
        String l1 = leg1.toLowerCase(Locale.ROOT);
        String l2 = leg2.toLowerCase(Locale.ROOT);

        boolean samePlayer = sharesPlayer(l1, l2);
        boolean teamWinAndOver = (isTeamWin(l1) && isOver(l2)) || (isTeamWin(l2) && isOver(l1));
        boolean teamWinAndPlayerPoints = (isTeamWin(l1) && isPlayerPoints(l2)) || (isTeamWin(l2) && isPlayerPoints(l1));
        boolean bothPlayerProps = isPlayerProp(l1) && isPlayerProp(l2);

        if (samePlayer && bothPlayerProps) {
            return STRONG_POSITIVE;
        }
        if (teamWinAndPlayerPoints) {
            return MODERATE_POSITIVE;
        }
        if (teamWinAndOver) {
            return MODERATE_POSITIVE;
        }
        if (bothPlayerProps) {
            return WEAK_THRESHOLD;
        }

        return 0.0;
    }

    private static double adjustForCorrelation(List<Integer> legOdds, double[][] matrix) {
        int n = legOdds.size();
        double[] probs = new double[n];
        for (int i = 0; i < n; i++) {
            probs[i] = OddsUtil.impliedProbability(legOdds.get(i));
        }

        double independentProb = 1.0;
        for (double p : probs) {
            independentProb *= p;
        }

        double avgCorrelation = 0;
        int pairs = 0;
        for (int i = 0; i < n; i++) {
            for (int j = i + 1; j < n; j++) {
                avgCorrelation += matrix[i][j];
                pairs++;
            }
        }
        if (pairs > 0) {
            avgCorrelation /= pairs;
        }

        double adjustment = 1.0 + avgCorrelation * 0.5;
        return Math.min(independentProb * adjustment, 1.0);
    }

    private static boolean sharesPlayer(String l1, String l2) {
        String[] words1 = l1.split("\\s+", -1);
        String[] words2 = l2.split("\\s+", -1);
        for (String w1 : words1) {
            if (w1.length() > 3) {
                for (String w2 : words2) {
                    if (w1.equals(w2)) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    private static boolean isTeamWin(String leg) {
        return leg.contains(" ml") || leg.contains("moneyline") || leg.contains("to win");
    }

    private static boolean isOver(String leg) {
        return leg.contains("over ") || leg.contains("over/under");
    }

    private static boolean isPlayerPoints(String leg) {
        return leg.contains("points") || leg.contains("pts") || leg.contains("scoring");
    }

    private static boolean isPlayerProp(String leg) {
        return leg.contains("points")
                || leg.contains("rebounds")
                || leg.contains("assists")
                || leg.contains("strikeouts")
                || leg.contains("rushing")
                || leg.contains("passing")
                || leg.contains("goals")
                || leg.contains("shots");
    }

    private static String describeCorrelation(double corr) {
        if (corr >= STRONG_POSITIVE) {
            return "Strong positive correlation";
        }
        if (corr >= MODERATE_POSITIVE) {
            return "Moderate positive correlation";
        }
        if (corr >= WEAK_THRESHOLD) {
            return "Weak positive correlation";
        }
        if (corr > -WEAK_THRESHOLD) {
            return "No significant correlation";
        }
        if (corr > -MODERATE_POSITIVE) {
            return "Weak negative correlation";
        }
        if (corr > -STRONG_POSITIVE) {
            return "Moderate negative correlation";
        }
        return "Strong negative correlation";
    }
}
