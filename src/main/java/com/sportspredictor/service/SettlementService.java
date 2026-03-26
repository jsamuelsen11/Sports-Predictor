package com.sportspredictor.service;

import com.sportspredictor.entity.Bankroll;
import com.sportspredictor.entity.BankrollTransaction;
import com.sportspredictor.entity.Bet;
import com.sportspredictor.entity.BetLeg;
import com.sportspredictor.entity.enums.BetLegStatus;
import com.sportspredictor.entity.enums.BetStatus;
import com.sportspredictor.entity.enums.BetType;
import com.sportspredictor.entity.enums.TransactionType;
import com.sportspredictor.repository.BankrollRepository;
import com.sportspredictor.repository.BankrollTransactionRepository;
import com.sportspredictor.repository.BetLegRepository;
import com.sportspredictor.repository.BetRepository;
import com.sportspredictor.service.ResultsService.GameResult;
import com.sportspredictor.service.ResultsService.TeamResult;
import com.sportspredictor.util.OddsUtil;
import com.sportspredictor.util.PayoutCalculator;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Handles bet settlement: single bets, parlays, and auto-settle via game results. */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class SettlementService {

    private static final int SCALE = 2;

    private final BetRepository betRepository;
    private final BetLegRepository betLegRepository;
    private final BankrollRepository bankrollRepository;
    private final BankrollTransactionRepository transactionRepository;
    private final ResultsService resultsService;

    /** Result of settling a single bet. */
    public record SettleBetResult(
            String betId,
            String previousStatus,
            String newStatus,
            BigDecimal stake,
            BigDecimal payout,
            BigDecimal balanceAfter,
            String summary) {}

    /** Input for settling a single parlay leg. */
    public record LegSettlement(int legNumber, String outcome, String resultDetail) {}

    /** Detail of a settled leg. */
    public record LegSettlementDetail(
            int legNumber, String selection, String previousStatus, String newStatus, String resultDetail) {}

    /** Result of settling a parlay. */
    public record SettleParlayResult(
            String betId,
            String newStatus,
            BigDecimal stake,
            BigDecimal payout,
            List<LegSettlementDetail> legs,
            BigDecimal balanceAfter,
            String summary) {}

    /** A single auto-settled bet summary. */
    public record AutoSettledBet(
            String betId, String eventId, String sport, String outcome, BigDecimal payout, String description) {}

    /** Result of auto-settling pending bets. */
    public record AutoSettleResult(
            int totalPending,
            int matched,
            int settled,
            int won,
            int lost,
            int pushed,
            int errors,
            List<AutoSettledBet> settledBets,
            String summary) {}

    /** Settles a single bet as WON, LOST, or PUSH. */
    public SettleBetResult settleBet(String betId, String outcome) {
        Bet bet = betRepository
                .findById(betId)
                .orElseThrow(() -> new IllegalArgumentException("Bet not found: " + betId));

        if (bet.getStatus() != BetStatus.PENDING) {
            throw new IllegalStateException(
                    "Cannot settle bet with status " + bet.getStatus().name());
        }

        String normalizedOutcome = normalizeOutcome(outcome);
        BetStatus newStatus = outcomeToStatus(normalizedOutcome);

        int americanOdds = OddsUtil.decimalToAmerican(bet.getOdds().doubleValue());
        BigDecimal settlementPayout = PayoutCalculator.settleBet(bet.getStake(), americanOdds, normalizedOutcome);

        BigDecimal actualPayout;
        BigDecimal creditAmount;
        TransactionType txnType;

        switch (newStatus) {
            case WON -> {
                actualPayout = settlementPayout.add(bet.getStake());
                creditAmount = actualPayout;
                txnType = TransactionType.BET_WON;
            }
            case LOST -> {
                actualPayout = BigDecimal.ZERO.setScale(SCALE, RoundingMode.HALF_UP);
                creditAmount = BigDecimal.ZERO;
                txnType = TransactionType.BET_LOST;
            }
            case PUSHED -> {
                actualPayout = bet.getStake();
                creditAmount = bet.getStake();
                txnType = TransactionType.BET_PUSH;
            }
            default -> throw new IllegalStateException("Unexpected status: " + newStatus);
        }

        bet.setStatus(newStatus);
        bet.setActualPayout(actualPayout.setScale(SCALE, RoundingMode.HALF_UP));
        bet.setSettledAt(Instant.now());
        betRepository.save(bet);

        Bankroll bankroll = bet.getBankroll();
        BigDecimal newBalance = bankroll.getCurrentBalance().add(creditAmount);
        bankroll.setCurrentBalance(newBalance);
        bankrollRepository.save(bankroll);

        BankrollTransaction txn = BankrollTransaction.builder()
                .bankroll(bankroll)
                .type(txnType)
                .amount(creditAmount.setScale(SCALE, RoundingMode.HALF_UP))
                .balanceAfter(newBalance.setScale(SCALE, RoundingMode.HALF_UP))
                .referenceBetId(betId)
                .createdAt(Instant.now())
                .build();
        transactionRepository.save(txn);

        String summary = String.format(
                "Settled bet %s as %s. Payout: $%s. Balance: $%s",
                betId,
                newStatus.name(),
                actualPayout.setScale(SCALE, RoundingMode.HALF_UP),
                newBalance.setScale(SCALE, RoundingMode.HALF_UP));

        log.info("Bet settled bet_id={} outcome={} payout={}", betId, newStatus, actualPayout);

        return new SettleBetResult(
                betId, "PENDING", newStatus.name(), bet.getStake(), actualPayout, newBalance, summary);
    }

    /** Settles a parlay by evaluating each leg's outcome. */
    public SettleParlayResult settleParlay(String betId, List<LegSettlement> legOutcomes) {
        Bet bet = betRepository
                .findById(betId)
                .orElseThrow(() -> new IllegalArgumentException("Bet not found: " + betId));

        if (bet.getBetType() != BetType.PARLAY) {
            throw new IllegalArgumentException("Bet " + betId + " is not a PARLAY");
        }
        if (bet.getStatus() != BetStatus.PENDING) {
            throw new IllegalStateException(
                    "Cannot settle bet with status " + bet.getStatus().name());
        }

        List<BetLeg> legs = betLegRepository.findByBetId(betId).stream()
                .sorted(Comparator.comparingInt(BetLeg::getLegNumber))
                .toList();

        List<LegSettlementDetail> legDetails = new ArrayList<>();
        boolean anyLost = false;
        boolean allPushed = true;
        double reducedDecimalOdds = 1.0;

        for (BetLeg leg : legs) {
            LegSettlement legOutcome = legOutcomes.stream()
                    .filter(lo -> lo.legNumber() == leg.getLegNumber())
                    .findFirst()
                    .orElseThrow(() -> new IllegalArgumentException("Missing outcome for leg " + leg.getLegNumber()));

            String normalizedOutcome = normalizeOutcome(legOutcome.outcome());
            BetLegStatus legStatus = legOutcomeToStatus(normalizedOutcome);

            final String previousStatus = leg.getStatus().name();
            leg.setStatus(legStatus);
            if (legOutcome.resultDetail() != null) {
                leg.setResultDetail(legOutcome.resultDetail());
            }
            betLegRepository.save(leg);

            legDetails.add(new LegSettlementDetail(
                    leg.getLegNumber(),
                    leg.getSelection(),
                    previousStatus,
                    legStatus.name(),
                    legOutcome.resultDetail()));

            if (legStatus == BetLegStatus.LOST) {
                anyLost = true;
                allPushed = false;
            } else if (legStatus == BetLegStatus.WON) {
                allPushed = false;
                reducedDecimalOdds *= leg.getOdds().doubleValue();
            }
            // PUSHED legs contribute odds of 1.0 (no effect on multiplication)
        }

        BetStatus parlayStatus;
        BigDecimal actualPayout;
        BigDecimal creditAmount;
        TransactionType txnType;

        if (anyLost) {
            parlayStatus = BetStatus.LOST;
            actualPayout = BigDecimal.ZERO.setScale(SCALE, RoundingMode.HALF_UP);
            creditAmount = BigDecimal.ZERO;
            txnType = TransactionType.BET_LOST;
        } else if (allPushed) {
            parlayStatus = BetStatus.PUSHED;
            actualPayout = bet.getStake();
            creditAmount = bet.getStake();
            txnType = TransactionType.BET_PUSH;
        } else {
            parlayStatus = BetStatus.WON;
            actualPayout = bet.getStake()
                    .multiply(BigDecimal.valueOf(reducedDecimalOdds))
                    .setScale(SCALE, RoundingMode.HALF_UP);
            creditAmount = actualPayout;
            txnType = TransactionType.BET_WON;
        }

        bet.setStatus(parlayStatus);
        bet.setActualPayout(actualPayout);
        bet.setSettledAt(Instant.now());
        betRepository.save(bet);

        Bankroll bankroll = bet.getBankroll();
        BigDecimal newBalance = bankroll.getCurrentBalance().add(creditAmount);
        bankroll.setCurrentBalance(newBalance);
        bankrollRepository.save(bankroll);

        BankrollTransaction txn = BankrollTransaction.builder()
                .bankroll(bankroll)
                .type(txnType)
                .amount(creditAmount.setScale(SCALE, RoundingMode.HALF_UP))
                .balanceAfter(newBalance.setScale(SCALE, RoundingMode.HALF_UP))
                .referenceBetId(betId)
                .createdAt(Instant.now())
                .build();
        transactionRepository.save(txn);

        String summary = String.format(
                "Settled %d-leg parlay as %s. Payout: $%s. Balance: $%s",
                legs.size(),
                parlayStatus.name(),
                actualPayout.setScale(SCALE, RoundingMode.HALF_UP),
                newBalance.setScale(SCALE, RoundingMode.HALF_UP));

        log.info("Parlay settled bet_id={} status={} payout={}", betId, parlayStatus, actualPayout);

        return new SettleParlayResult(
                betId, parlayStatus.name(), bet.getStake(), actualPayout, legDetails, newBalance, summary);
    }

    /** Auto-settles all pending bets whose games have completed. */
    public AutoSettleResult autoSettleBets(String sport) {
        List<Bet> pendingBets;
        if (sport != null && !sport.isBlank()) {
            pendingBets = betRepository.findByStatus(BetStatus.PENDING).stream()
                    .filter(b -> b.getSport().equalsIgnoreCase(sport))
                    .toList();
        } else {
            pendingBets = betRepository.findByStatus(BetStatus.PENDING);
        }

        Map<EventKey, List<Bet>> betsByEvent = pendingBets.stream()
                .filter(b -> b.getBetType() != BetType.PARLAY)
                .collect(Collectors.groupingBy(b -> new EventKey(b.getSport(), b.getEventId())));

        List<AutoSettledBet> settledBets = new ArrayList<>();
        int errors = 0;
        int won = 0;
        int lost = 0;
        int pushed = 0;

        for (Map.Entry<EventKey, List<Bet>> entry : betsByEvent.entrySet()) {
            String betSport = entry.getKey().sport();
            String eventId = entry.getKey().eventId();
            List<Bet> eventBets = entry.getValue();

            try {
                var results = resultsService.getGameResults(betSport, null, eventId);
                if (results.results().isEmpty()) {
                    continue;
                }

                GameResult gameResult = results.results().getFirst();
                for (Bet bet : eventBets) {
                    String outcome = determineOutcome(bet, gameResult);
                    if (outcome == null) {
                        log.warn("Could not determine outcome for bet_id={} event_id={}", bet.getId(), eventId);
                        errors++;
                        continue;
                    }

                    try {
                        SettleBetResult result = settleBet(bet.getId(), outcome);
                        settledBets.add(new AutoSettledBet(
                                bet.getId(),
                                eventId,
                                betSport,
                                result.newStatus(),
                                result.payout(),
                                bet.getDescription()));

                        switch (BetStatus.valueOf(result.newStatus())) {
                            case WON -> won++;
                            case LOST -> lost++;
                            case PUSHED -> pushed++;
                            default -> {
                                /* unexpected */
                            }
                        }
                    } catch (Exception e) {
                        log.warn("Error settling bet_id={}: {}", bet.getId(), e.getMessage());
                        errors++;
                    }
                }
            } catch (Exception e) {
                log.warn("Error fetching results for {}/{}: {}", betSport, eventId, e.getMessage());
                errors++;
            }
        }

        int settled = won + lost + pushed;
        String summary = String.format(
                "Auto-settle: %d pending, %d matched, %d settled (%d won, %d lost, %d pushed), %d errors",
                pendingBets.size(),
                betsByEvent.values().stream().mapToInt(List::size).sum(),
                settled,
                won,
                lost,
                pushed,
                errors);

        log.info(summary);

        return new AutoSettleResult(
                pendingBets.size(),
                betsByEvent.values().stream().mapToInt(List::size).sum(),
                settled,
                won,
                lost,
                pushed,
                errors,
                settledBets,
                summary);
    }

    private String determineOutcome(Bet bet, GameResult gameResult) {
        String selection = bet.getDescription().toLowerCase(Locale.ROOT);

        for (TeamResult team : gameResult.teams()) {
            String teamName = team.displayName().toLowerCase(Locale.ROOT);
            String abbr = team.abbreviation().toLowerCase(Locale.ROOT);

            if (selection.contains(teamName) || selection.contains(abbr)) {
                return team.winner() ? "WON" : "LOST";
            }
        }

        return null;
    }

    private static String normalizeOutcome(String outcome) {
        String upper = outcome.toUpperCase(Locale.ROOT).trim();
        return switch (upper) {
            case "WON", "WIN", "W" -> "WON";
            case "LOST", "LOSS", "L" -> "LOST";
            case "PUSHED", "PUSH", "P", "TIE", "DRAW" -> "PUSHED";
            default -> throw new IllegalArgumentException("Invalid outcome: " + outcome + ". Use WON, LOST, or PUSH");
        };
    }

    private static BetStatus outcomeToStatus(String normalizedOutcome) {
        return switch (normalizedOutcome) {
            case "WON" -> BetStatus.WON;
            case "LOST" -> BetStatus.LOST;
            case "PUSHED" -> BetStatus.PUSHED;
            default -> throw new IllegalArgumentException("Unexpected outcome: " + normalizedOutcome);
        };
    }

    private static BetLegStatus legOutcomeToStatus(String normalizedOutcome) {
        return switch (normalizedOutcome) {
            case "WON" -> BetLegStatus.WON;
            case "LOST" -> BetLegStatus.LOST;
            case "PUSHED" -> BetLegStatus.PUSHED;
            default -> throw new IllegalArgumentException("Unexpected outcome: " + normalizedOutcome);
        };
    }

    /** Structured key for grouping bets by sport and event. */
    private record EventKey(String sport, String eventId) {}
}
