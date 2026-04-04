package com.sportspredictor.mcpserver.util;

/** Static utility methods for converting between odds formats and calculating probabilities. */
public final class OddsUtil {

    /** Standard American even-money odds value. */
    public static final int EVEN_ODDS = 100;

    /** Minimum valid magnitude for American odds (must be >= 100 or <= -100). */
    public static final int MIN_AMERICAN_MAGNITUDE = 100;

    /** Minimum valid decimal odds (must be greater than 1.0). */
    public static final double MIN_DECIMAL_ODDS = 1.0;

    private static final double PERCENT = 100.0;

    private OddsUtil() {
        throw new AssertionError("Utility class — do not instantiate");
    }

    /**
     * Converts American odds to decimal odds.
     *
     * @param americanOdds American odds (e.g., +150, -110)
     * @return decimal odds (e.g., 2.5, 1.909)
     */
    public static double americanToDecimal(int americanOdds) {
        validateAmericanOdds(americanOdds);
        if (americanOdds > 0) {
            return 1.0 + (americanOdds / PERCENT);
        } else {
            return 1.0 + (PERCENT / Math.abs(americanOdds));
        }
    }

    /**
     * Converts decimal odds to American odds.
     *
     * @param decimalOdds decimal odds (must be greater than 1.0)
     * @return American odds (positive for underdogs, negative for favorites)
     */
    public static int decimalToAmerican(double decimalOdds) {
        validateDecimalOdds(decimalOdds);
        if (decimalOdds >= 2.0) {
            return (int) Math.round((decimalOdds - 1.0) * PERCENT);
        } else {
            return (int) Math.round(-PERCENT / (decimalOdds - 1.0));
        }
    }

    /**
     * Converts American odds to fractional odds string.
     *
     * @param americanOdds American odds (e.g., +150, -110)
     * @return reduced fractional odds (e.g., "3/2", "10/11")
     */
    public static String americanToFractional(int americanOdds) {
        validateAmericanOdds(americanOdds);
        int numerator;
        int denominator;
        if (americanOdds > 0) {
            numerator = americanOdds;
            denominator = (int) PERCENT;
        } else {
            numerator = (int) PERCENT;
            denominator = Math.abs(americanOdds);
        }
        int divisor = gcd(numerator, denominator);
        return (numerator / divisor) + "/" + (denominator / divisor);
    }

    /**
     * Calculates implied probability from American odds.
     *
     * @param americanOdds American odds (e.g., -110, +150)
     * @return implied probability between 0.0 and 1.0
     */
    public static double impliedProbability(int americanOdds) {
        validateAmericanOdds(americanOdds);
        if (americanOdds > 0) {
            return PERCENT / (americanOdds + PERCENT);
        } else {
            double absOdds = Math.abs((double) americanOdds);
            return absOdds / (absOdds + PERCENT);
        }
    }

    /**
     * Calculates vig (juice/overround) from two-way American odds.
     *
     * @param odds1 American odds for outcome 1
     * @param odds2 American odds for outcome 2
     * @return vig as a decimal (e.g., 0.0476 for ~4.76% juice)
     */
    public static double calculateVig(int odds1, int odds2) {
        validateAmericanOdds(odds1);
        validateAmericanOdds(odds2);
        double prob1 = impliedProbability(odds1);
        double prob2 = impliedProbability(odds2);
        return prob1 + prob2 - 1.0;
    }

    /**
     * Removes vig from two-way odds to get true probability for side 1.
     *
     * <p>Uses proportional scaling (multiplicative method) to distribute the overround.
     *
     * @param odds1 American odds for the side whose true probability is returned
     * @param odds2 American odds for the opposing side
     * @return true probability between 0.0 and 1.0
     */
    public static double trueProbability(int odds1, int odds2) {
        validateAmericanOdds(odds1);
        validateAmericanOdds(odds2);
        double prob1 = impliedProbability(odds1);
        double prob2 = impliedProbability(odds2);
        double totalImplied = prob1 + prob2;
        return prob1 / totalImplied;
    }

    private static void validateAmericanOdds(int americanOdds) {
        if (americanOdds > -MIN_AMERICAN_MAGNITUDE && americanOdds < MIN_AMERICAN_MAGNITUDE) {
            throw new IllegalArgumentException("American odds must be >= +" + MIN_AMERICAN_MAGNITUDE + " or <= -"
                    + MIN_AMERICAN_MAGNITUDE + ", got: " + americanOdds);
        }
    }

    private static void validateDecimalOdds(double decimalOdds) {
        if (decimalOdds <= MIN_DECIMAL_ODDS) {
            throw new IllegalArgumentException("Decimal odds must be > " + MIN_DECIMAL_ODDS + ", got: " + decimalOdds);
        }
    }

    private static int gcd(int a, int b) {
        int x = a;
        int y = b;
        while (y != 0) {
            int temp = y;
            y = x % y;
            x = temp;
        }
        return x;
    }
}
