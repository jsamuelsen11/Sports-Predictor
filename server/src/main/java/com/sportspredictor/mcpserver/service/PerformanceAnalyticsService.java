package com.sportspredictor.mcpserver.service;

import com.sportspredictor.mcpserver.entity.BankrollTransaction;
import com.sportspredictor.mcpserver.entity.Bet;
import com.sportspredictor.mcpserver.entity.enums.BetStatus;
import com.sportspredictor.mcpserver.entity.enums.BetType;
import com.sportspredictor.mcpserver.repository.BankrollTransactionRepository;
import com.sportspredictor.mcpserver.repository.BetRepository;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Performance analytics: streaks, ROI, parlay stats, profit graphs, season summaries, export. */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class PerformanceAnalyticsService {

    private static final int SCALE = 2;
    private static final List<BetStatus> SETTLED = List.of(BetStatus.WON, BetStatus.LOST, BetStatus.PUSHED);

    private final BetRepository betRepository;
    private final BankrollTransactionRepository transactionRepository;
    private final BankrollService bankrollService;

    /** Streak history result. */
    public record StreakResult(
            int currentStreak, String currentType, int longestWinStreak, int longestLossStreak, String summary) {}

    /** ROI by category result. */
    public record RoiResult(
            Map<String, Double> roiBySport, Map<String, Double> roiByBetType, double overallRoi, String summary) {}

    /** Parlay performance result. */
    public record ParlayResult(int totalParlays, double hitRate, double avgPayout, double bestPayout, String summary) {}

    /** Profit graph data point. */
    public record DataPoint(String date, double balance) {}

    /** Profit graph result. */
    public record ProfitGraphResult(List<DataPoint> dataPoints, String granularity, String summary) {}

    /** Season summary result. */
    public record SeasonSummaryResult(
            int totalBets, int wins, int losses, int pushes, double netProfit, double roi, String summary) {}

    /** Export result. */
    public record ExportResult(String data, String format, int count, String summary) {}

    /** Computes win/loss streak history. */
    public StreakResult getStreakHistory(String bankrollId) {
        List<Bet> settled = getSettledBets(bankrollId);
        settled.sort(Comparator.comparing(Bet::getSettledAt, Comparator.nullsLast(Comparator.naturalOrder())));

        int current = 0;
        String currentType = "none";
        int maxWin = 0;
        int maxLoss = 0;
        int streak = 0;
        BetStatus lastStatus = null;

        for (Bet bet : settled) {
            if (bet.getStatus() == BetStatus.PUSHED) {
                continue;
            }
            if (bet.getStatus() == lastStatus) {
                streak++;
            } else {
                streak = 1;
                lastStatus = bet.getStatus();
            }
            if (lastStatus == BetStatus.WON) {
                maxWin = Math.max(maxWin, streak);
            } else {
                maxLoss = Math.max(maxLoss, streak);
            }
        }
        current = streak;
        currentType = lastStatus != null ? lastStatus.name().toLowerCase(Locale.ROOT) : "none";

        return new StreakResult(
                current,
                currentType,
                maxWin,
                maxLoss,
                String.format(
                        "Current: %d %s. Best win streak: %d. Worst loss streak: %d.",
                        current, currentType, maxWin, maxLoss));
    }

    /** Computes ROI by sport and bet type. */
    public RoiResult getRoiByCategory(String bankrollId) {
        List<Bet> settled = getSettledBets(bankrollId);
        Map<String, Double> bySport = computeRoi(settled, Bet::getSport);
        Map<String, Double> byType = computeRoi(settled, b -> b.getBetType().name());
        double overall = computeOverallRoi(settled);

        return new RoiResult(
                bySport,
                byType,
                overall,
                String.format("Overall ROI: %.1f%% across %d bets.", overall * 100, settled.size()));
    }

    /** Computes parlay-specific performance. */
    public ParlayResult getParlayPerformance(String bankrollId) {
        List<Bet> parlays = betRepository.findByBankrollIdAndStatus(bankrollId, BetStatus.WON).stream()
                .filter(b -> b.getBetType() == BetType.PARLAY)
                .toList();
        List<Bet> allParlays = betRepository.findByBankrollId(bankrollId).stream()
                .filter(b -> b.getBetType() == BetType.PARLAY)
                .filter(b -> SETTLED.contains(b.getStatus()))
                .toList();

        int total = allParlays.size();
        double hitRate = total > 0 ? (double) parlays.size() / total : 0.0;
        double avgPayout = parlays.stream()
                .map(b -> b.getActualPayout() != null ? b.getActualPayout() : BigDecimal.ZERO)
                .mapToDouble(BigDecimal::doubleValue)
                .average()
                .orElse(0.0);
        double best = parlays.stream()
                .map(b -> b.getActualPayout() != null ? b.getActualPayout() : BigDecimal.ZERO)
                .mapToDouble(BigDecimal::doubleValue)
                .max()
                .orElse(0.0);

        return new ParlayResult(
                total,
                hitRate,
                avgPayout,
                best,
                String.format("%d parlays, %.0f%% hit rate, avg payout $%.2f.", total, hitRate * 100, avgPayout));
    }

    /** Generates profit graph data points. */
    public ProfitGraphResult getProfitGraphData(String bankrollId, String granularity) {
        List<BankrollTransaction> txns = transactionRepository.findByBankrollId(bankrollId);
        txns.sort(Comparator.comparing(BankrollTransaction::getCreatedAt));

        List<DataPoint> points = new ArrayList<>();
        for (BankrollTransaction txn : txns) {
            String date = DateTimeFormatter.ISO_LOCAL_DATE.format(
                    txn.getCreatedAt().atZone(ZoneOffset.UTC).toLocalDate());
            points.add(new DataPoint(date, txn.getBalanceAfter().doubleValue()));
        }

        return new ProfitGraphResult(
                points, granularity, String.format("%d data points (%s granularity).", points.size(), granularity));
    }

    /** Generates a season/period summary. */
    public SeasonSummaryResult getSeasonSummary(String bankrollId, String startDate, String endDate) {
        Instant start = LocalDate.parse(startDate).atStartOfDay(ZoneOffset.UTC).toInstant();
        Instant end = LocalDate.parse(endDate)
                .plusDays(1)
                .atStartOfDay(ZoneOffset.UTC)
                .toInstant();
        List<Bet> bets = betRepository.findByStatusInAndSettledAtBetween(SETTLED, start, end).stream()
                .filter(b -> b.getBankroll().getId().equals(bankrollId))
                .toList();

        int wins =
                (int) bets.stream().filter(b -> b.getStatus() == BetStatus.WON).count();
        int losses =
                (int) bets.stream().filter(b -> b.getStatus() == BetStatus.LOST).count();
        int pushes = (int)
                bets.stream().filter(b -> b.getStatus() == BetStatus.PUSHED).count();
        double profit = computeProfit(bets);
        double totalStaked =
                bets.stream().mapToDouble(b -> b.getStake().doubleValue()).sum();
        double roi = totalStaked > 0 ? profit / totalStaked : 0.0;

        return new SeasonSummaryResult(
                bets.size(),
                wins,
                losses,
                pushes,
                profit,
                roi,
                String.format(
                        "%d bets: %d-%d-%d. Profit: $%.2f (%.1f%% ROI).",
                        bets.size(), wins, losses, pushes, profit, roi * 100));
    }

    /** Exports bet history as JSON or CSV string. */
    public ExportResult exportBetHistory(String bankrollId, String format) {
        List<Bet> bets = betRepository.findByBankrollId(bankrollId);
        String data;
        if ("csv".equalsIgnoreCase(format)) {
            StringBuilder sb = new StringBuilder("id,sport,type,status,stake,odds,payout,placed_at\n");
            for (Bet b : bets) {
                sb.append(String.format(
                        "%s,%s,%s,%s,%.2f,%.3f,%s,%s%n",
                        b.getId(),
                        b.getSport(),
                        b.getBetType(),
                        b.getStatus(),
                        b.getStake(),
                        b.getOdds(),
                        b.getActualPayout() != null ? b.getActualPayout().toPlainString() : "",
                        b.getPlacedAt()));
            }
            data = sb.toString();
        } else {
            data = "["
                    + bets.stream()
                            .map(b -> String.format(
                                    "{\"id\":\"%s\",\"sport\":\"%s\",\"status\":\"%s\"}",
                                    b.getId(), b.getSport(), b.getStatus()))
                            .collect(Collectors.joining(","))
                    + "]";
        }
        return new ExportResult(
                data, format, bets.size(), String.format("Exported %d bets as %s.", bets.size(), format));
    }

    private List<Bet> getSettledBets(String bankrollId) {
        return betRepository.findByBankrollId(bankrollId).stream()
                .filter(b -> SETTLED.contains(b.getStatus()))
                .toList();
    }

    private double computeProfit(List<Bet> bets) {
        double profit = 0.0;
        for (Bet b : bets) {
            if (b.getStatus() == BetStatus.WON && b.getActualPayout() != null) {
                profit += b.getActualPayout().subtract(b.getStake()).doubleValue();
            } else if (b.getStatus() == BetStatus.LOST) {
                profit -= b.getStake().doubleValue();
            }
        }
        return BigDecimal.valueOf(profit).setScale(SCALE, RoundingMode.HALF_UP).doubleValue();
    }

    private double computeOverallRoi(List<Bet> bets) {
        double totalStaked =
                bets.stream().mapToDouble(b -> b.getStake().doubleValue()).sum();
        if (totalStaked == 0) {
            return 0.0;
        }
        return computeProfit(bets) / totalStaked;
    }

    private Map<String, Double> computeRoi(List<Bet> bets, java.util.function.Function<Bet, String> keyFn) {
        Map<String, List<Bet>> grouped = bets.stream().collect(Collectors.groupingBy(keyFn));
        Map<String, Double> result = new HashMap<>();
        for (var entry : grouped.entrySet()) {
            double staked = entry.getValue().stream()
                    .mapToDouble(b -> b.getStake().doubleValue())
                    .sum();
            double profit = computeProfit(entry.getValue());
            result.put(entry.getKey(), staked > 0 ? profit / staked : 0.0);
        }
        return result;
    }
}
