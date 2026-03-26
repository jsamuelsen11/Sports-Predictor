package com.sportspredictor.service;

import com.sportspredictor.entity.PredictionLog;
import com.sportspredictor.entity.enums.PredictionType;
import com.sportspredictor.repository.PredictionLogRepository;
import com.sportspredictor.util.OddsUtil;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/** Core prediction engine combining stats, trends, odds, injuries, and weather to generate predictions. */
@Service
@RequiredArgsConstructor
@Slf4j
public class PredictionService {

    private static final double PERCENT_MULTIPLIER = 100.0;
    private static final double STATS_WEIGHT = 0.35;
    private static final double TRENDS_WEIGHT = 0.25;
    private static final double ODDS_WEIGHT = 0.20;
    private static final double INJURIES_WEIGHT = 0.10;
    private static final double WEATHER_WEIGHT = 0.10;
    private static final double MAX_INJURY_IMPACT = 0.15;
    private static final double INJURY_SCALE = 0.03;
    private static final double HEAVY_WIND_THRESHOLD = 30.0;
    private static final double HEAVY_RAIN_THRESHOLD = 5.0;
    private static final double WEATHER_PENALTY = 0.05;

    private final StatsService statsService;
    private final TrendService trendService;
    private final OddsService oddsService;
    private final InjuryService injuryService;
    private final WeatherService weatherService;
    private final PredictionLogRepository predictionLogRepository;

    /** Full prediction result with all contributing factors. */
    public record PredictionResult(
            String predictionId,
            String eventId,
            String sport,
            String team1Id,
            String team2Id,
            String predictionType,
            String predictedOutcome,
            double confidence,
            double predictedProbability,
            List<String> keyFactors,
            MatchupSummary matchup,
            TrendSummary trends,
            InjurySummary injuries,
            WeatherSummary weather,
            OddsSummary odds,
            String summary) {}

    /** Matchup stats summary. */
    public record MatchupSummary(String team1, String team2, double team1Score, double team2Score, String advantage) {}

    /** Trend summary for both teams. */
    public record TrendSummary(String team1Trend, double team1WinRate, String team2Trend, double team2WinRate) {}

    /** Injury impact summary. */
    public record InjurySummary(int team1Injuries, int team2Injuries, List<String> keyAbsences) {}

    /** Weather conditions summary. */
    public record WeatherSummary(Double temperatureCelsius, Double windSpeedKmh, Double precipitationMm) {}

    /** Best available odds summary. */
    public record OddsSummary(String market, Double bestOdds, double impliedProbability) {}

    /** Generates a prediction for a game by combining multiple data sources. */
    public PredictionResult generatePrediction(
            String sport,
            String eventId,
            String team1Id,
            String team2Id,
            String predictionType,
            Double latitude,
            Double longitude,
            String gameDate) {

        try {
            List<String> keyFactors = new ArrayList<>();

            // 1. Stats comparison
            MatchupSummary matchup = evaluateMatchup(sport, team1Id, team2Id, keyFactors);

            // 2. Trend analysis
            TrendSummary trends = evaluateTrends(sport, team1Id, team2Id, keyFactors);

            // 3. Odds
            OddsSummary oddsSummary = evaluateOdds(sport, eventId, keyFactors);

            // 4. Injuries
            InjurySummary injuries = evaluateInjuries(sport, team1Id, team2Id, keyFactors);

            // 5. Weather
            WeatherSummary weather = evaluateWeather(latitude, longitude, gameDate, keyFactors);

            // 6. Compute weighted confidence
            double statsScore = matchup.team1Score() / (matchup.team1Score() + matchup.team2Score() + 0.001);
            double trendsScore = trends.team1WinRate();
            double oddsScore = oddsSummary.impliedProbability();
            double injuryModifier = computeInjuryModifier(injuries);
            double weatherModifier = computeWeatherModifier(weather);

            double rawConfidence = STATS_WEIGHT * statsScore
                    + TRENDS_WEIGHT * trendsScore
                    + ODDS_WEIGHT * oddsScore
                    + INJURIES_WEIGHT * (0.5 + injuryModifier)
                    + WEATHER_WEIGHT * (0.5 + weatherModifier);

            double confidence = Math.max(0.01, Math.min(0.99, rawConfidence));

            // 7. Determine predicted outcome
            String predictedOutcome = confidence >= 0.5 ? team1Id : team2Id;
            double predictedProbability = confidence >= 0.5 ? confidence : 1.0 - confidence;

            // 8. Log prediction
            String predictionId =
                    logPrediction(eventId, sport, predictionType, predictedOutcome, confidence, keyFactors);

            String summary = String.format(
                    Locale.ROOT,
                    "Prediction: %s wins (%s) with %.1f%% confidence. Key factors: %s.",
                    predictedOutcome,
                    predictionType,
                    predictedProbability * PERCENT_MULTIPLIER,
                    String.join(", ", keyFactors));

            return new PredictionResult(
                    predictionId,
                    eventId,
                    sport,
                    team1Id,
                    team2Id,
                    predictionType,
                    predictedOutcome,
                    confidence,
                    predictedProbability,
                    keyFactors,
                    matchup,
                    trends,
                    injuries,
                    weather,
                    oddsSummary,
                    summary);
        } catch (Exception e) {
            log.warn("Failed to generate prediction for event={}: {}", eventId, e.getMessage());
            return new PredictionResult(
                    null,
                    eventId,
                    sport,
                    team1Id,
                    team2Id,
                    predictionType,
                    "UNKNOWN",
                    0.0,
                    0.0,
                    List.of("Prediction failed: " + e.getMessage()),
                    null,
                    null,
                    null,
                    null,
                    null,
                    "Prediction could not be generated.");
        }
    }

