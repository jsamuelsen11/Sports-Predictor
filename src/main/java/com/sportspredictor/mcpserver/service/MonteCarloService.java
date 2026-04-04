package com.sportspredictor.mcpserver.service;

import com.sportspredictor.mcpserver.service.StatsService.StatCategory;
import com.sportspredictor.mcpserver.service.StatsService.StatEntry;
import com.sportspredictor.mcpserver.service.StatsService.StatSplit;
import com.sportspredictor.mcpserver.service.StatsService.TeamStatsResult;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/** Monte Carlo simulation engine for game outcome modeling. */
@Service
@RequiredArgsConstructor
@Slf4j
public class MonteCarloService {

    private static final int DEFAULT_SIMULATIONS = 10_000;
    private static final int MAX_SIMULATIONS = 100_000;
    private static final double DEFAULT_MEAN_SCORE = 100.0;
    private static final double DEFAULT_STD_DEV = 12.0;

    private final StatsService statsService;

    /** Score distribution from simulations. */
    public record ScoreDistribution(double mean, double median, double stdDev, Map<Integer, Double> histogram) {}

    /** Spread distribution from simulations. */
    public record SpreadDistribution(double meanSpread, double p5, double p25, double p50, double p75, double p95) {}

    /** Total distribution from simulations. */
    public record TotalDistribution(
            double meanTotal, double overProbability, double underProbability, double pushLine) {}

    /** 95% confidence interval. */
    public record ConfidenceInterval(double lower95, double upper95) {}

    /** Full simulation result. */
    public record SimulationOutput(
            String sport,
            String team1Id,
            String team2Id,
            int numSimulations,
            ScoreDistribution team1Scores,
            ScoreDistribution team2Scores,
            double team1WinProbability,
            double team2WinProbability,
            double drawProbability,
            SpreadDistribution spreadDistribution,
            TotalDistribution totalDistribution,
            ConfidenceInterval confidenceInterval,
            String summary) {}

    /** Runs a Monte Carlo simulation for a game between two teams. */
    public SimulationOutput simulateGame(String sport, String team1Id, String team2Id, Integer numSimulations) {
        int sims = resolveSimulationCount(numSimulations);

        double[] team1Params = getTeamScoreParams(sport, team1Id);
        double[] team2Params = getTeamScoreParams(sport, team2Id);

        double[] team1ScoresArr = new double[sims];
        double[] team2ScoresArr = new double[sims];
        double[] spreads = new double[sims];
        double[] totals = new double[sims];
        int team1Wins = 0;
        int team2Wins = 0;
        int draws = 0;

        ThreadLocalRandom rng = ThreadLocalRandom.current();

        for (int i = 0; i < sims; i++) {
            double s1 = Math.max(0, rng.nextGaussian() * team1Params[1] + team1Params[0]);
            double s2 = Math.max(0, rng.nextGaussian() * team2Params[1] + team2Params[0]);
            team1ScoresArr[i] = s1;
            team2ScoresArr[i] = s2;
            spreads[i] = s1 - s2;
            totals[i] = s1 + s2;

            if (s1 > s2) {
                team1Wins++;
            } else if (s2 > s1) {
                team2Wins++;
            } else {
                draws++;
            }
        }

        java.util.Arrays.sort(team1ScoresArr);
        java.util.Arrays.sort(team2ScoresArr);
        java.util.Arrays.sort(spreads);
        java.util.Arrays.sort(totals);

        ScoreDistribution team1Dist = buildScoreDistribution(team1ScoresArr);
        ScoreDistribution team2Dist = buildScoreDistribution(team2ScoresArr);

        double team1WinProb = (double) team1Wins / sims;
        double team2WinProb = (double) team2Wins / sims;
        double drawProb = (double) draws / sims;

        SpreadDistribution spreadDist = new SpreadDistribution(
                mean(spreads),
                percentile(spreads, 5),
                percentile(spreads, 25),
                percentile(spreads, 50),
                percentile(spreads, 75),
                percentile(spreads, 95));

        double meanTotal = mean(totals);
        double overCount = 0;
        double pushLine = Math.round(meanTotal * 2.0) / 2.0;
        for (double t : totals) {
            if (t > pushLine) {
                overCount++;
            }
        }
        TotalDistribution totalDist =
                new TotalDistribution(meanTotal, overCount / sims, 1.0 - overCount / sims, pushLine);

        ConfidenceInterval ci = new ConfidenceInterval(percentile(spreads, 2.5), percentile(spreads, 97.5));

        String summary = String.format(
                Locale.ROOT,
                "Simulated %d games: %s wins %.1f%%, %s wins %.1f%%, draw %.1f%%. "
                        + "Mean spread: %.1f, Mean total: %.1f",
                sims,
                team1Id,
                team1WinProb * 100,
                team2Id,
                team2WinProb * 100,
                drawProb * 100,
                mean(spreads),
                meanTotal);

        log.info("Monte Carlo simulation completed: {} sims for {} vs {}", sims, team1Id, team2Id);

        return new SimulationOutput(
                sport,
                team1Id,
                team2Id,
                sims,
                team1Dist,
                team2Dist,
                team1WinProb,
                team2WinProb,
                drawProb,
                spreadDist,
                totalDist,
                ci,
                summary);
    }

