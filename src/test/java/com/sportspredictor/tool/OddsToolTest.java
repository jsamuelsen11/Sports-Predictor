package com.sportspredictor.tool;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.within;

import com.sportspredictor.tool.OddsTool.ExpectedValueResponse;
import com.sportspredictor.tool.OddsTool.ImpliedProbabilityResponse;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/** Comprehensive tests for {@link OddsTool}. */
class OddsToolTest {

    private final OddsTool tool = new OddsTool();

    /** Tests for {@link OddsTool#calculateImpliedProbability(int, Integer)}. */
    @Nested
    class CalculateImpliedProbability {

        @Test
        void standardFavorite() {
            ImpliedProbabilityResponse result = tool.calculateImpliedProbability(-110, null);
            assertThat(result.americanOdds()).isEqualTo(-110);
            assertThat(result.decimalOdds()).isCloseTo(1.9091, within(0.001));
            assertThat(result.fractionalOdds()).isEqualTo("10/11");
            assertThat(result.impliedProbability()).isCloseTo(0.5238, within(0.001));
            assertThat(result.impliedProbabilityPercent()).isEqualTo("52.38%");
        }

        @Test
        void standardUnderdog() {
            ImpliedProbabilityResponse result = tool.calculateImpliedProbability(150, null);
            assertThat(result.decimalOdds()).isCloseTo(2.5, within(0.001));
            assertThat(result.fractionalOdds()).isEqualTo("3/2");
            assertThat(result.impliedProbability()).isCloseTo(0.4, within(0.001));
        }

        @Test
        void evenMoney() {
            ImpliedProbabilityResponse result = tool.calculateImpliedProbability(100, null);
            assertThat(result.decimalOdds()).isCloseTo(2.0, within(0.001));
            assertThat(result.impliedProbability()).isCloseTo(0.5, within(0.001));
        }

        @Test
        void heavyFavorite() {
            ImpliedProbabilityResponse result = tool.calculateImpliedProbability(-500, null);
            assertThat(result.impliedProbability()).isCloseTo(0.8333, within(0.001));
        }

        @Test
        void heavyUnderdog() {
            ImpliedProbabilityResponse result = tool.calculateImpliedProbability(500, null);
            assertThat(result.impliedProbability()).isCloseTo(0.1667, within(0.001));
        }

        @Test
        void withoutOpposingOddsVigFieldsAreNull() {
            ImpliedProbabilityResponse result = tool.calculateImpliedProbability(-110, null);
            assertThat(result.vig()).isNull();
            assertThat(result.vigPercent()).isNull();
            assertThat(result.trueProbability()).isNull();
            assertThat(result.trueProbabilityPercent()).isNull();
        }

        @Test
        void withVigCalculation() {
            ImpliedProbabilityResponse result = tool.calculateImpliedProbability(-110, -110);
            assertThat(result.vig()).isCloseTo(0.0476, within(0.001));
            assertThat(result.trueProbability()).isCloseTo(0.5, within(0.001));
            assertThat(result.vigPercent()).isEqualTo("4.76%");
            assertThat(result.trueProbabilityPercent()).isEqualTo("50.00%");
        }

        @Test
        void vigWithSkewedLine() {
            ImpliedProbabilityResponse result = tool.calculateImpliedProbability(-150, 130);
            assertThat(result.vig()).isGreaterThan(0.0);
            assertThat(result.trueProbability()).isGreaterThan(0.5);
        }

        @Test
        void summaryIsNonEmpty() {
            ImpliedProbabilityResponse result = tool.calculateImpliedProbability(-110, null);
            assertThat(result.summary()).isNotBlank();
            assertThat(result.summary()).contains("-110");
        }

        @Test
        void summaryIncludesVigWhenProvided() {
            ImpliedProbabilityResponse result = tool.calculateImpliedProbability(-110, -110);
            assertThat(result.summary()).contains("vig");
        }

        @Test
        void rejectsInvalidOdds() {
            assertThatIllegalArgumentException().isThrownBy(() -> tool.calculateImpliedProbability(0, null));
        }

        @Test
        void rejectsInvalidOpposingOdds() {
            assertThatIllegalArgumentException().isThrownBy(() -> tool.calculateImpliedProbability(-110, 50));
        }
    }

    /** Tests for {@link OddsTool#calculateExpectedValue(double, int, Double)}. */
    @Nested
    class CalculateExpectedValue {

        @Test
        void positiveEv() {
            ExpectedValueResponse result = tool.calculateExpectedValue(0.55, -110, null);
            assertThat(result.expectedValuePerUnit()).isGreaterThan(0.0);
            assertThat(result.edge()).isGreaterThan(0.0);
            assertThat(result.evRating()).isIn("POSITIVE", "STRONG_POSITIVE");
        }

