package com.sportspredictor.mcpserver.tool;

import com.sportspredictor.mcpserver.util.OddsUtil;
import java.util.Locale;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Service;

/** MCP tool for Kelly Criterion optimal bet sizing given predicted probability and offered odds. */
@Service
public class KellyStakeTool {

    private static final double PERCENT_MULTIPLIER = 100.0;
    private static final double STRONG_EDGE_THRESHOLD = 0.10;
    private static final double GOOD_EDGE_THRESHOLD = 0.05;
    private static final double HALF_DIVISOR = 2.0;
    private static final double QUARTER_DIVISOR = 4.0;

    /** Kelly Criterion staking response with fractions, optional wager amounts, and recommendation. */
    public record KellyStakeResponse(
            double predictedProbability,
            int americanOdds,
            double decimalOdds,
            double impliedProbability,
            double edge,
            String edgePercent,
            double fullKellyFraction,
            double halfKellyFraction,
            double quarterKellyFraction,
            Double fullKellyWager,
            Double halfKellyWager,
            Double quarterKellyWager,
            String recommendation,
            String summary) {}

    /** Calculates optimal Kelly Criterion bet sizing for a given predicted probability and odds. */
    @Tool(
            name = "calculate_kelly_stake",
            description = "Calculate optimal bet size using Kelly Criterion given your predicted probability"
                    + " and the offered odds. Returns full, half, and quarter Kelly fractions"
                    + " with optional dollar wager amounts.")
    public KellyStakeResponse calculateKellyStake(
            @ToolParam(description = "Your predicted probability of the outcome (0.0 to 1.0, e.g., 0.55" + " for 55%)")
                    double predictedProbability,
            @ToolParam(description = "American odds offered by the sportsbook (e.g., -110, +150)") int odds,
            @ToolParam(description = "Total bankroll in dollars for wager sizing (e.g., 1000.0)", required = false)
                    Double bankroll) {

        validateProbability(predictedProbability);

        double decimalOdds = OddsUtil.americanToDecimal(odds);
        double impliedProbability = OddsUtil.impliedProbability(odds);
        double edge = predictedProbability - impliedProbability;

        double netOdds = decimalOdds - 1.0;
        double fullKelly = computeFullKelly(predictedProbability, netOdds);
        double halfKelly = fullKelly / HALF_DIVISOR;
        double quarterKelly = fullKelly / QUARTER_DIVISOR;

        Double fullKellyWager = null;
        Double halfKellyWager = null;
        Double quarterKellyWager = null;

        if (bankroll != null) {
            fullKellyWager = fullKelly * bankroll;
            halfKellyWager = halfKelly * bankroll;
            quarterKellyWager = quarterKelly * bankroll;
        }

        String recommendation =
                buildRecommendation(fullKelly, edge, bankroll, fullKellyWager, halfKellyWager, quarterKellyWager);
        String summary = buildSummary(
                predictedProbability,
                odds,
                decimalOdds,
                impliedProbability,
                edge,
                fullKelly,
                halfKelly,
                quarterKelly,
                bankroll,
                fullKellyWager);

        return new KellyStakeResponse(
                predictedProbability,
                odds,
                decimalOdds,
                impliedProbability,
                edge,
                formatPercent(edge),
                fullKelly,
                halfKelly,
                quarterKelly,
                fullKellyWager,
                halfKellyWager,
                quarterKellyWager,
                recommendation,
                summary);
    }

    private static double computeFullKelly(double predictedProbability, double netOdds) {
        double q = 1.0 - predictedProbability;
        return Math.max(0.0, (netOdds * predictedProbability - q) / netOdds);
    }

    private static String buildRecommendation(
            double fullKelly,
            double edge,
            Double bankroll,
            Double fullKellyWager,
            Double halfKellyWager,
            Double quarterKellyWager) {

        if (fullKelly == 0.0) {
            return "No bet recommended -- no edge detected.";
        }

        String edgeMessage;
        if (edge > STRONG_EDGE_THRESHOLD) {
            edgeMessage = String.format(
                    Locale.ROOT,
                    "Strong edge detected (%.2f%%). Consider half Kelly for safety.",
                    edge * PERCENT_MULTIPLIER);
        } else if (edge > GOOD_EDGE_THRESHOLD) {
            edgeMessage = String.format(
                    Locale.ROOT, "Good edge detected (%.2f%%). Half Kelly recommended.", edge * PERCENT_MULTIPLIER);
        } else {
            edgeMessage = String.format(
                    Locale.ROOT,
                    "Marginal edge (%.2f%%). Quarter Kelly recommended for safety.",
                    edge * PERCENT_MULTIPLIER);
        }

        if (bankroll != null) {
            return edgeMessage
                    + String.format(
                            Locale.ROOT,
                            " On $%.2f bankroll: full $%.2f, half $%.2f, quarter $%.2f.",
                            bankroll,
                            fullKellyWager,
                            halfKellyWager,
                            quarterKellyWager);
        }

        return edgeMessage;
    }

    private static String buildSummary(
            double predictedProbability,
            int odds,
            double decimalOdds,
            double impliedProbability,
            double edge,
            double fullKelly,
            double halfKelly,
            double quarterKelly,
            Double bankroll,
            Double fullKellyWager) {

        StringBuilder sb = new StringBuilder();
        sb.append(String.format(
                Locale.ROOT,
                "Predicted: %.2f%%, Implied: %.2f%% at %+d (%.4f decimal). Edge: %+.2f%%.",
                predictedProbability * PERCENT_MULTIPLIER,
                impliedProbability * PERCENT_MULTIPLIER,
                odds,
                decimalOdds,
                edge * PERCENT_MULTIPLIER));

        if (fullKelly == 0.0) {
            sb.append(" No Kelly stake recommended.");
        } else {
            sb.append(String.format(
                    Locale.ROOT,
                    " Full Kelly: %.4f, Half Kelly: %.4f, Quarter Kelly: %.4f.",
                    fullKelly,
                    halfKelly,
                    quarterKelly));
            if (bankroll != null) {
                sb.append(String.format(
                        Locale.ROOT, " Full Kelly wager on $%.2f bankroll: $%.2f.", bankroll, fullKellyWager));
            }
        }

        return sb.toString();
    }

    private static void validateProbability(double probability) {
        if (probability <= 0.0 || probability >= 1.0) {
            throw new IllegalArgumentException(
                    "Predicted probability must be between 0.0 and 1.0 exclusive, got: " + probability);
        }
    }

    private static String formatPercent(double value) {
        return String.format(Locale.ROOT, "%.2f%%", value * PERCENT_MULTIPLIER);
    }
}
