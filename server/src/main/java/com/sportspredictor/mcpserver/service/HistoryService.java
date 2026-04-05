package com.sportspredictor.mcpserver.service;

import com.sportspredictor.mcpserver.entity.Bankroll;
import com.sportspredictor.mcpserver.entity.Bet;
import com.sportspredictor.mcpserver.entity.BetLeg;
import com.sportspredictor.mcpserver.entity.enums.BetStatus;
import com.sportspredictor.mcpserver.repository.BetLegRepository;
import com.sportspredictor.mcpserver.repository.BetRepository;
import com.sportspredictor.mcpserver.util.OddsUtil;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/** Provides bet history, slip details, analytics, and daily performance views. */
@Service
@RequiredArgsConstructor
@Slf4j
public class HistoryService {

    private static final int SCALE = 2;
    private static final BigDecimal HUNDRED = new BigDecimal("100");

    private final BetRepository betRepository;
    private final BetLegRepository betLegRepository;
    private final BankrollService bankrollService;

    /** A summary of a single bet for list views. */
    public record BetSummary(
            String betId,
            String sport,
            String betType,
            String status,
            BigDecimal stake,
            BigDecimal odds,
            BigDecimal potentialPayout,
            BigDecimal actualPayout,
            String description,
            Instant placedAt,
            Instant settledAt) {}

    /** Filtered bet history result. */
    public record BetHistoryResult(List<BetSummary> bets, int totalCount, String filters, String summary) {}

    /** Detail of a single leg. */
    public record LegDetail(
            int legNumber,
            String selection,
            BigDecimal odds,
            String status,
            String eventId,
            String sport,
            String resultDetail) {}

    /** Full bet slip with all legs. */
    public record BetSlipResult(
            String betId,
            String sport,
            String betType,
            String status,
            BigDecimal stake,
            BigDecimal odds,
            BigDecimal potentialPayout,
            BigDecimal actualPayout,
            String eventId,
            String description,
            Instant placedAt,
            Instant settledAt,
            String metadata,
            List<LegDetail> legs,
            String summary) {}

    /** Bankroll analytics result. */
    public record BankrollAnalyticsResult(
            Map<String, BigDecimal> profitBySport,
            Map<String, BigDecimal> profitByBetType,
            Map<String, BigDecimal> profitByMonth,
            BigDecimal avgOdds,
            double winRate,
            BigDecimal maxDrawdown,
            int totalBets,
            int wins,
            int losses,
            String summary) {}

    /** Today's performance card. */
    public record DailyPerformanceResult(
            String date,
            int placed,
            int pending,
            int settled,
            int wins,
            int losses,
            int pushes,
            BigDecimal dailyProfitLoss,
            BigDecimal currentBalance,
            List<BetSummary> todaysBets,
            String summary) {}

    /** Returns filtered bet history for the active bankroll. */
    public BetHistoryResult getBetHistory(
            String sport,
            String betType,
            String status,
            String startDate,
            String endDate,
            BigDecimal minOdds,
            BigDecimal maxOdds,
            BigDecimal minStake,
            BigDecimal maxStake) {

        Bankroll bankroll = bankrollService.getActiveBankroll();
        List<Bet> bets = betRepository.findByBankrollId(bankroll.getId());

        var filtered = bets.stream()
                .filter(b -> sport == null || sport.isBlank() || b.getSport().equalsIgnoreCase(sport))
                .filter(b -> betType == null
                        || betType.isBlank()
                        || b.getBetType().name().equalsIgnoreCase(betType))
                .filter(b -> status == null
                        || status.isBlank()
                        || b.getStatus().name().equalsIgnoreCase(status))
                .filter(b -> startDate == null
                        || startDate.isBlank()
                        || !b.getPlacedAt().isBefore(parseDate(startDate)))
                .filter(b ->
                        endDate == null || endDate.isBlank() || !b.getPlacedAt().isAfter(parseEndOfDay(endDate)))
                .filter(b -> minOdds == null || b.getOdds().compareTo(minOdds) >= 0)
                .filter(b -> maxOdds == null || b.getOdds().compareTo(maxOdds) <= 0)
                .filter(b -> minStake == null || b.getStake().compareTo(minStake) >= 0)
                .filter(b -> maxStake == null || b.getStake().compareTo(maxStake) <= 0)
                .sorted(Comparator.comparing(Bet::getPlacedAt).reversed())
                .map(this::toBetSummary)
                .toList();

        String filterDesc = buildFilterDescription(sport, betType, status, startDate, endDate);
        String summary =
                String.format("%d bets found%s", filtered.size(), filterDesc.isEmpty() ? "" : " (" + filterDesc + ")");

        return new BetHistoryResult(filtered, filtered.size(), filterDesc, summary);
    }

