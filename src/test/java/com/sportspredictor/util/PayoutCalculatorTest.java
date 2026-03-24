package com.sportspredictor.util;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.assertThatNullPointerException;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

/** Comprehensive tests for {@link PayoutCalculator}. */
class PayoutCalculatorTest {

    private static final BigDecimal HUNDRED = new BigDecimal("100.00");

    /** Tests for {@link PayoutCalculator#moneylinePayout(BigDecimal, int)}. */
    @Nested
    class MoneylinePayout {

        @ParameterizedTest
        @CsvSource({
            "100.00, -150, 66.67",
            "100.00, 150, 150.00",
            "100.00, 100, 100.00",
            "100.00, -100, 100.00",
            "100.00, -110, 90.91",
            "100.00, 200, 200.00",
            "100.00, -200, 50.00",
            "50.00, -110, 45.45",
            "100.00, 500, 500.00",
            "100.00, -500, 20.00",
            "100.00, 10000, 10000.00",
            "100.00, -10000, 1.00",
        })
        void calculatesCorrectly(BigDecimal stake, int odds, BigDecimal expected) {
            assertThat(PayoutCalculator.moneylinePayout(stake, odds)).isEqualByComparingTo(expected);
        }

        @Test
        void handlesSmallStakeRounding() {
            BigDecimal result = PayoutCalculator.moneylinePayout(new BigDecimal("0.01"), 150);
            assertThat(result).isEqualByComparingTo(new BigDecimal("0.02"));
        }
    }

    /** Tests for {@link PayoutCalculator#parlayPayout(BigDecimal, List)}. */
    @Nested
    class ParlayPayout {

        @Test
        void twoLegEqualOdds() {
            // -110, -110 -> 1.9091 * 1.9091 = 3.6446 -> profit = 264.46
            BigDecimal result = PayoutCalculator.parlayPayout(HUNDRED, List.of(-110, -110));
            assertThat(result).isEqualByComparingTo(new BigDecimal("264.46"));
        }

        @Test
        void threeLegMixedOdds() {
            // -110 (1.9091), +150 (2.5), -200 (1.5)
            // combined = 1.9091 * 2.5 * 1.5 = 7.1591 -> profit = 615.91
            BigDecimal result = PayoutCalculator.parlayPayout(HUNDRED, List.of(-110, 150, -200));
            assertThat(result).isEqualByComparingTo(new BigDecimal("615.91"));
        }

        @Test
        void allFavorites() {
            BigDecimal result = PayoutCalculator.parlayPayout(HUNDRED, List.of(-200, -300));
            // 1.5 * 1.3333 = 2.0 -> profit = 100.00
            assertThat(result).isEqualByComparingTo(new BigDecimal("100.00"));
        }

        @Test
        void allUnderdogs() {
            BigDecimal result = PayoutCalculator.parlayPayout(HUNDRED, List.of(200, 300));
            // 3.0 * 4.0 = 12.0 -> profit = 1100.00
            assertThat(result).isEqualByComparingTo(new BigDecimal("1100.00"));
        }

        @Test
        void rejectsSingleLeg() {
            assertThatIllegalArgumentException()
                    .isThrownBy(() -> PayoutCalculator.parlayPayout(HUNDRED, List.of(-110)));
        }

        @Test
        void rejectsEmptyList() {
            assertThatIllegalArgumentException()
                    .isThrownBy(() -> PayoutCalculator.parlayPayout(HUNDRED, Collections.emptyList()));
        }

        @Test
        void rejectsNullList() {
            assertThatNullPointerException().isThrownBy(() -> PayoutCalculator.parlayPayout(HUNDRED, null));
        }
    }

    /** Tests for {@link PayoutCalculator#settleBet(BigDecimal, int, String)}. */
    @Nested
    class SettleBet {

        @Test
        void wonReturnsProfit() {
            BigDecimal result = PayoutCalculator.settleBet(HUNDRED, 150, "WON");
            assertThat(result).isEqualByComparingTo(new BigDecimal("150.00"));
        }