    private double[] getTeamScoreParams(String sport, String teamId) {
        try {
            TeamStatsResult stats = statsService.getTeamStats(sport, teamId, null);
            double meanScore = extractScoringMean(stats);
            double stdDev = estimateStdDev(meanScore, sport);
            return new double[] {meanScore, stdDev};
        } catch (Exception e) {
            log.warn("Failed to get stats for {}/{}, using defaults: {}", sport, teamId, e.getMessage());
            return new double[] {DEFAULT_MEAN_SCORE, DEFAULT_STD_DEV};
        }
    }

    private double extractScoringMean(TeamStatsResult stats) {
        for (StatSplit split : stats.splits()) {
            for (StatCategory cat : split.categories()) {
                for (StatEntry entry : cat.stats()) {
                    String name = entry.name().toLowerCase(Locale.ROOT);
                    if (name.contains("pointspergame")
                            || name.contains("points per game")
                            || name.contains("runspergame")
                            || name.contains("goalspergame")) {
                        return entry.value();
                    }
                }
            }
        }
        return DEFAULT_MEAN_SCORE;
    }

    private double estimateStdDev(double meanScore, String sport) {
        String sportLower = sport.toLowerCase(Locale.ROOT);
        return switch (sportLower) {
            case "nfl" -> meanScore * 0.45;
            case "nba" -> meanScore * 0.11;
            case "mlb" -> meanScore * 0.55;
            case "nhl" -> meanScore * 0.40;
            default -> meanScore * 0.12;
        };
    }

    private static ScoreDistribution buildScoreDistribution(double[] sorted) {
        double mean = mean(sorted);
        double median = percentile(sorted, 50);
        double stdDev = stdDev(sorted, mean);

        Map<Integer, Double> histogram = new HashMap<>();
        for (double v : sorted) {
            int bucket = (int) Math.round(v);
            histogram.merge(bucket, 1.0 / sorted.length, Double::sum);
        }

        return new ScoreDistribution(mean, median, stdDev, histogram);
    }

    private static double mean(double[] arr) {
        double sum = 0;
        for (double v : arr) {
            sum += v;
        }
        return sum / arr.length;
    }

    private static double stdDev(double[] arr, double mean) {
        double sumSq = 0;
        for (double v : arr) {
            sumSq += (v - mean) * (v - mean);
        }
        return Math.sqrt(sumSq / arr.length);
    }

    private static double percentile(double[] sorted, double p) {
        double index = (p / 100.0) * (sorted.length - 1);
        int lower = (int) Math.floor(index);
        int upper = Math.min(lower + 1, sorted.length - 1);
        double fraction = index - lower;
        return sorted[lower] + fraction * (sorted[upper] - sorted[lower]);
    }

    private static int resolveSimulationCount(Integer requested) {
        if (requested == null || requested <= 0) {
            return DEFAULT_SIMULATIONS;
        }
        return Math.min(requested, MAX_SIMULATIONS);
    }
}
