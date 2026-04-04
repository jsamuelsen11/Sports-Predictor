package com.sportspredictor.mcpserver.util;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.within;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

/** Comprehensive tests for {@link OddsUtil}. */
class OddsUtilTest {

    /** Tests for {@link OddsUtil#americanToDecimal(int)}. */
    @Nested
    class AmericanToDecimal {

        @ParameterizedTest
        @CsvSource({
            "150, 2.5",
            "-110, 1.9090909090909092",
            "100, 2.0",
            "-100, 2.0",
            "200, 3.0",
            "-200, 1.5",
            "500, 6.0",
            "-500, 1.2",
            "10000, 101.0",
            "-10000, 1.01"
        })
        void convertsCorrectly(int american, double expectedDecimal) {
            assertThat(OddsUtil.americanToDecimal(american)).isCloseTo(expectedDecimal, within(0.0001));
        }
    }

    /** Tests for {@link OddsUtil#decimalToAmerican(double)}. */
    @Nested
    class DecimalToAmerican {

        @ParameterizedTest
        @CsvSource({"2.5, 150", "1.5, -200", "2.0, 100", "3.0, 200", "1.2, -500", "6.0, 500", "101.0, 10000"})
        void convertsCorrectly(double decimal, int expectedAmerican) {
            assertThat(OddsUtil.decimalToAmerican(decimal)).isEqualTo(expectedAmerican);
        }

        @Test
        void rejectsOddsAtOne() {
            assertThatIllegalArgumentException().isThrownBy(() -> OddsUtil.decimalToAmerican(1.0));
        }

        @Test
        void rejectsOddsBelowOne() {
            assertThatIllegalArgumentException().isThrownBy(() -> OddsUtil.decimalToAmerican(0.5));
        }
    }

    /** Tests for {@link OddsUtil#americanToFractional(int)}. */
    @Nested
    class AmericanToFractional {

        @ParameterizedTest
        @CsvSource({
            "150, 3/2",
            "-110, 10/11",
            "100, 1/1",
            "-100, 1/1",
            "200, 2/1",
            "-200, 1/2",
            "250, 5/2",
            "-150, 2/3",
            "500, 5/1",
            "-500, 1/5"
        })
        void convertsCorrectly(int american, String expectedFractional) {
            assertThat(OddsUtil.americanToFractional(american)).isEqualTo(expectedFractional);
        }
    }

    /** Tests for {@link OddsUtil#impliedProbability(int)}. */
    @Nested
    class ImpliedProbability {

        @ParameterizedTest
        @CsvSource({
            "-110, 0.5238095238",
            "110, 0.4761904762",
            "100, 0.5",
            "-100, 0.5",
            "-200, 0.6666666667",
            "200, 0.3333333333",
            "-300, 0.75",
            "300, 0.25"
        })
        void calculatesCorrectly(int american, double expectedProb) {
            assertThat(OddsUtil.impliedProbability(american)).isCloseTo(expectedProb, within(0.0001));
        }

        @Test
        void resultIsBetweenZeroAndOne() {
            assertThat(OddsUtil.impliedProbability(-10000)).isGreaterThan(0.0).isLessThan(1.0);
            assertThat(OddsUtil.impliedProbability(10000)).isGreaterThan(0.0).isLessThan(1.0);
        }
    }

    /** Tests for {@link OddsUtil#calculateVig(int, int)}. */
    @Nested
    class CalculateVig {

        @Test
        void standardJuice() {
            double vig = OddsUtil.calculateVig(-110, -110);
            assertThat(vig).isCloseTo(0.0476, within(0.001));
        }

        @Test
        void noVigOnEvenOdds() {
            double vig = OddsUtil.calculateVig(100, -100);
            assertThat(vig).isCloseTo(0.0, within(0.0001));
        }

        @Test
        void vigIsNonNegative() {
            double vig = OddsUtil.calculateVig(-150, 130);
            assertThat(vig).isGreaterThanOrEqualTo(0.0);
        }
    }

    /** Tests for {@link OddsUtil#trueProbability(int, int)}. */
    @Nested
    class TrueProbability {

        @Test
        void removesVigFromEqualOdds() {
            double trueProb = OddsUtil.trueProbability(-110, -110);
            assertThat(trueProb).isCloseTo(0.5, within(0.0001));
        }

        @Test
        void removesVigFromSkewedOdds() {
            double trueProb = OddsUtil.trueProbability(-150, 130);
            assertThat(trueProb).isGreaterThan(0.5);
            assertThat(trueProb).isLessThan(OddsUtil.impliedProbability(-150));
        }

        @Test
        void complementarySidesAddToOne() {
            double prob1 = OddsUtil.trueProbability(-150, 130);
            double prob2 = OddsUtil.trueProbability(130, -150);
            assertThat(prob1 + prob2).isCloseTo(1.0, within(0.0001));
        }
    }

    /** Tests for validation of invalid inputs. */
    @Nested
    class Validation {

        @ParameterizedTest
        @ValueSource(ints = {0, 50, -50, 99, -99, 1, -1})
        void rejectsInvalidAmericanOdds(int invalid) {
            assertThatIllegalArgumentException().isThrownBy(() -> OddsUtil.americanToDecimal(invalid));
            assertThatIllegalArgumentException().isThrownBy(() -> OddsUtil.americanToFractional(invalid));
            assertThatIllegalArgumentException().isThrownBy(() -> OddsUtil.impliedProbability(invalid));
        }

        @Test
        void rejectsInvalidAmericanOddsInVig() {
            assertThatIllegalArgumentException().isThrownBy(() -> OddsUtil.calculateVig(0, -110));
            assertThatIllegalArgumentException().isThrownBy(() -> OddsUtil.calculateVig(-110, 0));
        }

        @Test
        void rejectsInvalidAmericanOddsInTrueProbability() {
            assertThatIllegalArgumentException().isThrownBy(() -> OddsUtil.trueProbability(50, -110));
            assertThatIllegalArgumentException().isThrownBy(() -> OddsUtil.trueProbability(-110, 50));
        }
    }
}