    /** Returns full details of a single bet including all parlay legs. */
    public BetSlipResult getBetSlip(String betId) {
        Bet bet = betRepository
                .findById(betId)
                .orElseThrow(() -> new IllegalArgumentException("Bet not found: " + betId));

        List<LegDetail> legs = betLegRepository.findByBetId(betId).stream()
                .sorted(Comparator.comparingInt(BetLeg::getLegNumber))
                .map(leg -> new LegDetail(
                        leg.getLegNumber(),
                        leg.getSelection(),
                        leg.getOdds(),
                        leg.getStatus().name(),
                        leg.getEventId(),
                        leg.getSport(),
                        leg.getResultDetail()))
                .toList();

        String summary = String.format(
                "%s bet on %s — %s ($%s at %s odds)",
                bet.getBetType().name(),
                bet.getDescription(),
                bet.getStatus().name(),
                bet.getStake().setScale(SCALE, RoundingMode.HALF_UP),
                formatOdds(bet.getOdds()));

        return new BetSlipResult(
                bet.getId(),
                bet.getSport(),
                bet.getBetType().name(),
                bet.getStatus().name(),
                bet.getStake(),
                bet.getOdds(),
                bet.getPotentialPayout(),
                bet.getActualPayout(),
                bet.getEventId(),
                bet.getDescription(),
                bet.getPlacedAt(),
                bet.getSettledAt(),
                bet.getMetadata(),
                legs,
                summary);
    }

    /** Returns advanced analytics for the active bankroll. */
    public BankrollAnalyticsResult getBankrollAnalytics() {
        Bankroll bankroll = bankrollService.getActiveBankroll();
        List<Bet> allBets = betRepository.findByBankrollId(bankroll.getId());

        List<Bet> settled = allBets.stream()
                .filter(b -> b.getStatus() == BetStatus.WON || b.getStatus() == BetStatus.LOST)
                .toList();

        Map<String, BigDecimal> profitBySport = computeProfitBy(settled, Bet::getSport);
        Map<String, BigDecimal> profitByBetType =
                computeProfitBy(settled, b -> b.getBetType().name());
        Map<String, BigDecimal> profitByMonth = computeProfitBy(settled, b -> b.getPlacedAt()
                .atOffset(ZoneOffset.UTC)
                .toLocalDate()
                .withDayOfMonth(1)
                .toString());

        int wins = (int)
                settled.stream().filter(b -> b.getStatus() == BetStatus.WON).count();
        int losses = settled.size() - wins;
        double winRate = settled.isEmpty() ? 0.0 : (double) wins / settled.size();

        BigDecimal avgOdds = settled.isEmpty()
                ? BigDecimal.ZERO
                : settled.stream()
                        .map(Bet::getOdds)
                        .reduce(BigDecimal.ZERO, BigDecimal::add)
                        .divide(BigDecimal.valueOf(settled.size()), SCALE, RoundingMode.HALF_UP);

        BigDecimal maxDrawdown = computeMaxDrawdown(allBets);

        String summary = String.format(
                "Analytics: %d bets (%d-%d, %.1f%% win rate). Avg odds: %s. Max drawdown: $%s",
                settled.size(), wins, losses, winRate * HUNDRED.doubleValue(), avgOdds, maxDrawdown);

        return new BankrollAnalyticsResult(
                profitBySport,
                profitByBetType,
                profitByMonth,
                avgOdds,
                winRate,
                maxDrawdown,
                settled.size(),
                wins,
                losses,
                summary);
    }

    /** Returns today's betting performance card. */
    public DailyPerformanceResult getDailyPerformance() {
        Bankroll bankroll = bankrollService.getActiveBankroll();
        LocalDate today = LocalDate.now(ZoneOffset.UTC);
        Instant dayStart = today.atStartOfDay(ZoneOffset.UTC).toInstant();
        Instant dayEnd = today.plusDays(1).atStartOfDay(ZoneOffset.UTC).toInstant();

        List<Bet> todaysBets = betRepository.findByPlacedAtBetween(dayStart, dayEnd).stream()
                .filter(b -> b.getBankroll().getId().equals(bankroll.getId()))
                .toList();

        int pending = 0;
        int wins = 0;
        int losses = 0;
        int pushes = 0;
        BigDecimal dailyPl = BigDecimal.ZERO;

        for (Bet bet : todaysBets) {
            switch (bet.getStatus()) {
                case PENDING -> pending++;
                case WON -> {
                    wins++;
                    BigDecimal payout = bet.getActualPayout() != null ? bet.getActualPayout() : BigDecimal.ZERO;
                    dailyPl = dailyPl.add(payout.subtract(bet.getStake()));
                }
                case LOST -> {
                    losses++;
                    dailyPl = dailyPl.subtract(bet.getStake());
                }
                case PUSHED -> pushes++;
                default -> {
                    /* CANCELLED, VOID */
                }
            }
        }

        int settled = wins + losses + pushes;
        List<BetSummary> summaries = todaysBets.stream()
                .sorted(Comparator.comparing(Bet::getPlacedAt).reversed())
                .map(this::toBetSummary)
                .toList();

        String summary = String.format(
                "Today (%s): %d placed, %d pending, %d settled (%d-%d-%d). Daily P/L: $%s",
                today,
                todaysBets.size(),
                pending,
                settled,
                wins,
                losses,
                pushes,
                dailyPl.setScale(SCALE, RoundingMode.HALF_UP));

        return new DailyPerformanceResult(
                today.toString(),
                todaysBets.size(),
                pending,
                settled,
                wins,
                losses,
                pushes,
                dailyPl.setScale(SCALE, RoundingMode.HALF_UP),
                bankroll.getCurrentBalance(),
                summaries,
                summary);
    }

