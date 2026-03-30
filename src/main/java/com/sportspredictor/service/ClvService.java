package com.sportspredictor.service;

import com.sportspredictor.entity.Bet;
import com.sportspredictor.entity.OddsSnapshot;
import com.sportspredictor.entity.enums.BetStatus;
import com.sportspredictor.repository.BetRepository;
import com.sportspredictor.repository.OddsSnapshotRepository;
import com.sportspredictor.util.OddsUtil;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Closing Line Value (CLV) analysis — measures betting skill by comparing placed vs closing odds. */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class ClvService {

    private final BetRepository betRepository;
    private final OddsSnapshotRepository oddsSnapshotRepository;

    /** CLV result for a single bet. */
    public record SingleClvResult(String betId, int placedOdds, int closingOdds, double clvPercent, String summary) {}

    /** CLV report across settled bets. */
    public record ClvReportResult(
            int totalBets, double averageClv, double positiveClvRate, List<SportClv> bySport, String summary) {}

    /** CLV breakdown by sport. */
    public record SportClv(String sport, double averageClv, int count) {}

    /** CLV history over time. */
    public record ClvHistoryResult(List<ClvDataPoint> dataPoints, double trendSlope, String summary) {}

    /** Single CLV data point. */
    public record ClvDataPoint(String date, double clvPercent, int betCount) {}

    /** Calculates CLV for a single bet by comparing its odds to the closing line. */
    public SingleClvResult calculateClosingLineValue(String betId) {
        Bet bet = betRepository
                .findById(betId)
                .orElseThrow(() -> new IllegalArgumentException("Bet not found: " + betId));

        int placedOdds = OddsUtil.decimalToAmerican(bet.getOdds().doubleValue());
        int closingOdds = findClosingOdds(bet.getEventId());

        double clvPercent = computeClvPercent(placedOdds, closingOdds);

        String summary = String.format(
                Locale.ROOT,
                "Bet %s: placed at %s, closed at %s. CLV: %+.2f%%",
                betId,
                formatOdds(placedOdds),
                formatOdds(closingOdds),
                clvPercent);

        return new SingleClvResult(betId, placedOdds, closingOdds, clvPercent, summary);
    }

    /** Generates a CLV report across settled bets for the given period and sport. */
    public ClvReportResult getClosingLineValueReport(String startDate, String endDate, String sport) {
        List<Bet> settledBets = getSettledBets(startDate, endDate, sport);

        if (settledBets.isEmpty()) {
            return new ClvReportResult(0, 0, 0, List.of(), "No settled bets found for the given criteria");
        }

        List<Double> clvValues = new ArrayList<>();
        Map<String, List<Double>> bySportMap = new java.util.HashMap<>();

        for (Bet bet : settledBets) {
            int placedOdds = OddsUtil.decimalToAmerican(bet.getOdds().doubleValue());
            int closingOdds = findClosingOdds(bet.getEventId());
            double clv = computeClvPercent(placedOdds, closingOdds);
            clvValues.add(clv);
            bySportMap.computeIfAbsent(bet.getSport(), k -> new ArrayList<>()).add(clv);
        }

        double avgClv =
                clvValues.stream().mapToDouble(Double::doubleValue).average().orElse(0);
        double positiveRate = (double) clvValues.stream().filter(v -> v > 0).count() / clvValues.size();

        List<SportClv> bySport = bySportMap.entrySet().stream()
                .map(e -> new SportClv(
                        e.getKey(),
                        e.getValue().stream()
                                .mapToDouble(Double::doubleValue)
                                .average()
                                .orElse(0),
                        e.getValue().size()))
                .toList();

        String summary = String.format(
                Locale.ROOT,
                "CLV Report: %d bets, avg CLV %+.2f%%, positive CLV rate %.1f%%",
                settledBets.size(),
                avgClv,
                positiveRate * 100);

        return new ClvReportResult(settledBets.size(), avgClv, positiveRate, bySport, summary);
    }

    /** Analyzes CLV trends over time for historical prediction quality assessment. */
    public ClvHistoryResult compareToClosingLine(String startDate, String endDate, String sport) {
        List<Bet> settledBets = getSettledBets(startDate, endDate, sport);

        if (settledBets.isEmpty()) {
            return new ClvHistoryResult(List.of(), 0, "No data for CLV history");
        }

        Map<String, List<Double>> byDate = settledBets.stream()
                .collect(Collectors.groupingBy(
                        b -> b.getPlacedAt().toString().substring(0, 10),
                        Collectors.mapping(
                                b -> {
                                    int placed = OddsUtil.decimalToAmerican(
                                            b.getOdds().doubleValue());
                                    int closing = findClosingOdds(b.getEventId());
                                    return computeClvPercent(placed, closing);
                                },
                                Collectors.toList())));

        List<ClvDataPoint> dataPoints = byDate.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .map(e -> new ClvDataPoint(
                        e.getKey(),
                        e.getValue().stream()
                                .mapToDouble(Double::doubleValue)
                                .average()
                                .orElse(0),
                        e.getValue().size()))
                .toList();

        double trendSlope = calculateTrendSlope(dataPoints);

        String trend = trendSlope > 0 ? "improving" : trendSlope < 0 ? "declining" : "flat";
        String summary = String.format(
                Locale.ROOT,
                "CLV History: %d data points, trend %s (slope: %+.4f)",
                dataPoints.size(),
                trend,
                trendSlope);

        return new ClvHistoryResult(dataPoints, trendSlope, summary);
    }

    private List<Bet> getSettledBets(String startDate, String endDate, String sport) {
        Instant start = startDate != null ? Instant.parse(startDate + "T00:00:00Z") : Instant.EPOCH;
        Instant end = endDate != null ? Instant.parse(endDate + "T23:59:59Z") : Instant.now();

        List<BetStatus> settledStatuses = List.of(BetStatus.WON, BetStatus.LOST, BetStatus.PUSHED);
        List<Bet> bets = betRepository.findByStatusInAndSettledAtBetween(settledStatuses, start, end);

        if (sport != null && !sport.isBlank()) {
            bets = bets.stream()
                    .filter(b -> b.getSport().equalsIgnoreCase(sport))
                    .toList();
        }
        return bets;
    }

    private int findClosingOdds(String eventId) {
        List<OddsSnapshot> snapshots = oddsSnapshotRepository.findByEventIdOrderByCapturedAtAsc(eventId);

        if (snapshots.isEmpty()) {
            return OddsUtil.EVEN_ODDS;
        }

        OddsSnapshot lastSnapshot = snapshots.getLast();
        try {
            String oddsData = lastSnapshot.getOddsData();
            if (oddsData.matches("-?\\d+")) {
                return Integer.parseInt(oddsData);
            }
        } catch (NumberFormatException e) {
            log.debug("Could not parse closing odds for event {}: {}", eventId, e.getMessage());
        }

        return OddsUtil.EVEN_ODDS;
    }

    private static double computeClvPercent(int placedOdds, int closingOdds) {
        double placedProb = OddsUtil.impliedProbability(placedOdds);
        double closingProb = OddsUtil.impliedProbability(closingOdds);

        if (closingProb == 0) {
            return 0;
        }
        return ((closingProb - placedProb) / closingProb) * 100;
    }

    private static double calculateTrendSlope(List<ClvDataPoint> points) {
        if (points.size() < 2) {
            return 0;
        }

        double n = points.size();
        double sumX = 0;
        double sumY = 0;
        double sumXy = 0;
        double sumX2 = 0;

        for (int i = 0; i < points.size(); i++) {
            double x = i;
            double y = points.get(i).clvPercent();
            sumX += x;
            sumY += y;
            sumXy += x * y;
            sumX2 += x * x;
        }

        double denominator = n * sumX2 - sumX * sumX;
        if (denominator == 0) {
            return 0;
        }
        return (n * sumXy - sumX * sumY) / denominator;
    }

    private static String formatOdds(int odds) {
        return odds > 0 ? "+" + odds : String.valueOf(odds);
    }
}
