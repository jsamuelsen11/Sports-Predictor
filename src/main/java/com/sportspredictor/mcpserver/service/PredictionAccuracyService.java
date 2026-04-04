package com.sportspredictor.mcpserver.service;

import com.sportspredictor.mcpserver.entity.PredictionLog;
import com.sportspredictor.mcpserver.entity.enums.PredictionType;
import com.sportspredictor.mcpserver.repository.PredictionLogRepository;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/** Analyzes prediction track record from settled predictions in the prediction log. */
@Service
@RequiredArgsConstructor
@Slf4j
public class PredictionAccuracyService {

    private static final double PERCENT_MULTIPLIER = 100.0;
    private static final double BUCKET_50 = 0.50;
    private static final double BUCKET_60 = 0.60;
    private static final double BUCKET_70 = 0.70;
    private static final double BUCKET_80 = 0.80;
    private static final double BUCKET_90 = 0.90;

    private final PredictionLogRepository predictionLogRepository;

    /** Overall accuracy result with breakdowns. */
    public record AccuracyResult(
            int totalPredictions,
            int wins,
            int losses,
            double winRate,
            List<ConfidenceBucket> confidenceBuckets,
            List<SportBreakdown> sportBreakdowns,
            List<TypeBreakdown> typeBreakdowns,
            String summary) {}

    /** Accuracy within a confidence range. */
    public record ConfidenceBucket(String range, int total, int wins, double winRate) {}

    /** Accuracy breakdown by sport. */
    public record SportBreakdown(String sport, int total, int wins, double winRate) {}

    /** Accuracy breakdown by prediction type. */
    public record TypeBreakdown(String predictionType, int total, int wins, double winRate) {}

    /** Computes accuracy metrics for settled predictions with optional filtering. */
    public AccuracyResult getAccuracy(String sport, String predictionType, String dateRange, Double minConfidence) {
        try {
            List<PredictionLog> predictions = fetchSettledPredictions(sport);

            if (predictionType != null && !predictionType.isBlank()) {
                PredictionType type = PredictionType.valueOf(predictionType.toUpperCase(Locale.ROOT));
                predictions = predictions.stream()
                        .filter(p -> p.getPredictionType() == type)
                        .toList();
            }

            Instant rangeStart = parseDateRangeStart(dateRange);
            if (!rangeStart.equals(Instant.EPOCH)) {
                predictions = predictions.stream()
                        .filter(p -> p.getCreatedAt().isAfter(rangeStart))
                        .toList();
            }

            if (minConfidence != null) {
                predictions = predictions.stream()
                        .filter(p -> p.getConfidence() >= minConfidence)
                        .toList();
            }

            int total = predictions.size();
            int wins = (int) predictions.stream().filter(this::isCorrect).count();
            int losses = total - wins;
            double winRate = total > 0 ? (double) wins / total : 0.0;

            List<ConfidenceBucket> buckets = buildConfidenceBuckets(predictions);
            List<SportBreakdown> sportBreakdowns = buildSportBreakdowns(predictions);
            List<TypeBreakdown> typeBreakdowns = buildTypeBreakdowns(predictions);

            String summary = String.format(
                    Locale.ROOT,
                    "Prediction accuracy: %d/%d (%.1f%%) over %d total predictions."
                            + " Confidence buckets: %d ranges. Sports: %d. Types: %d.",
                    wins,
                    total,
                    winRate * PERCENT_MULTIPLIER,
                    total,
                    buckets.size(),
                    sportBreakdowns.size(),
                    typeBreakdowns.size());

            return new AccuracyResult(total, wins, losses, winRate, buckets, sportBreakdowns, typeBreakdowns, summary);
        } catch (Exception e) {
            log.warn("Failed to compute prediction accuracy: {}", e.getMessage());
            return new AccuracyResult(0, 0, 0, 0.0, List.of(), List.of(), List.of(), "No prediction data available.");
        }
    }

    private List<PredictionLog> fetchSettledPredictions(String sport) {
        if (sport != null && !sport.isBlank()) {
            return predictionLogRepository.findBySportAndActualOutcomeIsNotNull(sport);
        }
        return predictionLogRepository.findByActualOutcomeIsNotNull();
    }

    private boolean isCorrect(PredictionLog prediction) {
        return prediction.getPredictedOutcome().equalsIgnoreCase(prediction.getActualOutcome());
    }

    private List<ConfidenceBucket> buildConfidenceBuckets(List<PredictionLog> predictions) {
        double[][] ranges = {
            {0.0, BUCKET_50},
            {BUCKET_50, BUCKET_60},
            {BUCKET_60, BUCKET_70},
            {BUCKET_70, BUCKET_80},
            {BUCKET_80, BUCKET_90},
            {BUCKET_90, 1.01}
        };
        String[] labels = {"0-50%", "50-60%", "60-70%", "70-80%", "80-90%", "90-100%"};

        return java.util.stream.IntStream.range(0, ranges.length)
                .mapToObj(i -> {
                    double low = ranges[i][0];
                    double high = ranges[i][1];
                    List<PredictionLog> inBucket = predictions.stream()
                            .filter(p -> p.getConfidence() >= low && p.getConfidence() < high)
                            .toList();
                    int total = inBucket.size();
                    int wins = (int) inBucket.stream().filter(this::isCorrect).count();
                    double winRate = total > 0 ? (double) wins / total : 0.0;
                    return new ConfidenceBucket(labels[i], total, wins, winRate);
                })
                .filter(b -> b.total() > 0)
                .toList();
    }

    private List<SportBreakdown> buildSportBreakdowns(List<PredictionLog> predictions) {
        Map<String, List<PredictionLog>> bySport =
                predictions.stream().collect(Collectors.groupingBy(PredictionLog::getSport));

        return bySport.entrySet().stream()
                .map(entry -> {
                    int total = entry.getValue().size();
                    int wins = (int)
                            entry.getValue().stream().filter(this::isCorrect).count();
                    double winRate = total > 0 ? (double) wins / total : 0.0;
                    return new SportBreakdown(entry.getKey(), total, wins, winRate);
                })
                .toList();
    }

    private List<TypeBreakdown> buildTypeBreakdowns(List<PredictionLog> predictions) {
        Map<PredictionType, List<PredictionLog>> byType =
                predictions.stream().collect(Collectors.groupingBy(PredictionLog::getPredictionType));

        return byType.entrySet().stream()
                .map(entry -> {
                    int total = entry.getValue().size();
                    int wins = (int)
                            entry.getValue().stream().filter(this::isCorrect).count();
                    double winRate = total > 0 ? (double) wins / total : 0.0;
                    return new TypeBreakdown(entry.getKey().name(), total, wins, winRate);
                })
                .toList();
    }

    private Instant parseDateRangeStart(String dateRange) {
        if (dateRange == null || dateRange.isBlank() || "season".equalsIgnoreCase(dateRange)) {
            return Instant.EPOCH;
        }
        String trimmed = dateRange.trim().toLowerCase(Locale.ROOT);
        if (trimmed.endsWith("d")) {
            try {
                int days = Integer.parseInt(trimmed.substring(0, trimmed.length() - 1));
                return Instant.now().minus(days, ChronoUnit.DAYS);
            } catch (NumberFormatException e) {
                log.warn("Invalid date range format: {}", dateRange);
                return Instant.EPOCH;
            }
        }
        return Instant.EPOCH;
    }
}