    private MatchupSummary evaluateMatchup(String sport, String team1Id, String team2Id, List<String> factors) {
        StatsService.TeamStatsResult stats1 = statsService.getTeamStats(sport, team1Id, null);
        StatsService.TeamStatsResult stats2 = statsService.getTeamStats(sport, team2Id, null);

        double score1 = countStatValues(stats1);
        double score2 = countStatValues(stats2);

        String advantage;
        if (score1 > score2) {
            advantage = team1Id;
            factors.add(String.format(Locale.ROOT, "%s has statistical advantage", team1Id));
        } else if (score2 > score1) {
            advantage = team2Id;
            factors.add(String.format(Locale.ROOT, "%s has statistical advantage", team2Id));
        } else {
            advantage = "EVEN";
            factors.add("Statistical matchup is even");
        }

        return new MatchupSummary(team1Id, team2Id, score1, score2, advantage);
    }

    private TrendSummary evaluateTrends(String sport, String team1Id, String team2Id, List<String> factors) {
        TrendService.TrendResult trend1 = trendService.analyzeTrends(sport, team1Id, "last_10", null);
        TrendService.TrendResult trend2 = trendService.analyzeTrends(sport, team2Id, "last_10", null);

        if (trend1.record().winRate() > trend2.record().winRate()) {
            factors.add(String.format(
                    Locale.ROOT,
                    "%s trending better (%.0f%% vs %.0f%% win rate)",
                    team1Id,
                    trend1.record().winRate() * PERCENT_MULTIPLIER,
                    trend2.record().winRate() * PERCENT_MULTIPLIER));
        } else if (trend2.record().winRate() > trend1.record().winRate()) {
            factors.add(String.format(
                    Locale.ROOT,
                    "%s trending better (%.0f%% vs %.0f%% win rate)",
                    team2Id,
                    trend2.record().winRate() * PERCENT_MULTIPLIER,
                    trend1.record().winRate() * PERCENT_MULTIPLIER));
        }

        return new TrendSummary(
                trend1.record().trend(),
                trend1.record().winRate(),
                trend2.record().trend(),
                trend2.record().winRate());
    }

    private OddsSummary evaluateOdds(String sport, String eventId, List<String> factors) {
        OddsService.LiveOddsResult odds = oddsService.getLiveOdds(sport, eventId, null);

        if (odds.events().isEmpty()) {
            return new OddsSummary(null, null, 0.5);
        }

        OddsService.EventOdds event = odds.events().get(0);
        Double bestOdds = null;
        double impliedProb = 0.5;

        for (OddsService.BookmakerOdds bookmaker : event.bookmakers()) {
            for (OddsService.MarketOdds market : bookmaker.markets()) {
                for (OddsService.OutcomeOdds outcome : market.outcomes()) {
                    int price = (int) outcome.price();
                    if (bestOdds == null || price > bestOdds) {
                        bestOdds = (double) price;
                        impliedProb = OddsUtil.impliedProbability(price);
                    }
                }
            }
        }

        if (bestOdds != null) {
            factors.add(
                    String.format(Locale.ROOT, "Market implied probability: %.1f%%", impliedProb * PERCENT_MULTIPLIER));
        }

        return new OddsSummary("h2h", bestOdds, impliedProb);
    }

