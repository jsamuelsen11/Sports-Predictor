package com.sportspredictor.mcpserver.tool;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.within;

import com.sportspredictor.mcpserver.tool.KellyStakeTool.KellyStakeResponse;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/** Comprehensive tests for {@link KellyStakeTool}. */
class KellyStakeToolTest {

    private final KellyStakeTool tool = new KellyStakeTool();

    /** Tests for a bet with a clear positive edge. */
    @Nested
    class PositiveEdge {

        @Test
        void fullKellyIsPositive() {
            KellyStakeResponse result = tool.calculateKellyStake(0.60, -110, 1000.0);
            assertThat(result.fullKellyFraction()).isGreaterThan(0.0);
        }

        @Test
        void wagersComputedFromBankroll() {
            KellyStakeResponse result = tool.calculateKellyStake(0.60, -110, 1000.0);
            assertThat(result.fullKellyWager()).isNotNull();
            assertThat(result.halfKellyWager()).isNotNull();
            assertThat(result.quarterKellyWager()).isNotNull();
            assertThat(result.fullKellyWager()).isGreaterThan(0.0);
        }

        @Test
        void fieldsPopulated() {
            KellyStakeResponse result = tool.calculateKellyStake(0.60, -110, 1000.0);
            assertThat(result.americanOdds()).isEqualTo(-110);
            assertThat(result.predictedProbability()).isEqualTo(0.60);
            assertThat(result.decimalOdds()).isCloseTo(1.9091, within(0.001));
            assertThat(result.impliedProbability()).isCloseTo(0.5238, within(0.001));
            assertThat(result.edge()).isGreaterThan(0.0);
            assertThat(result.edgePercent()).isNotBlank();
        }
    }

    /** Tests for a bet with no positive edge. */
    @Nested
    class NoEdge {

        @Test
        void fullKellyClampsToZero() {
            KellyStakeResponse result = tool.calculateKellyStake(0.40, -110, null);
            assertThat(result.fullKellyFraction()).isEqualTo(0.0);
            assertThat(result.halfKellyFraction()).isEqualTo(0.0);
            assertThat(result.quarterKellyFraction()).isEqualTo(0.0);
        }

        @Test
        void recommendationIndicatesNoBet() {
            KellyStakeResponse result = tool.calculateKellyStake(0.40, -110, null);
            assertThat(result.recommendation()).contains("No bet recommended");
        }
    }

    /** Tests for a bet with a very strong edge. */
    @Nested
    class StrongEdge {

        @Test
        void recommendationMentionsStrongEdge() {
            KellyStakeResponse result = tool.calculateKellyStake(0.75, 150, null);
            assertThat(result.recommendation()).contains("Strong edge detected");
        }

        @Test
        void fullKellyIsSubstantial() {
            KellyStakeResponse result = tool.calculateKellyStake(0.75, 150, null);
            assertThat(result.fullKellyFraction()).isGreaterThan(0.10);
        }
    }

    /** Tests for even-money odds. */
    @Nested
    class EvenMoney {

        @Test
        void producesModerateKellyFraction() {
            KellyStakeResponse result = tool.calculateKellyStake(0.55, 100, null);
            assertThat(result.fullKellyFraction()).isGreaterThan(0.0);
            assertThat(result.fullKellyFraction()).isLessThan(0.5);
        }

        @Test
        void decimalOddsIsTwo() {
            KellyStakeResponse result = tool.calculateKellyStake(0.55, 100, null);
            assertThat(result.decimalOdds()).isCloseTo(2.0, within(0.001));
        }
    }

    /** Tests for a heavy underdog at +300. */
    @Nested
    class HeavyUnderdog {

        @Test
        void showsEdgeAtPlusThreeHundred() {
            KellyStakeResponse result = tool.calculateKellyStake(0.30, 300, null);
            assertThat(result.fullKellyFraction()).isGreaterThan(0.0);
            assertThat(result.edge()).isGreaterThan(0.0);
        }

        @Test
        void impliedProbabilityIsCorrect() {
            KellyStakeResponse result = tool.calculateKellyStake(0.30, 300, null);
            assertThat(result.impliedProbability()).isCloseTo(0.25, within(0.001));
        }
    }

