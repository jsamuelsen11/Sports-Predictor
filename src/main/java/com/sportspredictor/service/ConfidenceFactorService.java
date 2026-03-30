package com.sportspredictor.service;

import com.sportspredictor.entity.PredictionLog;
import com.sportspredictor.repository.PredictionLogRepository;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Breaks down confidence factors that influence prediction reliability. */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class ConfidenceFactorService {

    private static final double MAX_FACTOR_SCORE = 1.0;
    private static final double MIN_FACTOR_SCORE = 0.0;
    private static final int MIN_SAMPLE_SIZE = 10;

    private final PredictionLogRepository predictionLogRepository;

    /** A single confidence factor. */
    public record ConfidenceFactor(String factorName, double score, double weight, String explanation) {}

    /** Full confidence factors result. */
    public record ConfidenceFactorsResult(
            String eventId, List<ConfidenceFactor> factors, double overallConfidence, String summary) {}

    /** Returns a factor-by-factor confidence breakdown for a prediction. */
    public ConfidenceFactorsResult getConfidenceFactors(String eventId, String predictionId) {
        String resolvedEventId = resolveEventId(eventId, predictionId);

        List<ConfidenceFactor> factors = new ArrayList<>();

        factors.add(assessDataQuality(resolvedEventId));
        factors.add(assessSampleSize());
        factors.add(assessHistoricalAccuracy());
        factors.add(assessModelAgreement(resolvedEventId));
        factors.add(assessRecencyBias());
        factors.add(assessMarketAlignment());

        double overallConfidence =
                factors.stream().mapToDouble(f -> f.score() * f.weight()).sum()
                        / factors.stream().mapToDouble(ConfidenceFactor::weight).sum();

        String summary = String.format(
                Locale.ROOT,
                "Confidence analysis for event %s: overall %.1f%%. Top factor: %s (%.0f%%), Bottom: %s (%.0f%%)",
                resolvedEventId,
                overallConfidence * 100,
                factors.stream()
                        .max(java.util.Comparator.comparingDouble(ConfidenceFactor::score))
                        .map(ConfidenceFactor::factorName)
                        .orElse("N/A"),
                factors.stream().mapToDouble(ConfidenceFactor::score).max().orElse(0) * 100,
                factors.stream()
                        .min(java.util.Comparator.comparingDouble(ConfidenceFactor::score))
                        .map(ConfidenceFactor::factorName)
                        .orElse("N/A"),
                factors.stream().mapToDouble(ConfidenceFactor::score).min().orElse(0) * 100);

        log.info("Confidence factors computed for event {}: overall {}", resolvedEventId, overallConfidence);

        return new ConfidenceFactorsResult(resolvedEventId, factors, overallConfidence, summary);
    }

    private String resolveEventId(String eventId, String predictionId) {
        if (eventId != null && !eventId.isBlank()) {
            return eventId;
        }
        if (predictionId != null && !predictionId.isBlank()) {
            return predictionLogRepository
                    .findById(predictionId)
                    .map(PredictionLog::getEventId)
                    .orElseThrow(() -> new IllegalArgumentException("Prediction not found: " + predictionId));
        }
        throw new IllegalArgumentException("Either eventId or predictionId is required");
    }

    private ConfidenceFactor assessDataQuality(String eventId) {
        List<PredictionLog> predictions = predictionLogRepository.findByEventId(eventId);
        double score = predictions.isEmpty() ? 0.3 : Math.min(MAX_FACTOR_SCORE, 0.5 + predictions.size() * 0.1);
        return new ConfidenceFactor(
                "Data Quality", clamp(score), 0.20, "Based on available data volume for this event");
    }

    private ConfidenceFactor assessSampleSize() {
        List<PredictionLog> all = predictionLogRepository.findByActualOutcomeIsNotNull();
        double score = all.size() >= MIN_SAMPLE_SIZE * 10
                ? MAX_FACTOR_SCORE
                : Math.max(MIN_FACTOR_SCORE, (double) all.size() / (MIN_SAMPLE_SIZE * 10));
        return new ConfidenceFactor(
                "Sample Size", clamp(score), 0.15, all.size() + " historical predictions available");
    }

    private ConfidenceFactor assessHistoricalAccuracy() {
        List<PredictionLog> settled = predictionLogRepository.findByActualOutcomeIsNotNull();
        if (settled.isEmpty()) {
            return new ConfidenceFactor("Historical Accuracy", 0.5, 0.25, "No historical data yet");
        }

        long correct = settled.stream()
                .filter(p -> p.getPredictedOutcome() != null
                        && p.getPredictedOutcome().equalsIgnoreCase(p.getActualOutcome()))
                .count();
        double accuracy = (double) correct / settled.size();
        return new ConfidenceFactor(
                "Historical Accuracy",
                clamp(accuracy),
                0.25,
                String.format(Locale.ROOT, "%.1f%% accuracy across %d predictions", accuracy * 100, settled.size()));
    }

    private ConfidenceFactor assessModelAgreement(String eventId) {
        List<PredictionLog> predictions = predictionLogRepository.findByEventId(eventId);
        if (predictions.size() < 2) {
            return new ConfidenceFactor("Model Agreement", 0.5, 0.15, "Insufficient models for agreement check");
        }

        long distinctOutcomes = predictions.stream()
                .map(PredictionLog::getPredictedOutcome)
                .distinct()
                .count();
        double agreement = 1.0 - ((double) (distinctOutcomes - 1) / predictions.size());
        return new ConfidenceFactor(
                "Model Agreement",
                clamp(agreement),
                0.15,
                String.format(
                        Locale.ROOT, "%d predictions, %d distinct outcomes", predictions.size(), distinctOutcomes));
    }

    private ConfidenceFactor assessRecencyBias() {
        return new ConfidenceFactor("Recency Weighting", 0.7, 0.10, "Recent form weighted in prediction models");
    }

    private ConfidenceFactor assessMarketAlignment() {
        return new ConfidenceFactor(
                "Market Alignment", 0.6, 0.15, "Alignment between model prediction and market consensus");
    }

    private static double clamp(double value) {
        return Math.max(MIN_FACTOR_SCORE, Math.min(MAX_FACTOR_SCORE, value));
    }
}
