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
        for (int i = 0; i < legOdds.size(); i++) {
            Integer leg = legOdds.get(i);
            Objects.requireNonNull(leg, "Parlay leg odds at index " + i + " must not be null");
            combinedDecimal *= OddsUtil.americanToDecimal(leg);
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

    /**
     * Calculates game prop bet profit. Uses the same formula as moneyline with variable odds.
     *
     * @param stake wager amount (must be positive)
     * @param americanOdds American odds for the game prop
     * @return profit amount
     */
    public static BigDecimal gamePropPayout(BigDecimal stake, int americanOdds) {
        return moneylinePayout(stake, americanOdds);
    }

    /**
     * Calculates first-half bet profit. Uses the same formula as moneyline.
     *
     * @param stake wager amount (must be positive)
     * @param americanOdds American odds for the first half line
     * @return profit amount
     */
    public static BigDecimal firstHalfPayout(BigDecimal stake, int americanOdds) {
        return moneylinePayout(stake, americanOdds);
    }

    /**
     * Calculates teaser bet profit. A teaser adjusts spreads/totals in the bettor's favor but at reduced
     * odds. The teaser odds depend on the number of legs and teaser points.
     *
     * <p>Standard teaser payouts (NFL/NBA):
     * <ul>
     *   <li>2-leg 6pt: -110</li>
     *   <li>2-leg 6.5pt: -120</li>
     *   <li>2-leg 7pt: -130</li>
     *   <li>3-leg 6pt: +150</li>
     *   <li>3-leg 6.5pt: +130</li>
     *   <li>3-leg 7pt: +110</li>
     * </ul>
     *
     * @param stake wager amount (must be positive)
     * @param legCount number of teaser legs (must be >= 2)
     * @param teaserPoints teaser adjustment in points (e.g., 6.0, 6.5, 7.0)
     * @return profit amount
     */
    public static BigDecimal teaserPayout(BigDecimal stake, int legCount, double teaserPoints) {
        validateStake(stake);
        if (legCount < MIN_PARLAY_LEGS) {
            throw new IllegalArgumentException(
                    "Teaser requires at least " + MIN_PARLAY_LEGS + " legs, got: " + legCount);
        }
        int teaserOdds = lookupTeaserOdds(legCount, teaserPoints);
        return moneylinePayout(stake, teaserOdds);
    }

    /**
     * Calculates round-robin total profit. A round-robin generates all possible parlay combinations
     * of a given size from a set of selections.
     *
     * <p>For example, 4 picks in 2-leg round robin produces C(4,2)=6 sub-parlays. The total stake
     * is split equally across all sub-parlays if all win.
     *
     * @param stakePerCombo wager amount per sub-parlay (must be positive)
     * @param legOdds list of American odds for each selection
     * @param parlaySize number of legs per sub-parlay (e.g., 2 for two-team RR)
     * @return list of profits for each sub-parlay, in combination order
     */
    public static List<BigDecimal> roundRobinPayouts(BigDecimal stakePerCombo, List<Integer> legOdds, int parlaySize) {
        validateStake(stakePerCombo);
        Objects.requireNonNull(legOdds, "Leg odds must not be null");
        if (parlaySize < MIN_PARLAY_LEGS) {
            throw new IllegalArgumentException(
                    "Round-robin parlay size must be at least " + MIN_PARLAY_LEGS + ", got: " + parlaySize);
        }
        if (legOdds.size() < parlaySize) {
            throw new IllegalArgumentException(String.format(
                    "Need at least %d legs for %d-leg round-robin, got: %d", parlaySize, parlaySize, legOdds.size()));
        }

        List<BigDecimal> payouts = new java.util.ArrayList<>();
        List<List<Integer>> combos = combinations(legOdds, parlaySize);
        for (List<Integer> combo : combos) {
            payouts.add(parlayPayout(stakePerCombo, combo));
        }
        return payouts;
    }

    /**
     * Returns the number of sub-parlays in a round-robin: C(n, k).
     *
     * @param totalSelections total number of picks
     * @param parlaySize legs per sub-parlay
     * @return number of combinations
     */
    public static int roundRobinComboCount(int totalSelections, int parlaySize) {
        if (totalSelections < parlaySize || parlaySize < MIN_PARLAY_LEGS) {
            return 0;
        }
        return binomial(totalSelections, parlaySize);
    }

    /**
     * Looks up standard teaser odds based on leg count and teaser points.
     *
     * <p>Table covers NFL/NBA standard teasers (6, 6.5, 7 points). For leg counts > 3 or
     * non-standard points, a formula-based approximation is used.
     */
    public static int lookupTeaserOdds(int legCount, double teaserPoints) {
        // Standard 2-leg teaser odds
        if (legCount == 2) {
            if (teaserPoints <= 6.0) {
                return STANDARD_JUICE; // -110
            }
            if (teaserPoints <= 6.5) {
                return -120;
            }
            return -130;
        }
        // Standard 3-leg teaser odds
        if (legCount == 3) {
            if (teaserPoints <= 6.0) {
                return 150;
            }
            if (teaserPoints <= 6.5) {
                return 130;
            }
            return 110;
        }
        // 4+ legs: approximate by multiplying the 2-leg decimal odds
        double baseTeaserDecimal = OddsUtil.americanToDecimal(lookupTeaserOdds(2, teaserPoints));
        double combined = Math.pow(baseTeaserDecimal, legCount - 1);
        return OddsUtil.decimalToAmerican(combined);
    }

    private static int binomial(int n, int k) {
        if (k > n - k) {
            return binomial(n, n - k);
        }
        long result = 1;
        for (int i = 0; i < k; i++) {
            result = result * (n - i) / (i + 1);
        }
        return (int) result;
    }

    private static <T> List<List<T>> combinations(List<T> items, int k) {
        List<List<T>> result = new java.util.ArrayList<>();
        combinationsHelper(items, k, 0, new java.util.ArrayList<>(), result);
        return result;
    }

    private static <T> void combinationsHelper(List<T> items, int k, int start, List<T> current, List<List<T>> result) {
        if (current.size() == k) {
            result.add(new java.util.ArrayList<>(current));
            return;
        }
        for (int i = start; i < items.size(); i++) {
            current.add(items.get(i));
            combinationsHelper(items, k, i + 1, current, result);
            current.remove(current.size() - 1);
        }
    }

    private static void validateStake(BigDecimal stake) {
        Objects.requireNonNull(stake, "Stake must not be null");
        if (stake.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Stake must be positive, got: " + stake);
        }
    }
}