    /** Tests confirming null wagers when no bankroll is provided. */
    @Nested
    class NoBankroll {

        @Test
        void wagersAreNull() {
            KellyStakeResponse result = tool.calculateKellyStake(0.60, -110, null);
            assertThat(result.fullKellyWager()).isNull();
            assertThat(result.halfKellyWager()).isNull();
            assertThat(result.quarterKellyWager()).isNull();
        }

        @Test
        void fractionsAreStillComputed() {
            KellyStakeResponse result = tool.calculateKellyStake(0.60, -110, null);
            assertThat(result.fullKellyFraction()).isGreaterThan(0.0);
        }
    }

    /** Tests verifying exact dollar wager amounts given a known bankroll. */
    @Nested
    class WithBankroll {

        @Test
        void fullKellyWagerMatchesFractionTimesBankroll() {
            KellyStakeResponse result = tool.calculateKellyStake(0.60, -110, 1000.0);
            double expectedFull = result.fullKellyFraction() * 1000.0;
            assertThat(result.fullKellyWager()).isCloseTo(expectedFull, within(0.001));
        }

        @Test
        void halfKellyWagerMatchesFractionTimesBankroll() {
            KellyStakeResponse result = tool.calculateKellyStake(0.60, -110, 1000.0);
            double expectedHalf = result.halfKellyFraction() * 1000.0;
            assertThat(result.halfKellyWager()).isCloseTo(expectedHalf, within(0.001));
        }

        @Test
        void quarterKellyWagerMatchesFractionTimesBankroll() {
            KellyStakeResponse result = tool.calculateKellyStake(0.60, -110, 1000.0);
            double expectedQuarter = result.quarterKellyFraction() * 1000.0;
            assertThat(result.quarterKellyWager()).isCloseTo(expectedQuarter, within(0.001));
        }
    }

    /** Tests for probability validation. */
    @Nested
    class InputValidation {

        @Test
        void zeroThrowsIllegalArgument() {
            assertThatIllegalArgumentException().isThrownBy(() -> tool.calculateKellyStake(0.0, -110, null));
        }

        @Test
        void oneThrowsIllegalArgument() {
            assertThatIllegalArgumentException().isThrownBy(() -> tool.calculateKellyStake(1.0, -110, null));
        }

        @Test
        void negativeThrowsIllegalArgument() {
            assertThatIllegalArgumentException().isThrownBy(() -> tool.calculateKellyStake(-0.5, -110, null));
        }
    }

    /** Tests for summary content. */
    @Nested
    class SummaryContent {

        @Test
        void summaryContainsOdds() {
            KellyStakeResponse result = tool.calculateKellyStake(0.60, -110, null);
            assertThat(result.summary()).contains("-110");
        }

        @Test
        void summaryContainsPredictedPercent() {
            KellyStakeResponse result = tool.calculateKellyStake(0.60, -110, null);
            assertThat(result.summary()).contains("60.00%");
        }

        @Test
        void summaryContainsEdge() {
            KellyStakeResponse result = tool.calculateKellyStake(0.60, -110, null);
            assertThat(result.summary()).contains("Edge:");
        }
    }

    /** Tests verifying the mathematical relationship between Kelly fractions. */
    @Nested
    class FractionRelationships {

        @Test
        void halfKellyIsFullKellyDividedByTwo() {
            KellyStakeResponse result = tool.calculateKellyStake(0.60, -110, null);
            assertThat(result.halfKellyFraction()).isCloseTo(result.fullKellyFraction() / 2.0, within(0.0001));
        }

        @Test
        void quarterKellyIsFullKellyDividedByFour() {
            KellyStakeResponse result = tool.calculateKellyStake(0.60, -110, null);
            assertThat(result.quarterKellyFraction()).isCloseTo(result.fullKellyFraction() / 4.0, within(0.0001));
        }

        @Test
        void relationsHoldForUnderdogBet() {
            KellyStakeResponse result = tool.calculateKellyStake(0.45, 200, null);
            assertThat(result.halfKellyFraction()).isCloseTo(result.fullKellyFraction() / 2.0, within(0.0001));
            assertThat(result.quarterKellyFraction()).isCloseTo(result.fullKellyFraction() / 4.0, within(0.0001));
        }
    }
}
