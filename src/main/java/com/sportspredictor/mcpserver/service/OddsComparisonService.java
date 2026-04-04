package com.sportspredictor.mcpserver.service;

import com.sportspredictor.mcpserver.util.OddsUtil;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/** Compares odds across bookmakers and identifies value bets from market inefficiencies. */
@Service
@RequiredArgsConstructor
@Slf4j
public class OddsComparisonService {

    private static final double PERCENT_MULTIPLIER = 100.0;
    private static final double STRONG_EDGE_THRESHOLD = 10.0;
    private static final double GOOD_EDGE_THRESHOLD = 5.0;

    private final OddsService oddsService;

    /** Side-by-side odds comparison for an event across all bookmakers. */
    public record OddsComparison(
            String eventId,
            String homeTeam,
            String awayTeam,
            List<BookmakerLine> bookmakerLines,
            BestLine bestHome,
            BestLine bestAway,
            String summary) {}

    /** A single bookmaker's line for one outcome. */
    public record BookmakerLine(String bookmaker, String outcomeName, int price, double impliedProbability) {}

    /** The best available line for an outcome. */
    public record BestLine(String outcomeName, String bookmaker, int price, double impliedProbability) {}

    /** A value bet where one bookmaker's line deviates from the consensus. */
    public record ValueBet(
            String eventId,
            String homeTeam,
            String awayTeam,
            String outcomeName,
            String bookmaker,
            int price,
            double impliedProbability,
            double edgePercent,
            double expectedValuePerUnit,
            String rating) {}

    /** Compares odds for a specific event across all bookmakers. */
    public OddsComparison compareOdds(String sport, String eventId, String market) {
        try {
            OddsService.LiveOddsResult odds = oddsService.getLiveOdds(sport, eventId, market);

            if (odds.events().isEmpty()) {
                return new OddsComparison(eventId, "", "", List.of(), null, null, "No odds available for this event.");
            }

            OddsService.EventOdds event = odds.events().get(0);
            List<BookmakerLine> lines = new ArrayList<>();

            for (OddsService.BookmakerOdds bookmaker : event.bookmakers()) {
                for (OddsService.MarketOdds mkt : bookmaker.markets()) {
                    for (OddsService.OutcomeOdds outcome : mkt.outcomes()) {
                        int price = (int) outcome.price();
                        double implied = OddsUtil.impliedProbability(price);
                        lines.add(new BookmakerLine(bookmaker.title(), outcome.name(), price, implied));
                    }
                }
            }

            BestLine bestHome = findBestLine(lines, event.homeTeam());
            BestLine bestAway = findBestLine(lines, event.awayTeam());

            String summary = String.format(
                    Locale.ROOT,
                    "%s vs %s: %d bookmaker lines compared. Best %s: %s at %+d. Best %s: %s at %+d.",
                    event.homeTeam(),
                    event.awayTeam(),
                    lines.size(),
                    event.homeTeam(),
                    bestHome != null ? bestHome.bookmaker() : "N/A",
                    bestHome != null ? bestHome.price() : 0,
                    event.awayTeam(),
                    bestAway != null ? bestAway.bookmaker() : "N/A",
                    bestAway != null ? bestAway.price() : 0);

            return new OddsComparison(eventId, event.homeTeam(), event.awayTeam(), lines, bestHome, bestAway, summary);
        } catch (Exception e) {
            log.warn("Failed to compare odds for sport={}, event={}: {}", sport, eventId, e.getMessage());
            return new OddsComparison(eventId, "", "", List.of(), null, null, "Failed to retrieve odds comparison.");
        }
    }

    /** Finds value bets across all events where a bookmaker's line deviates from consensus. */
    public List<ValueBet> findValueBets(String sport, String market, double minEdgePct) {
        try {
            OddsService.LiveOddsResult odds = oddsService.getLiveOdds(sport, null, market);
            List<ValueBet> valueBets = new ArrayList<>();

            for (OddsService.EventOdds event : odds.events()) {
                Map<String, List<BookmakerLine>> linesByOutcome = groupLinesByOutcome(event);

                for (Map.Entry<String, List<BookmakerLine>> entry : linesByOutcome.entrySet()) {
                    String outcomeName = entry.getKey();
                    List<BookmakerLine> outcomelLines = entry.getValue();

                    if (outcomelLines.size() < 2) {
                        continue;
                    }

                    double consensusProb = outcomelLines.stream()
                            .mapToDouble(BookmakerLine::impliedProbability)
                            .average()
                            .orElse(0.0);

                    for (BookmakerLine line : outcomelLines) {
                        double edge = (consensusProb - line.impliedProbability()) * PERCENT_MULTIPLIER;
                        if (edge >= minEdgePct) {
                            double decimalOdds = OddsUtil.americanToDecimal(line.price());
                            double ev = (consensusProb * (decimalOdds - 1.0)) - (1.0 - consensusProb);
                            String rating = rateEdge(edge);

                            valueBets.add(new ValueBet(
                                    event.id(),
                                    event.homeTeam(),
                                    event.awayTeam(),
                                    outcomeName,
                                    line.bookmaker(),
                                    line.price(),
                                    line.impliedProbability(),
                                    edge,
                                    ev,
                                    rating));
                        }
                    }
                }
            }

            valueBets.sort(Comparator.comparingDouble(ValueBet::edgePercent).reversed());
            return valueBets;
        } catch (Exception e) {
            log.warn("Failed to find value bets for sport={}: {}", sport, e.getMessage());
            return List.of();
        }
    }

    private BestLine findBestLine(List<BookmakerLine> lines, String outcomeName) {
        return lines.stream()
                .filter(l -> l.outcomeName().equalsIgnoreCase(outcomeName))
                .max(Comparator.comparingInt(BookmakerLine::price))
                .map(l -> new BestLine(l.outcomeName(), l.bookmaker(), l.price(), l.impliedProbability()))
                .orElse(null);
    }

    private Map<String, List<BookmakerLine>> groupLinesByOutcome(OddsService.EventOdds event) {
        Map<String, List<BookmakerLine>> grouped = new HashMap<>();
        for (OddsService.BookmakerOdds bookmaker : event.bookmakers()) {
            for (OddsService.MarketOdds mkt : bookmaker.markets()) {
                for (OddsService.OutcomeOdds outcome : mkt.outcomes()) {
                    int price = (int) outcome.price();
                    double implied = OddsUtil.impliedProbability(price);
                    grouped.computeIfAbsent(outcome.name(), k -> new ArrayList<>())
                            .add(new BookmakerLine(bookmaker.title(), outcome.name(), price, implied));
                }
            }
        }
        return grouped;
    }

    private static String rateEdge(double edgePercent) {
        if (edgePercent >= STRONG_EDGE_THRESHOLD) {
            return "STRONG";
        } else if (edgePercent >= GOOD_EDGE_THRESHOLD) {
            return "GOOD";
        } else {
            return "MARGINAL";
        }
    }
}
