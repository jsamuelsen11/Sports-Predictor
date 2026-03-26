package com.sportspredictor.service;

import com.sportspredictor.entity.PredictionLog;
import com.sportspredictor.repository.PredictionLogRepository;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Backtesting engine: replay strategies against historical predictions, export logs. */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class BacktestingService {

    private static final int SCALE = 4;

    private final PredictionLogRepository predictionLogRepository;

    /** Backtest result. */
    public record BacktestResult(
            int totalPredictions,
            int correctPredictions,
            double accuracy,
            double simulatedProfit,
            double roi,
            double maxDrawdown,
            String summary) {}

    /** Model performance by factor result. */
    public record FactorPerformanceResult(
            Map<String, Double> accuracyByFactor, int totalPredictions, String factor, String summary) {}

    /** Export result. */
    public record ExportPredictionResult(String data, String format, int count, String summary) {}

    /**
     * Replays a betting strategy against historical predictions.
     *
     * <p>Filters predictions by sport, confidence, and date range, then simulates
     * flat-stake betting to compute P/L and accuracy.
     */
    public BacktestResult backtestStrategy(
            String sport, String betType, double minConfidence, double minEdge, String startDate, String endDate) {

        Instant start = LocalDate.parse(startDate).atStartOfDay(ZoneOffset.UTC).toInstant();
        Instant end = LocalDate.parse(endDate)
                .plusDays(1)
                .atStartOfDay(ZoneOffset.UTC)
                .toInstant();

        List<PredictionLog> predictions;
        if (sport != null && !sport.isBlank()) {
            predictions = predictionLogRepository.findBySportAndCreatedAtBetween(sport, start, end);
        } else {
            predictions = predictionLogRepository.findByCreatedAtBetween(start, end);
        }

        predictions = predictions.stream()
                .filter(p -> p.getActualOutcome() != null)
                .filter(p -> p.getConfidence() >= minConfidence)
                .toList();

        int correct = 0;
        double profit = 0.0;
        double peak = 0.0;
        double maxDrawdown = 0.0;

        for (PredictionLog p : predictions) {
            boolean isCorrect = p.getPredictedOutcome().equalsIgnoreCase(p.getActualOutcome());
            if (isCorrect) {
                correct++;
                profit += 1.0; // Flat 1-unit win
            } else {
                profit -= 1.0; // Flat 1-unit loss
            }
            if (profit > peak) {
                peak = profit;
            }
            double drawdown = peak - profit;
            if (drawdown > maxDrawdown) {
                maxDrawdown = drawdown;
            }
        }

        double accuracy = predictions.isEmpty() ? 0.0 : (double) correct / predictions.size();
        double roi = predictions.isEmpty() ? 0.0 : profit / predictions.size();

        return new BacktestResult(
                predictions.size(),
                correct,
                accuracy,
                BigDecimal.valueOf(profit).setScale(SCALE, RoundingMode.HALF_UP).doubleValue(),
                roi,
                maxDrawdown,
                String.format(
                        "Backtest: %d predictions, %.1f%% accuracy, %.2f units profit, %.1f%% ROI.",
                        predictions.size(), accuracy * 100, profit, roi * 100));
    }

    /** Breaks down model accuracy by a specified factor. */
    public FactorPerformanceResult getModelPerformanceByFactor(String factor) {
        List<PredictionLog> settled = predictionLogRepository.findByActualOutcomeIsNotNull();

        java.util.function.Function<PredictionLog, String> keyFn =
                switch (factor.toLowerCase(Locale.ROOT)) {
                    case "type" -> p -> p.getPredictionType().name();
                    default -> PredictionLog::getSport;
                };
        Map<String, List<PredictionLog>> grouped = settled.stream().collect(Collectors.groupingBy(keyFn));

        Map<String, Double> accuracyMap = new HashMap<>();
        for (var entry : grouped.entrySet()) {
            long correct = entry.getValue().stream()
                    .filter(p -> p.getPredictedOutcome().equalsIgnoreCase(p.getActualOutcome()))
                    .count();
            accuracyMap.put(entry.getKey(), (double) correct / entry.getValue().size());
        }

        return new FactorPerformanceResult(
                accuracyMap,
                settled.size(),
                factor,
                String.format("Performance by %s across %d predictions.", factor, settled.size()));
    }

    /** Exports prediction log as JSON or CSV. */
    public ExportPredictionResult exportPredictionLog(String sport, String format) {
        List<PredictionLog> predictions;
        if (sport != null && !sport.isBlank()) {
            predictions = predictionLogRepository.findBySport(sport);
        } else {
            predictions = predictionLogRepository.findAll();
        }

        String data;
        if ("csv".equalsIgnoreCase(format)) {
            StringBuilder sb = new StringBuilder("id,sport,type,predicted,actual,confidence\n");
            for (PredictionLog p : predictions) {
                sb.append(String.format(
                        "%s,%s,%s,%s,%s,%.2f%n",
                        p.getId(),
                        p.getSport(),
                        p.getPredictionType(),
                        p.getPredictedOutcome(),
                        p.getActualOutcome(),
                        p.getConfidence()));
            }
            data = sb.toString();
        } else {
            data = "["
                    + predictions.stream()
                            .map(p -> String.format(
                                    "{\"id\":\"%s\",\"sport\":\"%s\",\"predicted\":\"%s\",\"actual\":\"%s\"}",
                                    p.getId(), p.getSport(), p.getPredictedOutcome(), p.getActualOutcome()))
                            .collect(Collectors.joining(","))
                    + "]";
        }

        return new ExportPredictionResult(
                data,
                format,
                predictions.size(),
                String.format("Exported %d predictions as %s.", predictions.size(), format));
    }
}
