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
import com.sportspredictor.util.OddsUtil;
import com.sportspredictor.util.PayoutCalculator;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/** Handles bet placement (single and parlay) and bet cancellation. */
@Service
@RequiredArgsConstructor
@Slf4j
public class BettingService {

    private static final int SCALE = 2;

    private final BetRepository betRepository;
    private final BetLegRepository betLegRepository;
    private final BankrollService bankrollService;
    private final BankrollRepository bankrollRepository;
    private final BankrollTransactionRepository transactionRepository;

    /** Result of placing a single bet. */
    public record PlaceBetResult(
            String betId,
            String sport,
            String eventId,
            String betType,
            String selection,
            String description,
            BigDecimal stake,
            int americanOdds,
            BigDecimal decimalOdds,
            BigDecimal potentialPayout,
            BigDecimal balanceAfter,
            String summary) {}

    /** Input for a single parlay leg. */
    public record ParlayLegInput(String sport, String eventId, String selection, int americanOdds) {}

    /** Summary of a placed parlay leg. */
    public record LegSummary(int legNumber, String sport, String eventId, String selection, int americanOdds) {}

    /** Result of placing a parlay bet. */
    public record PlaceParlayResult(
            String betId,
            BigDecimal stake,
            BigDecimal combinedDecimalOdds,
            int combinedAmericanOdds,
            BigDecimal potentialPayout,
            List<LegSummary> legs,
            BigDecimal balanceAfter,
            String summary) {}

    /** Result of cancelling a bet. */
    public record CancelBetResult(String betId, BigDecimal refundedStake, BigDecimal balanceAfter, String summary) {}

    /** Places a single bet, deducting the stake from the active bankroll. */
    public PlaceBetResult placeBet(
            String sport,
            String eventId,
            String betTypeStr,
            String selection,
            int americanOdds,
            BigDecimal stake,
            String description,
            String metadata) {

        validatePositiveStake(stake);
        BetType betType = parseBetType(betTypeStr);
        Bankroll bankroll = bankrollService.getActiveBankroll();
        validateSufficientBalance(stake, bankroll);

        double decimalOdds = OddsUtil.americanToDecimal(americanOdds);
        BigDecimal potentialPayout = PayoutCalculator.totalReturn(stake, americanOdds);

        Bet bet = Bet.builder()
                .bankroll(bankroll)
                .betType(betType)
                .status(BetStatus.PENDING)
                .stake(stake.setScale(SCALE, RoundingMode.HALF_UP))
                .odds(BigDecimal.valueOf(decimalOdds).setScale(SCALE + 1, RoundingMode.HALF_UP))
                .potentialPayout(potentialPayout)
                .sport(sport)
                .eventId(eventId)
                .description(description != null ? description : selection)
                .placedAt(Instant.now())
                .metadata(metadata)
                .build();
        betRepository.save(bet);

        BigDecimal newBalance = deductStake(bankroll, stake, bet.getId());

        String summary = String.format(
                "Placed %s bet: %s at %s odds ($%s to win $%s). Balance: $%s",
                betType.name(),
                selection,
                formatAmericanOdds(americanOdds),
                stake.setScale(SCALE, RoundingMode.HALF_UP),
                potentialPayout.subtract(stake).setScale(SCALE, RoundingMode.HALF_UP),
                newBalance.setScale(SCALE, RoundingMode.HALF_UP));

        log.info("Bet placed bet_id={} sport={} type={} stake={}", bet.getId(), sport, betType, stake);

        return new PlaceBetResult(
                bet.getId(),
                sport,
                eventId,
                betType.name(),
                selection,
                bet.getDescription(),
                stake,
                americanOdds,
                BigDecimal.valueOf(decimalOdds),
                potentialPayout,
                newBalance,
                summary);
    }