        @Test
        void negativeEv() {
            ExpectedValueResponse result = tool.calculateExpectedValue(0.45, -110, null);
            assertThat(result.expectedValuePerUnit()).isLessThan(0.0);
            assertThat(result.edge()).isLessThan(0.0);
            assertThat(result.evRating()).isEqualTo("NEGATIVE");
        }

        @Test
        void strongPositiveEv() {
            ExpectedValueResponse result = tool.calculateExpectedValue(0.70, -110, null);
            assertThat(result.evRating()).isEqualTo("STRONG_POSITIVE");
        }

        @Test
        void kellyWithBankroll() {
            ExpectedValueResponse result = tool.calculateExpectedValue(0.55, -110, 1000.0);
            assertThat(result.kelly().fullKellyWager()).isNotNull();
            assertThat(result.kelly().halfKellyWager()).isNotNull();
            assertThat(result.kelly().quarterKellyWager()).isNotNull();
            assertThat(result.kelly().fullKellyWager()).isGreaterThan(0.0);
        }

        @Test
        void kellyWithoutBankroll() {
            ExpectedValueResponse result = tool.calculateExpectedValue(0.55, -110, null);
            assertThat(result.kelly().fullKellyWager()).isNull();
            assertThat(result.kelly().halfKellyWager()).isNull();
            assertThat(result.kelly().quarterKellyWager()).isNull();
        }

        @Test
        void kellyClampsToZeroWhenNegativeEdge() {
            ExpectedValueResponse result = tool.calculateExpectedValue(0.40, -110, 1000.0);
            assertThat(result.kelly().fullKelly()).isEqualTo(0.0);
            assertThat(result.kelly().halfKelly()).isEqualTo(0.0);
            assertThat(result.kelly().quarterKelly()).isEqualTo(0.0);
            assertThat(result.kelly().recommendation()).contains("No bet");
        }

        @Test
        void kellyFractionsAreCorrectlyDerived() {
            ExpectedValueResponse result = tool.calculateExpectedValue(0.60, 100, null);
            assertThat(result.kelly().halfKelly()).isCloseTo(result.kelly().fullKelly() / 2.0, within(0.0001));
            assertThat(result.kelly().quarterKelly()).isCloseTo(result.kelly().fullKelly() / 4.0, within(0.0001));
        }

        @Test
        void underdogWithEdge() {
            // +200 implies 33.3%, predicted 45% -> positive EV
            ExpectedValueResponse result = tool.calculateExpectedValue(0.45, 200, null);
            assertThat(result.expectedValuePerUnit()).isGreaterThan(0.0);
            assertThat(result.kelly().fullKelly()).isGreaterThan(0.0);
        }

        @Test
        void responseFieldsPopulated() {
            ExpectedValueResponse result = tool.calculateExpectedValue(0.55, -110, 1000.0);
            assertThat(result.americanOdds()).isEqualTo(-110);
            assertThat(result.decimalOdds()).isCloseTo(1.9091, within(0.001));
            assertThat(result.predictedProbability()).isEqualTo(0.55);
            assertThat(result.predictedProbabilityPercent()).isEqualTo("55.00%");
            assertThat(result.impliedProbability()).isCloseTo(0.5238, within(0.001));
            assertThat(result.edgePercent()).isNotNull();
            assertThat(result.summary()).isNotBlank();
        }

        @Test
        void rejectsProbabilityAtZero() {
            assertThatIllegalArgumentException().isThrownBy(() -> tool.calculateExpectedValue(0.0, -110, null));
        }

        @Test
        void rejectsProbabilityAtOne() {
            assertThatIllegalArgumentException().isThrownBy(() -> tool.calculateExpectedValue(1.0, -110, null));
        }

        @Test
        void rejectsProbabilityAboveOne() {
            assertThatIllegalArgumentException().isThrownBy(() -> tool.calculateExpectedValue(1.5, -110, null));
        }

        @Test
        void rejectsProbabilityBelowZero() {
            assertThatIllegalArgumentException().isThrownBy(() -> tool.calculateExpectedValue(-0.1, -110, null));
        }

        @Test
        void rejectsInvalidOdds() {
            assertThatIllegalArgumentException().isThrownBy(() -> tool.calculateExpectedValue(0.55, 0, null));
        }

        @Test
        void summaryContainsKeyInfo() {
            ExpectedValueResponse result = tool.calculateExpectedValue(0.55, -110, null);
            assertThat(result.summary()).contains("EV:");
            assertThat(result.summary()).contains("Edge:");
        }
    }
}
