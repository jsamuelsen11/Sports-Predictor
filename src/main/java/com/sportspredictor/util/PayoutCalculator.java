package com.sportspredictor.util;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

/** Static utility methods for calculating bet payouts across all MVP bet types. */
public final class PayoutCalculator {

    /** Decimal scale for all payout results (cents precision). */
    public static final int PAYOUT_SCALE = 2;

    /** Standard American odds juice for spread and totals bets. */
    public static final int STANDARD_JUICE = -110;

    /** Minimum number of legs required for a parlay bet. */
    public static final int MIN_PARLAY_LEGS = 2;

    private PayoutCalculator() {
        throw new AssertionError("Utility class — do not instantiate");
    }

    /**
     * Calculates moneyline bet profit.
     *
     * @param stake wager amount (must be positive)
     * @param americanOdds American odds (e.g., -150, +150)
     * @return profit amount (e.g., $100 at +150 returns $150 profit)
     */
    public static BigDecimal moneylinePayout(BigDecimal stake, int americanOdds) {
        validateStake(stake);
        double decimalOdds = OddsUtil.americanToDecimal(americanOdds);
        return stake.multiply(BigDecimal.valueOf(decimalOdds - 1.0)).setScale(PAYOUT_SCALE, RoundingMode.HALF_UP);
    }

    /**
     * Calculates spread bet profit. Uses the same formula as moneyline.
     *
     * @param stake wager amount (must be positive)
     * @param americanOdds American odds (typically -110)
     * @return profit amount
     */
    public static BigDecimal spreadPayout(BigDecimal stake, int americanOdds) {
        return moneylinePayout(stake, americanOdds);
    }

    /**
     * Calculates totals (over/under) bet profit. Uses the same formula as moneyline.
     *
     * @param stake wager amount (must be positive)
     * @param americanOdds American odds (typically -110)
     * @return profit amount
     */
    public static BigDecimal totalsPayout(BigDecimal stake, int americanOdds) {
        return moneylinePayout(stake, americanOdds);
    }

    /**
     * Calculates player prop bet profit. Uses the same formula as moneyline with variable odds.
     *
     * @param stake wager amount (must be positive)
     * @param americanOdds American odds for the prop
     * @return profit amount
     */
    public static BigDecimal playerPropPayout(BigDecimal stake, int americanOdds) {
        return moneylinePayout(stake, americanOdds);
    }

    /**
     * Calculates parlay bet profit by multiplying decimal odds of all legs.
     *
     * @param stake wager amount (must be positive)
     * @param legOdds list of American odds for each leg (must have at least 2)
     * @return profit amount
     */
    public static BigDecimal parlayPayout(BigDecimal stake, List<Integer> legOdds) {
        validateStake(stake);
        Objects.requireNonNull(legOdds, "Parlay leg odds must not be null");
        if (legOdds.size() < MIN_PARLAY_LEGS) {
            throw new IllegalArgumentException(
                    "Parlay requires at least " + MIN_PARLAY_LEGS + " legs, got: " + legOdds.size());
        }

        double combinedDecimal = 1.0;
        for (int odds : legOdds) {
            combinedDecimal *= OddsUtil.americanToDecimal(odds);
        }

        return stake.multiply(BigDecimal.valueOf(combinedDecimal - 1.0)).setScale(PAYOUT_SCALE, RoundingMode.HALF_UP);
    }

    /**
     * Calculates total return (stake + profit) for a single-leg bet.
     *
     * @param stake wager amount (must be positive)
     * @param americanOdds American odds
     * @return total amount returned (stake + profit)
     */
    public static BigDecimal totalReturn(BigDecimal stake, int americanOdds) {
        validateStake(stake);
        double decimalOdds = OddsUtil.americanToDecimal(americanOdds);
        return stake.multiply(BigDecimal.valueOf(decimalOdds)).setScale(PAYOUT_SCALE, RoundingMode.HALF_UP);
    }

    /**
     * Settles a bet based on outcome.
     *
     * @param stake wager amount (must be positive)
     * @param americanOdds American odds the bet was placed at
     * @param outcome settlement result: "WON", "LOST", or "PUSHED"
     * @return payout — WON: profit, LOST: zero, PUSHED: original stake returned
     */
    public static BigDecimal settleBet(BigDecimal stake, int americanOdds, String outcome) {
        validateStake(stake);
        Objects.requireNonNull(outcome, "Outcome must not be null");
        return switch (outcome.toUpperCase(Locale.ROOT)) {
            case "WON" -> moneylinePayout(stake, americanOdds);
            case "LOST" -> BigDecimal.ZERO.setScale(PAYOUT_SCALE, RoundingMode.HALF_UP);
            case "PUSHED" -> stake.setScale(PAYOUT_SCALE, RoundingMode.HALF_UP);
            default ->
                throw new IllegalArgumentException("Invalid outcome: " + outcome + ". Must be WON, LOST, or PUSHED");
        };
    }

    private static void validateStake(BigDecimal stake) {
        Objects.requireNonNull(stake, "Stake must not be null");
        if (stake.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Stake must be positive, got: " + stake);
        }
    }
}