    /** Places a parlay bet with multiple legs, deducting the stake from the active bankroll. */
    public PlaceParlayResult placeParlayBet(
            List<ParlayLegInput> legs, BigDecimal stake, String description, String metadata) {

        if (legs == null || legs.size() < PayoutCalculator.MIN_PARLAY_LEGS) {
            throw new IllegalArgumentException(
                    "Parlay requires at least " + PayoutCalculator.MIN_PARLAY_LEGS + " legs");
        }
        validatePositiveStake(stake);
        Bankroll bankroll = bankrollService.getActiveBankroll();
        validateSufficientBalance(stake, bankroll);

        List<Integer> americanOddsList =
                legs.stream().map(ParlayLegInput::americanOdds).toList();
        BigDecimal parlayProfit = PayoutCalculator.parlayPayout(stake, americanOddsList);
        BigDecimal potentialPayout = parlayProfit.add(stake);

        double combinedDecimal = 1.0;
        for (ParlayLegInput leg : legs) {
            combinedDecimal *= OddsUtil.americanToDecimal(leg.americanOdds());
        }
        int combinedAmerican = OddsUtil.decimalToAmerican(combinedDecimal);

        String parlayDescription = description != null ? description : legs.size() + "-leg parlay";

        Bet bet = Bet.builder()
                .bankroll(bankroll)
                .betType(BetType.PARLAY)
                .status(BetStatus.PENDING)
                .stake(stake.setScale(SCALE, RoundingMode.HALF_UP))
                .odds(BigDecimal.valueOf(combinedDecimal).setScale(SCALE + 1, RoundingMode.HALF_UP))
                .potentialPayout(potentialPayout)
                .sport(legs.getFirst().sport())
                .eventId(legs.getFirst().eventId())
                .description(parlayDescription)
                .placedAt(Instant.now())
                .metadata(metadata)
                .build();
        betRepository.save(bet);

        List<LegSummary> legSummaries = new ArrayList<>();
        for (int i = 0; i < legs.size(); i++) {
            ParlayLegInput legInput = legs.get(i);
            BetLeg betLeg = BetLeg.builder()
                    .bet(bet)
                    .legNumber(i + 1)
                    .selection(legInput.selection())
                    .odds(BigDecimal.valueOf(OddsUtil.americanToDecimal(legInput.americanOdds()))
                            .setScale(SCALE + 1, RoundingMode.HALF_UP))
                    .status(BetLegStatus.PENDING)
                    .eventId(legInput.eventId())
                    .sport(legInput.sport())
                    .build();
            betLegRepository.save(betLeg);
            legSummaries.add(new LegSummary(
                    i + 1, legInput.sport(), legInput.eventId(), legInput.selection(), legInput.americanOdds()));
        }

        BigDecimal newBalance = deductStake(bankroll, stake, bet.getId());

        String summary = String.format(
                "Placed %d-leg parlay at %s combined odds ($%s to win $%s). Balance: $%s",
                legs.size(),
                formatAmericanOdds(combinedAmerican),
                stake.setScale(SCALE, RoundingMode.HALF_UP),
                parlayProfit.setScale(SCALE, RoundingMode.HALF_UP),
                newBalance.setScale(SCALE, RoundingMode.HALF_UP));

        log.info("Parlay placed bet_id={} legs={} stake={}", bet.getId(), legs.size(), stake);

        return new PlaceParlayResult(
                bet.getId(),
                stake,
                BigDecimal.valueOf(combinedDecimal),
                combinedAmerican,
                potentialPayout,
                legSummaries,
                newBalance,
                summary);
    }

    /** Cancels a pending bet and refunds the stake to the bankroll. */
    public CancelBetResult cancelBet(String betId) {
        Bet bet = betRepository
                .findById(betId)
                .orElseThrow(() -> new IllegalArgumentException("Bet not found: " + betId));

        if (bet.getStatus() != BetStatus.PENDING) {
            throw new IllegalStateException("Cannot cancel bet with status "
                    + bet.getStatus().name() + " — only PENDING bets can be cancelled");
        }

        bet.setStatus(BetStatus.CANCELLED);
        betRepository.save(bet);

        Bankroll bankroll = bet.getBankroll();
        BigDecimal newBalance = bankroll.getCurrentBalance().add(bet.getStake());
        bankroll.setCurrentBalance(newBalance);
        bankrollRepository.save(bankroll);

        BankrollTransaction txn = BankrollTransaction.builder()
                .bankroll(bankroll)
                .type(TransactionType.BET_CANCELLED)
                .amount(bet.getStake())
                .balanceAfter(newBalance.setScale(SCALE, RoundingMode.HALF_UP))
                .referenceBetId(betId)
                .createdAt(Instant.now())
                .build();
        transactionRepository.save(txn);

        String summary = String.format(
                "Cancelled bet %s. Refunded $%s. Balance: $%s",
                betId,
                bet.getStake().setScale(SCALE, RoundingMode.HALF_UP),
                newBalance.setScale(SCALE, RoundingMode.HALF_UP));

        log.info("Bet cancelled bet_id={} refund={}", betId, bet.getStake());

        return new CancelBetResult(betId, bet.getStake(), newBalance, summary);
    }

    private BigDecimal deductStake(Bankroll bankroll, BigDecimal stake, String betId) {
        BigDecimal newBalance = bankroll.getCurrentBalance().subtract(stake);
        bankroll.setCurrentBalance(newBalance);
        bankrollRepository.save(bankroll);

        BankrollTransaction txn = BankrollTransaction.builder()
                .bankroll(bankroll)
                .type(TransactionType.BET_PLACED)
                .amount(stake.setScale(SCALE, RoundingMode.HALF_UP))
                .balanceAfter(newBalance.setScale(SCALE, RoundingMode.HALF_UP))
                .referenceBetId(betId)
                .createdAt(Instant.now())
                .build();
        transactionRepository.save(txn);

        return newBalance;
    }

    private static void validatePositiveStake(BigDecimal stake) {
        if (stake == null || stake.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Stake must be positive, got: " + stake);
        }
    }

    private static void validateSufficientBalance(BigDecimal stake, Bankroll bankroll) {
        if (stake.compareTo(bankroll.getCurrentBalance()) > 0) {
            throw new IllegalArgumentException(String.format(
                    "Insufficient balance: stake $%s exceeds balance $%s",
                    stake.setScale(SCALE, RoundingMode.HALF_UP),
                    bankroll.getCurrentBalance().setScale(SCALE, RoundingMode.HALF_UP)));
        }
    }

    private static BetType parseBetType(String betTypeStr) {
        try {
            return BetType.valueOf(betTypeStr.toUpperCase(java.util.Locale.ROOT));
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Invalid bet type: " + betTypeStr + ". Valid types: "
                    + java.util.Arrays.toString(BetType.values()));
        }
    }

    private static String formatAmericanOdds(int odds) {
        return odds > 0 ? "+" + odds : String.valueOf(odds);
    }
}
