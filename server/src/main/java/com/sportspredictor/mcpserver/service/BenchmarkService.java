package com.sportspredictor.mcpserver.service;

import com.sportspredictor.mcpserver.entity.Bet;
import com.sportspredictor.mcpserver.entity.enums.BetStatus;
import com.sportspredictor.mcpserver.repository.BetRepository;
import com.sportspredictor.mcpserver.util.OddsUtil;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ThreadLocalRandom;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Benchmarks system betting performance against random and always-favorites strategies. */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class BenchmarkService {

    private static final int SCALE = 2;
    private static final BigDecimal DEFAULT_STAKE = BigDecimal.TEN;

    private final BetRepository betRepository;

    /** Performance metrics for a single strategy. */
    public record StrategyMetrics(
            String strategyName,
            double winRate,
            BigDecimal profitLoss,
            double roi,
            BigDecimal maxDrawdown,
            int totalBets) {}

    /** Benchmark comparison result. */
    public record BenchmarkResult(
            StrategyMetrics systemMetrics,
            StrategyMetrics randomMetrics,
            StrategyMetrics favoritesMetrics,
            String summary) {}

    /** Compares system performance against random and always-favorites strategies. */
    public BenchmarkResult compareToRandom(String startDate, String endDate, String sport) {
        Instant start = startDate != null ? Instant.parse(startDate + "T00:00:00Z") : Instant.EPOCH;
        Instant end = endDate != null ? Instant.parse(endDate + "T23:59:59Z") : Instant.now();

        List<BetStatus> settledStatuses = List.of(BetStatus.WON, BetStatus.LOST, BetStatus.PUSHED);
        List<Bet> settledBets = betRepository.findByStatusInAndSettledAtBetween(settledStatuses, start, end);

        if (sport != null && !sport.isBlank()) {
            settledBets = settledBets.stream()
                    .filter(b -> b.getSport().equalsIgnoreCase(sport))
                    .toList();
        }

        if (settledBets.isEmpty()) {
            StrategyMetrics empty = new StrategyMetrics("system", 0, BigDecimal.ZERO, 0, BigDecimal.ZERO, 0);
            return new BenchmarkResult(empty, empty, empty, "No settled bets found for the given criteria");
        }

        StrategyMetrics system = computeSystemMetrics(settledBets);
        StrategyMetrics random = simulateRandomStrategy(settledBets);
        StrategyMetrics favorites = simulateFavoritesStrategy(settledBets);

        String summary = String.format(
                Locale.ROOT,
                "Benchmark (%d bets): System ROI %.1f%%, Random ROI %.1f%%, Favorites ROI %.1f%%",
                settledBets.size(),
                system.roi(),
                random.roi(),
                favorites.roi());

        log.info(summary);

        return new BenchmarkResult(system, random, favorites, summary);
    }

    private StrategyMetrics computeSystemMetrics(List<Bet> bets) {
        int won = 0;
        BigDecimal totalWagered = BigDecimal.ZERO;
        BigDecimal totalReturns = BigDecimal.ZERO;
        BigDecimal runningPnL = BigDecimal.ZERO;
        BigDecimal maxDrawdown = BigDecimal.ZERO;
        BigDecimal peak = BigDecimal.ZERO;

        for (Bet bet : bets) {
            totalWagered = totalWagered.add(bet.getStake());
            BigDecimal payout = bet.getActualPayout() != null ? bet.getActualPayout() : BigDecimal.ZERO;
            totalReturns = totalReturns.add(payout);

            if (bet.getStatus() == BetStatus.WON) {
                won++;
            }

            runningPnL = runningPnL.add(payout).subtract(bet.getStake());
            if (runningPnL.compareTo(peak) > 0) {
                peak = runningPnL;
            }
            BigDecimal drawdown = peak.subtract(runningPnL);
            if (drawdown.compareTo(maxDrawdown) > 0) {
                maxDrawdown = drawdown;
            }
        }

        BigDecimal profitLoss = totalReturns.subtract(totalWagered);
        double roi = totalWagered.compareTo(BigDecimal.ZERO) > 0
                ? profitLoss.divide(totalWagered, 4, RoundingMode.HALF_UP).doubleValue() * 100
                : 0;

        return new StrategyMetrics(
                "System",
                bets.isEmpty() ? 0 : (double) won / bets.size() * 100,
                profitLoss.setScale(SCALE, RoundingMode.HALF_UP),
                roi,
                maxDrawdown.setScale(SCALE, RoundingMode.HALF_UP),
                bets.size());
    }

    private StrategyMetrics simulateRandomStrategy(List<Bet> bets) {
        int totalWins = 0;
        BigDecimal totalPnL = BigDecimal.ZERO;
        BigDecimal totalWagered = BigDecimal.ZERO;

        ThreadLocalRandom rng = ThreadLocalRandom.current();
        for (Bet bet : bets) {
            totalWagered = totalWagered.add(DEFAULT_STAKE);
            boolean randomWin = rng.nextBoolean();
            if (randomWin) {
                totalWins++;
                int americanOdds = OddsUtil.decimalToAmerican(bet.getOdds().doubleValue());
                double decimalOdds = OddsUtil.americanToDecimal(americanOdds);
                BigDecimal profit = DEFAULT_STAKE.multiply(BigDecimal.valueOf(decimalOdds - 1));
                totalPnL = totalPnL.add(profit);
            } else {
                totalPnL = totalPnL.subtract(DEFAULT_STAKE);
            }
        }

        double roi = totalWagered.compareTo(BigDecimal.ZERO) > 0
                ? totalPnL.divide(totalWagered, 4, RoundingMode.HALF_UP).doubleValue() * 100
                : 0;

        return new StrategyMetrics(
                "Random",
                bets.isEmpty() ? 0 : (double) totalWins / bets.size() * 100,
                totalPnL.setScale(SCALE, RoundingMode.HALF_UP),
                roi,
                BigDecimal.ZERO,
                bets.size());
    }

    private StrategyMetrics simulateFavoritesStrategy(List<Bet> bets) {
        int totalWins = 0;
        BigDecimal totalPnL = BigDecimal.ZERO;
        BigDecimal totalWagered = BigDecimal.ZERO;

        for (Bet bet : bets) {
            totalWagered = totalWagered.add(DEFAULT_STAKE);
            int americanOdds = OddsUtil.decimalToAmerican(bet.getOdds().doubleValue());

            boolean isFavorite = americanOdds < 0;
            boolean betWon = bet.getStatus() == BetStatus.WON;

            if (isFavorite && betWon) {
                totalWins++;
                double decimalOdds = OddsUtil.americanToDecimal(americanOdds);
                BigDecimal profit = DEFAULT_STAKE.multiply(BigDecimal.valueOf(decimalOdds - 1));
                totalPnL = totalPnL.add(profit);
            } else if (isFavorite) {
                totalPnL = totalPnL.subtract(DEFAULT_STAKE);
            } else if (betWon) {
                totalPnL = totalPnL.subtract(DEFAULT_STAKE);
            } else {
                totalWins++;
                double decimalOdds = OddsUtil.americanToDecimal(americanOdds);
                BigDecimal profit = DEFAULT_STAKE.multiply(BigDecimal.valueOf(decimalOdds - 1));
                totalPnL = totalPnL.add(profit);
            }
        }

        double roi = totalWagered.compareTo(BigDecimal.ZERO) > 0
                ? totalPnL.divide(totalWagered, 4, RoundingMode.HALF_UP).doubleValue() * 100
                : 0;

        return new StrategyMetrics(
                "Always Favorites",
                bets.isEmpty() ? 0 : (double) totalWins / bets.size() * 100,
                totalPnL.setScale(SCALE, RoundingMode.HALF_UP),
                roi,
                BigDecimal.ZERO,
                bets.size());
    }
}