    private InjurySummary evaluateInjuries(String sport, String team1Id, String team2Id, List<String> factors) {
        InjuryService.InjuryReportResult inj1 = injuryService.getInjuryReport(sport, team1Id);
        InjuryService.InjuryReportResult inj2 = injuryService.getInjuryReport(sport, team2Id);

        List<String> keyAbsences = new ArrayList<>();
        for (InjuryService.TeamInjuryInfo teamInj : inj1.teamInjuries()) {
            for (InjuryService.InjuryInfo injury : teamInj.injuries()) {
                if ("Out".equalsIgnoreCase(injury.status())) {
                    keyAbsences.add(String.format(Locale.ROOT, "%s (%s) - Out", injury.athleteName(), team1Id));
                }
            }
        }
        for (InjuryService.TeamInjuryInfo teamInj : inj2.teamInjuries()) {
            for (InjuryService.InjuryInfo injury : teamInj.injuries()) {
                if ("Out".equalsIgnoreCase(injury.status())) {
                    keyAbsences.add(String.format(Locale.ROOT, "%s (%s) - Out", injury.athleteName(), team2Id));
                }
            }
        }

        if (!keyAbsences.isEmpty()) {
            factors.add(String.format(Locale.ROOT, "%d key injuries noted", keyAbsences.size()));
        }

        return new InjurySummary(inj1.totalInjuries(), inj2.totalInjuries(), keyAbsences);
    }

    private WeatherSummary evaluateWeather(Double latitude, Double longitude, String gameDate, List<String> factors) {
        if (latitude == null || longitude == null) {
            return new WeatherSummary(null, null, null);
        }

        WeatherService.WeatherResult weather = weatherService.getWeatherForecast(latitude, longitude, gameDate);

        if (weather.hourlyForecasts().isEmpty()) {
            return new WeatherSummary(null, null, null);
        }

        WeatherService.HourlyWeather hourly = weather.hourlyForecasts().get(0);

        if (hourly.windSpeedKmh() != null && hourly.windSpeedKmh() > HEAVY_WIND_THRESHOLD) {
            factors.add(String.format(Locale.ROOT, "Heavy wind: %.1f km/h", hourly.windSpeedKmh()));
        }
        if (hourly.precipitationMm() != null && hourly.precipitationMm() > HEAVY_RAIN_THRESHOLD) {
            factors.add(String.format(Locale.ROOT, "Heavy rain: %.1f mm", hourly.precipitationMm()));
        }

        return new WeatherSummary(hourly.temperatureCelsius(), hourly.windSpeedKmh(), hourly.precipitationMm());
    }

    private static double computeInjuryModifier(InjurySummary injuries) {
        if (injuries == null) {
            return 0.0;
        }
        int diff = injuries.team2Injuries() - injuries.team1Injuries();
        return Math.max(-MAX_INJURY_IMPACT, Math.min(MAX_INJURY_IMPACT, diff * INJURY_SCALE));
    }

    private static double computeWeatherModifier(WeatherSummary weather) {
        if (weather == null || weather.windSpeedKmh() == null) {
            return 0.0;
        }
        double penalty = 0.0;
        if (weather.windSpeedKmh() > HEAVY_WIND_THRESHOLD) {
            penalty -= WEATHER_PENALTY;
        }
        if (weather.precipitationMm() != null && weather.precipitationMm() > HEAVY_RAIN_THRESHOLD) {
            penalty -= WEATHER_PENALTY;
        }
        return penalty;
    }

    private static double countStatValues(StatsService.TeamStatsResult stats) {
        return stats.splits().stream()
                .flatMap(s -> s.categories().stream())
                .flatMap(c -> c.stats().stream())
                .mapToDouble(StatsService.StatEntry::value)
                .average()
                .orElse(0.0);
    }

    private String logPrediction(
            String eventId,
            String sport,
            String predictionType,
            String predictedOutcome,
            double confidence,
            List<String> keyFactors) {
        try {
            PredictionType type = PredictionType.valueOf(predictionType.toUpperCase(Locale.ROOT));
            PredictionLog predictionLog = PredictionLog.builder()
                    .id(UUID.randomUUID().toString())
                    .eventId(eventId)
                    .sport(sport)
                    .predictionType(type)
                    .predictedOutcome(predictedOutcome)
                    .confidence(confidence)
                    .keyFactors(String.join("; ", keyFactors))
                    .createdAt(Instant.now())
                    .build();
            predictionLogRepository.save(predictionLog);
            return predictionLog.getId();
        } catch (Exception e) {
            log.warn("Failed to log prediction for event={}: {}", eventId, e.getMessage());
            return null;
        }
    }
}