    private BetSummary toBetSummary(Bet bet) {
        return new BetSummary(
                bet.getId(),
                bet.getSport(),
                bet.getBetType().name(),
                bet.getStatus().name(),
                bet.getStake(),
                bet.getOdds(),
                bet.getPotentialPayout(),
                bet.getActualPayout(),
                bet.getDescription(),
                bet.getPlacedAt(),
                bet.getSettledAt());
    }

    private Map<String, BigDecimal> computeProfitBy(
            List<Bet> bets, java.util.function.Function<Bet, String> keyExtractor) {
        return bets.stream()
                .collect(Collectors.groupingBy(
                        keyExtractor,
                        LinkedHashMap::new,
                        Collectors.reducing(
                                BigDecimal.ZERO,
                                b -> {
                                    BigDecimal payout =
                                            b.getActualPayout() != null ? b.getActualPayout() : BigDecimal.ZERO;
                                    return b.getStatus() == BetStatus.WON
                                            ? payout.subtract(b.getStake())
                                            : b.getStake().negate();
                                },
                                BigDecimal::add)));
    }

    private BigDecimal computeMaxDrawdown(List<Bet> bets) {
        List<Bet> settled = bets.stream()
                .filter(b -> b.getSettledAt() != null)
                .sorted(Comparator.comparing(Bet::getSettledAt))
                .toList();

        BigDecimal runningPl = BigDecimal.ZERO;
        BigDecimal peak = BigDecimal.ZERO;
        BigDecimal maxDrawdown = BigDecimal.ZERO;

        for (Bet bet : settled) {
            BigDecimal payout = bet.getActualPayout() != null ? bet.getActualPayout() : BigDecimal.ZERO;
            if (bet.getStatus() == BetStatus.WON) {
                runningPl = runningPl.add(payout.subtract(bet.getStake()));
            } else if (bet.getStatus() == BetStatus.LOST) {
                runningPl = runningPl.subtract(bet.getStake());
            }

            if (runningPl.compareTo(peak) > 0) {
                peak = runningPl;
            }
            BigDecimal drawdown = peak.subtract(runningPl);
            if (drawdown.compareTo(maxDrawdown) > 0) {
                maxDrawdown = drawdown;
            }
        }

        return maxDrawdown.setScale(SCALE, RoundingMode.HALF_UP);
    }

    private static Instant parseDate(String date) {
        return LocalDate.parse(date).atStartOfDay(ZoneOffset.UTC).toInstant();
    }

    private static Instant parseEndOfDay(String date) {
        return LocalDate.parse(date).plusDays(1).atStartOfDay(ZoneOffset.UTC).toInstant();
    }

    private static String formatOdds(BigDecimal decimalOdds) {
        int american = OddsUtil.decimalToAmerican(decimalOdds.doubleValue());
        return american > 0 ? "+" + american : String.valueOf(american);
    }

    private static String buildFilterDescription(
            String sport, String betType, String status, String startDate, String endDate) {
        StringBuilder sb = new StringBuilder();
        if (sport != null && !sport.isBlank()) {
            sb.append("sport=").append(sport);
        }
        if (betType != null && !betType.isBlank()) {
            if (!sb.isEmpty()) {
                sb.append(", ");
            }
            sb.append("type=").append(betType);
        }
        if (status != null && !status.isBlank()) {
            if (!sb.isEmpty()) {
                sb.append(", ");
            }
            sb.append("status=").append(status);
        }
        if (startDate != null && !startDate.isBlank()) {
            if (!sb.isEmpty()) {
                sb.append(", ");
            }
            sb.append("from=").append(startDate);
        }
        if (endDate != null && !endDate.isBlank()) {
            if (!sb.isEmpty()) {
                sb.append(", ");
            }
            sb.append("to=").append(endDate);
        }
        return sb.toString();
    }
}
