package com.sportspredictor.mcpserver.tool;

import com.sportspredictor.mcpserver.util.OddsUtil;
import java.util.Locale;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Service;

/** MCP tools for odds analysis: implied probability, expected value, and Kelly Criterion sizing. */
@Service
public class OddsTool {

    private static final double STRONG_EV_THRESHOLD = 0.05;
    private static final double PERCENT_MULTIPLIER = 100.0;

    /** Response containing odds conversions, implied probability, and optional vig analysis. */
    public record ImpliedProbabilityResponse(
            int americanOdds,
            double decimalOdds,
            String fractionalOdds,
            double impliedProbability,
            String impliedProbabilityPercent,
            Double vig,
            String vigPercent,
            Double trueProbability,
            String trueProbabilityPercent,
            String summary) {}

    /** Kelly Criterion staking recommendation. */
    public record KellyRecommendation(
            double fullKelly,
            double halfKelly,
            double quarterKelly,
            Double fullKellyWager,
            Double halfKellyWager,
            Double quarterKellyWager,
            String recommendation) {}

    /** Response containing EV analysis with Kelly Criterion sizing. */
    public record ExpectedValueResponse(
            int americanOdds,
            double decimalOdds,
            double predictedProbability,
            String predictedProbabilityPercent,
            double impliedProbability,
            double edge,
            String edgePercent,
            double expectedValuePerUnit,
            String evRating,
            KellyRecommendation kelly,
            String summary) {}

    /** Converts American odds to all formats with implied probability and optional vig analysis. */
    @Tool(
            name = "calculate_implied_probability",
            description = "Convert American odds to decimal and fractional formats, show implied probability,"
                    + " and optionally calculate vig/juice when both sides of a two-way market are"
                    + " provided")
    public ImpliedProbabilityResponse calculateImpliedProbability(
            @ToolParam(description = "American odds for the primary side (e.g., -110, +150)") int odds,
            @ToolParam(
                            description = "American odds for the opposing side, to calculate vig and true"
                                    + " probability (e.g., if primary is -110, opposing might be"
                                    + " -110)",
                            required = false)
                    Integer odds2) {

        double decimalOdds = OddsUtil.americanToDecimal(odds);
        String fractionalOdds = OddsUtil.americanToFractional(odds);
        double impliedProb = OddsUtil.impliedProbability(odds);

        Double vig = null;
        String vigPercent = null;
        Double trueProb = null;
        String trueProbPercent = null;

        StringBuilder summaryBuilder = new StringBuilder();
        summaryBuilder.append(String.format(
                Locale.ROOT,
                "Odds of %+d imply a %.2f%% chance. Decimal: %.4f, Fractional: %s.",
                odds,
                impliedProb * PERCENT_MULTIPLIER,
                decimalOdds,
                fractionalOdds));

        if (odds2 != null) {
            vig = OddsUtil.calculateVig(odds, odds2);
            vigPercent = formatPercent(vig);
            trueProb = OddsUtil.trueProbability(odds, odds2);
            trueProbPercent = formatPercent(trueProb);
            summaryBuilder.append(String.format(
                    Locale.ROOT,
                    " Market vig: %.2f%%. True probability (vig removed): %.2f%%.",
                    vig * PERCENT_MULTIPLIER,
                    trueProb * PERCENT_MULTIPLIER));
        }

        return new ImpliedProbabilityResponse(
                odds,
                decimalOdds,
                fractionalOdds,
                impliedProb,
                formatPercent(impliedProb),
                vig,
                vigPercent,
                trueProb,
                trueProbPercent,
                summaryBuilder.toString());
    }

    /** Calculates expected value and Kelly Criterion staking for a bet. */
    @Tool(
            name = "calculate_expected_value",
            description = "Calculate expected value of a bet given your predicted probability, plus Kelly"
                    + " Criterion bankroll sizing recommendations. Positive EV indicates a"
                    + " profitable bet over time.")
    public ExpectedValueResponse calculateExpectedValue(
            @ToolParam(description = "Your predicted probability of the outcome (0.0 to 1.0, e.g., 0.55" + " for 55%)")
                    double predictedProbability,
            @ToolParam(description = "American odds offered by the sportsbook (e.g., -110, +150)") int odds,
            @ToolParam(
                            description = "Total bankroll in dollars for Kelly wager sizing (e.g., 1000.0)",
                            required = false)
                    Double bankroll) {

        validateProbability(predictedProbability);

        double decimalOdds = OddsUtil.americanToDecimal(odds);
        double impliedProb = OddsUtil.impliedProbability(odds);
        double edge = predictedProbability - impliedProb;

        double netOdds = decimalOdds - 1.0;
        double ev = (predictedProbability * netOdds) - (1.0 - predictedProbability);

        String evRating;
        if (ev > STRONG_EV_THRESHOLD) {
            evRating = "STRONG_POSITIVE";
        } else if (ev > 0) {
            evRating = "POSITIVE";
        } else {
            evRating = "NEGATIVE";
        }

        KellyRecommendation kelly = buildKellyRecommendation(predictedProbability, netOdds, bankroll);

        String summary = String.format(
                Locale.ROOT,
                "EV: %+.4f per unit (%s). Edge: %+.2f%%. Predicted: %.2f%%, Implied: %.2f%%. %s",
                ev,
                evRating,
                edge * PERCENT_MULTIPLIER,
                predictedProbability * PERCENT_MULTIPLIER,
                impliedProb * PERCENT_MULTIPLIER,
                kelly.recommendation());

        return new ExpectedValueResponse(
                odds,
                decimalOdds,
                predictedProbability,
                formatPercent(predictedProbability),
                impliedProb,
                edge,
                formatPercent(edge),
                ev,
                evRating,
                kelly,
                summary);
    }

    private static KellyRecommendation buildKellyRecommendation(
            double predictedProbability, double netOdds, Double bankroll) {
        double fullKelly = Math.max(0.0, (netOdds * predictedProbability - (1.0 - predictedProbability)) / netOdds);
        double halfKelly = fullKelly / 2.0;
        double quarterKelly = fullKelly / 4.0;

        Double fullWager = null;
        Double halfWager = null;
        Double quarterWager = null;

        StringBuilder rec = new StringBuilder();

        if (fullKelly == 0.0) {
            rec.append("No bet recommended — no edge detected.");
        } else {
            rec.append(
                    String.format(Locale.ROOT, "Kelly suggests %.2f%% of bankroll.", fullKelly * PERCENT_MULTIPLIER));
            if (bankroll != null) {
                fullWager = fullKelly * bankroll;
                halfWager = halfKelly * bankroll;
                quarterWager = quarterKelly * bankroll;
                rec.append(String.format(
                        Locale.ROOT,
                        " On $%.2f bankroll: full $%.2f, half $%.2f, quarter $%.2f.",
                        bankroll,
                        fullWager,
                        halfWager,
                        quarterWager));
            }
        }

        return new KellyRecommendation(
                fullKelly, halfKelly, quarterKelly, fullWager, halfWager, quarterWager, rec.toString());
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