        @Test
        void lostReturnsZero() {
            BigDecimal result = PayoutCalculator.settleBet(HUNDRED, 150, "LOST");
            assertThat(result).isEqualByComparingTo(BigDecimal.ZERO);
        }

        @Test
        void pushedReturnsStake() {
            BigDecimal result = PayoutCalculator.settleBet(HUNDRED, 150, "PUSHED");
            assertThat(result).isEqualByComparingTo(HUNDRED);
        }

        @Test
        void outcomeIsCaseInsensitive() {
            assertThat(PayoutCalculator.settleBet(HUNDRED, 150, "won")).isEqualByComparingTo(new BigDecimal("150.00"));
            assertThat(PayoutCalculator.settleBet(HUNDRED, 150, "lost")).isEqualByComparingTo(BigDecimal.ZERO);
            assertThat(PayoutCalculator.settleBet(HUNDRED, 150, "pushed")).isEqualByComparingTo(HUNDRED);
        }

        @Test
        void rejectsInvalidOutcome() {
            assertThatIllegalArgumentException()
                    .isThrownBy(() -> PayoutCalculator.settleBet(HUNDRED, 150, "CANCELLED"));
        }

        @Test
        void rejectsNullOutcome() {
            assertThatNullPointerException().isThrownBy(() -> PayoutCalculator.settleBet(HUNDRED, 150, null));
        }
    }

    /** Tests for {@link PayoutCalculator#totalReturn(BigDecimal, int)}. */
    @Nested
    class TotalReturn {

        @ParameterizedTest
        @CsvSource({
            "100.00, -150, 166.67",
            "100.00, 150, 250.00",
            "100.00, -110, 190.91",
            "100.00, 100, 200.00",
            "100.00, -100, 200.00",
        })
        void calculatesCorrectly(BigDecimal stake, int odds, BigDecimal expected) {
            assertThat(PayoutCalculator.totalReturn(stake, odds)).isEqualByComparingTo(expected);
        }
    }

    /** Tests verifying spread, totals, and player prop delegate to moneyline. */
    @Nested
    class DelegatingMethods {

        @Test
        void spreadMatchesMoneyline() {
            assertThat(PayoutCalculator.spreadPayout(HUNDRED, -110))
                    .isEqualByComparingTo(PayoutCalculator.moneylinePayout(HUNDRED, -110));
        }

        @Test
        void totalsMatchesMoneyline() {
            assertThat(PayoutCalculator.totalsPayout(HUNDRED, -110))
                    .isEqualByComparingTo(PayoutCalculator.moneylinePayout(HUNDRED, -110));
        }

        @Test
        void playerPropMatchesMoneyline() {
            assertThat(PayoutCalculator.playerPropPayout(HUNDRED, 150))
                    .isEqualByComparingTo(PayoutCalculator.moneylinePayout(HUNDRED, 150));
        }
    }

    /** Tests for input validation. */
    @Nested
    class Validation {

        @Test
        void rejectsNullStake() {
            assertThatNullPointerException().isThrownBy(() -> PayoutCalculator.moneylinePayout(null, -110));
        }

        @Test
        void rejectsZeroStake() {
            assertThatIllegalArgumentException()
                    .isThrownBy(() -> PayoutCalculator.moneylinePayout(BigDecimal.ZERO, -110));
        }

        @Test
        void rejectsNegativeStake() {
            assertThatIllegalArgumentException()
                    .isThrownBy(() -> PayoutCalculator.moneylinePayout(new BigDecimal("-10.00"), -110));
        }

        @ParameterizedTest
        @ValueSource(ints = {0, 50, -50, 99, -99})
        void rejectsInvalidOdds(int invalid) {
            assertThatIllegalArgumentException().isThrownBy(() -> PayoutCalculator.moneylinePayout(HUNDRED, invalid));
        }
    }
}
