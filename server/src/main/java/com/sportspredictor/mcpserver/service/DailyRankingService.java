package com.sportspredictor.mcpserver.service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicInteger;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

/** Ranks today's games by expected value using predictions and odds comparison. */
@Service
@RequiredArgsConstructor
@Slf4j
public class DailyRankingService {

    private static final double PERCENT_MULTIPLIER = 100.0;
    private static final double DEFAULT_MIN_CONFIDENCE = 0.0;
    private static final double DEFAULT_MIN_EDGE = 0.0;
    private static final double STRONG_CONFIDENCE = 0.70;
    private static final double GOOD_CONFIDENCE = 0.55;

    private final ScheduleService scheduleService;
    private final PredictionService predictionService;
    private final OddsComparisonService oddsComparisonService;

    /** A ranked play with prediction details and EV. */
    public record RankedPlay(
            int rank,
            String eventId,
            String matchup,
            String predictedOutcome,
            double confidence,
            double expectedValue,
            String bestBookmaker,
            int bestOdds,
            String predictionType,
            String rating) {}

    /** Daily card result with all ranked plays. */
    public record DailyCardResult(
            String sport,
            String date,
            List<RankedPlay> rankedPlays,
            int totalGames,
            int qualifiedPlays,
            String summary) {}

    /** Scans today's games, generates predictions, and ranks by expected value. */
    @Cacheable(value = "schedules", key = "'daily-rank-' + #sport + '-' + #minConfidence + '-' + #minEdge")
    public DailyCardResult rankTodaysPlays(String sport, Double minConfidence, Double minEdge) {
        double effectiveMinConf = (minConfidence != null) ? minConfidence : DEFAULT_MIN_CONFIDENCE;
        double effectiveMinEdge = (minEdge != null) ? minEdge : DEFAULT_MIN_EDGE;

        try {
            ScheduleService.ScheduleResult schedule = scheduleService.getTodaySchedule(sport);
            List<RankedPlay> plays = new ArrayList<>();

            for (ScheduleService.GameInfo game : schedule.games()) {
                if (game.completed() || game.teams().size() < 2) {
                    continue;
                }

                String team1Id = game.teams().get(0).id();
                String team2Id = game.teams().get(1).id();

                PredictionService.PredictionResult prediction = predictionService.generatePrediction(
                        sport, game.id(), team1Id, team2Id, "MONEYLINE", null, null, null);

                if (prediction.confidence() < effectiveMinConf) {
                    continue;
                }

                OddsComparisonService.OddsComparison odds = oddsComparisonService.compareOdds(sport, game.id(), "h2h");

                int bestOdds = 0;
                String bestBookmaker = "N/A";
                if (odds.bestHome() != null && prediction.predictedOutcome().equals(team1Id)) {
                    bestOdds = odds.bestHome().price();
                    bestBookmaker = odds.bestHome().bookmaker();
                } else if (odds.bestAway() != null) {
                    bestOdds = odds.bestAway().price();
                    bestBookmaker = odds.bestAway().bookmaker();
                }

                double ev = prediction.predictedProbability()
                        - (bestOdds != 0
                                ? com.sportspredictor.mcpserver.util.OddsUtil.impliedProbability(bestOdds)
                                : 0.5);

                if (ev * PERCENT_MULTIPLIER < effectiveMinEdge) {
                    continue;
                }

                String rating = ratePlay(prediction.confidence());
                String matchupName = game.shortName() != null ? game.shortName() : game.name();

                plays.add(new RankedPlay(
                        0,
                        game.id(),
                        matchupName,
                        prediction.predictedOutcome(),
                        prediction.confidence(),
                        ev,
                        bestBookmaker,
                        bestOdds,
                        prediction.predictionType(),
                        rating));
            }

            plays.sort(Comparator.comparingDouble(RankedPlay::expectedValue).reversed());

            AtomicInteger rank = new AtomicInteger(1);
            List<RankedPlay> rankedPlays = plays.stream()
                    .map(p -> new RankedPlay(
                            rank.getAndIncrement(),
                            p.eventId(),
                            p.matchup(),
                            p.predictedOutcome(),
                            p.confidence(),
                            p.expectedValue(),
                            p.bestBookmaker(),
                            p.bestOdds(),
                            p.predictionType(),
                            p.rating()))
                    .toList();

            String summary = String.format(
                    Locale.ROOT,
                    "Today's %s card: %d qualified plays from %d games. Min confidence: %.0f%%, min edge: %.1f%%.",
                    sport.toUpperCase(Locale.ROOT),
                    rankedPlays.size(),
                    schedule.gameCount(),
                    effectiveMinConf * PERCENT_MULTIPLIER,
                    effectiveMinEdge);

            return new DailyCardResult(
                    sport, schedule.dateRange(), rankedPlays, schedule.gameCount(), rankedPlays.size(), summary);
        } catch (Exception e) {
            log.warn("Failed to rank today's plays for sport={}: {}", sport, e.getMessage());
            return new DailyCardResult(sport, null, List.of(), 0, 0, "Failed to generate daily card.");
        }
    }

    private static String ratePlay(double confidence) {
        if (confidence >= STRONG_CONFIDENCE) {
            return "STRONG";
        } else if (confidence >= GOOD_CONFIDENCE) {
            return "GOOD";
        } else {
            return "LEAN";
        }
    }
}
