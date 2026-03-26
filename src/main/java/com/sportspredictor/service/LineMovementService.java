package com.sportspredictor.service;

import com.sportspredictor.entity.OddsSnapshot;
import com.sportspredictor.repository.OddsSnapshotRepository;
import com.sportspredictor.util.OddsUtil;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Detects line movement, arbitrage opportunities, and market consensus from odds snapshots. */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class LineMovementService {

    private static final int SCALE = 4;
    private static final double SHARP_MOVE_THRESHOLD = 1.0;

    private final OddsSnapshotRepository oddsSnapshotRepository;

    /** Result of line movement detection. */
    public record LineMovementResult(
            String eventId,
            String openingOddsData,
            String currentOddsData,
            String bookmaker,
            double delta,
            String direction,
            boolean isSharpMove,
            int snapshotCount,
            String summary) {}

    /** A single arbitrage opportunity. */
    public record ArbOpportunity(
            String eventId, String sport, String outcome1Book, String outcome2Book, double profitMargin) {}

    /** Result of arbitrage scan. */
    public record ArbitrageResult(List<ArbOpportunity> opportunities, int eventsScanned, String summary) {}

    /** Result of market consensus. */
    public record MarketConsensusResult(
            String eventId,
            double averageImpliedProbability,
            int bookmakerCount,
            Map<String, Double> oddsByBookmaker,
            String summary) {}

    /** Detects line movement for an event by comparing earliest and latest odds snapshots. */
    public LineMovementResult detectLineMovement(String eventId) {
        List<OddsSnapshot> snapshots = oddsSnapshotRepository.findByEventIdOrderByCapturedAtAsc(eventId);

        if (snapshots.isEmpty()) {
            return new LineMovementResult(eventId, "N/A", "N/A", "N/A", 0.0, "none", false, 0, "No odds data found");
        }

        OddsSnapshot opening = snapshots.get(0);
        OddsSnapshot current = snapshots.get(snapshots.size() - 1);

        double delta = 0.0;
        try {
            double openVal = Double.parseDouble(opening.getOddsData());
            double currVal = Double.parseDouble(current.getOddsData());
            delta = currVal - openVal;
        } catch (NumberFormatException e) {
            log.debug("Non-numeric odds data for event {}", eventId);
        }

        String direction = delta > 0 ? "up" : delta < 0 ? "down" : "unchanged";
        boolean isSharp = Math.abs(delta) >= SHARP_MOVE_THRESHOLD;

        String summary = String.format(
                "Event %s: line moved %s by %.2f (%s). %d snapshots. %s",
                eventId,
                direction,
                Math.abs(delta),
                opening.getBookmaker(),
                snapshots.size(),
                isSharp ? "SHARP MOVE detected" : "Normal movement");

        return new LineMovementResult(
                eventId,
                opening.getOddsData(),
                current.getOddsData(),
                current.getBookmaker(),
                delta,
                direction,
                isSharp,
                snapshots.size(),
                summary);
    }

    /** Scans for arbitrage opportunities across bookmakers for a sport. */
    public ArbitrageResult findArbitrageOpportunities(String sport) {
        Instant since = Instant.now().minus(24, ChronoUnit.HOURS);
        List<OddsSnapshot> snapshots = oddsSnapshotRepository.findBySportAndCapturedAtAfter(sport, since);

        Map<String, List<OddsSnapshot>> byEvent = new HashMap<>();
        for (OddsSnapshot snap : snapshots) {
            byEvent.computeIfAbsent(snap.getEventId(), k -> new ArrayList<>()).add(snap);
        }

        List<ArbOpportunity> arbs = new ArrayList<>();
        for (var entry : byEvent.entrySet()) {
            List<OddsSnapshot> eventSnaps = entry.getValue();
            if (eventSnaps.size() < 2) {
                continue;
            }

            double bestProb1 = Double.MAX_VALUE;
            double bestProb2 = Double.MAX_VALUE;
            String book1 = "";
            String book2 = "";

            for (OddsSnapshot snap : eventSnaps) {
                try {
                    int odds = Integer.parseInt(snap.getOddsData());
                    double prob = OddsUtil.impliedProbability(odds);
                    if (prob < bestProb1) {
                        bestProb2 = bestProb1;
                        book2 = book1;
                        bestProb1 = prob;
                        book1 = snap.getBookmaker();
                    } else if (prob < bestProb2) {
                        bestProb2 = prob;
                        book2 = snap.getBookmaker();
                    }
                } catch (NumberFormatException e) {
                    log.debug("Non-numeric odds for snapshot {}", snap.getId());
                }
            }

            double totalImplied = bestProb1 + bestProb2;
            if (totalImplied < 1.0) {
                double margin = BigDecimal.valueOf(1.0 - totalImplied)
                        .setScale(SCALE, RoundingMode.HALF_UP)
                        .doubleValue();
                arbs.add(new ArbOpportunity(entry.getKey(), sport, book1, book2, margin));
            }
        }

        String summary = String.format(
                "Scanned %d events for %s. Found %d arbitrage opportunities.", byEvent.size(), sport, arbs.size());

        return new ArbitrageResult(arbs, byEvent.size(), summary);
    }

    /** Computes market consensus (average implied probability) across bookmakers for an event. */
    public MarketConsensusResult getMarketConsensus(String eventId) {
        List<OddsSnapshot> snapshots = oddsSnapshotRepository.findByEventIdOrderByCapturedAtAsc(eventId);

        Map<String, Double> oddsByBook = new HashMap<>();
        double totalProb = 0.0;
        int count = 0;

        for (OddsSnapshot snap : snapshots) {
            try {
                int odds = Integer.parseInt(snap.getOddsData());
                double prob = OddsUtil.impliedProbability(odds);
                oddsByBook.put(snap.getBookmaker(), prob);
                totalProb += prob;
                count++;
            } catch (NumberFormatException e) {
                log.debug("Non-numeric odds in snapshot {}", snap.getId());
            }
        }

        double avgProb = count > 0 ? totalProb / count : 0.0;
        String summary = String.format(
                "Market consensus for %s: %.1f%% implied probability across %d bookmakers.",
                eventId, avgProb * 100, count);

        return new MarketConsensusResult(eventId, avgProb, count, oddsByBook, summary);
    }
}
