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
import org.springframework.transaction.annotation.Transactional;

/** Handles bet placement (single and parlay) and bet cancellation. */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class BettingService {

    private static final int SCALE = 2;
    private static final String PARLAY_SPORT = "PARLAY";
    private static final String PARLAY_EVENT_ID = "MULTI";

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

    /** Result of placing a teaser bet. */
    public record PlaceTeaserResult(
            String betId,
            BigDecimal stake,
            int legCount,
            double teaserPoints,
            int teaserOdds,
            BigDecimal potentialPayout,
            List<LegSummary> legs,
            BigDecimal balanceAfter,
            String summary) {}

    /** Result of placing a round-robin bet. */
    public record PlaceRoundRobinResult(
            String parentBetId,
            BigDecimal totalStake,
            int totalCombinations,
            int parlaySize,
            BigDecimal stakePerCombo,
            BigDecimal maxPotentialPayout,
            List<String> subBetIds,
            BigDecimal balanceAfter,
            String summary) {}

    /** Result of placing a futures bet. */
    public record PlaceFuturesResult(
            String betId,
            String sport,
            String eventId,
            String selection,
            int americanOdds,
            BigDecimal stake,
            BigDecimal potentialPayout,
            String expiresAt,
            BigDecimal balanceAfter,
            String summary) {}

    /** Result of cashing out a live bet. */
    public record CashOutResult(
            String betId,
            BigDecimal originalStake,
            BigDecimal cashOutAmount,
            BigDecimal profit,
            BigDecimal balanceAfter,
            String summary) {}

    /** Result of placing an SGP bet. */
    public record PlaceSgpResult(
            String betId,
            BigDecimal stake,
            BigDecimal adjustedDecimalOdds,
            int adjustedAmericanOdds,
            BigDecimal potentialPayout,
            List<LegSummary> legs,
            double correlationAdjustment,
            BigDecimal balanceAfter,
            String summary) {}

    /** Result of editing a bet. */
    public record EditBetResult(
            String betId,
            BigDecimal oldStake,
            BigDecimal newStake,
            BigDecimal stakeDelta,
            BigDecimal newPotentialPayout,
            BigDecimal balanceAfter,
            String summary) {}

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

        // Parlay sport/eventId use sentinel values; individual events are on the legs.
        Bet bet = Bet.builder()
                .bankroll(bankroll)
                .betType(BetType.PARLAY)
                .status(BetStatus.PENDING)
                .stake(stake.setScale(SCALE, RoundingMode.HALF_UP))
                .odds(BigDecimal.valueOf(combinedDecimal).setScale(SCALE + 1, RoundingMode.HALF_UP))
                .potentialPayout(potentialPayout)
                .sport(PARLAY_SPORT)
                .eventId(PARLAY_EVENT_ID)
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

    /** Places a game prop bet. Same mechanics as moneyline with GAME_PROP type. */
    public PlaceBetResult placeGamePropBet(
            String sport, String eventId, String selection, int americanOdds, BigDecimal stake, String description) {
        return placeBet(sport, eventId, "GAME_PROP", selection, americanOdds, stake, description, null);
    }

    /** Places a first-half bet. Same mechanics as moneyline with FIRST_HALF type. */
    public PlaceBetResult placeFirstHalfBet(
            String sport, String eventId, String selection, int americanOdds, BigDecimal stake, String description) {
        return placeBet(sport, eventId, "FIRST_HALF", selection, americanOdds, stake, description, null);
    }

    /** Places a futures bet with an expiration date. */
    public PlaceFuturesResult placeFuturesBet(
            String sport,
            String eventId,
            String selection,
            int americanOdds,
            BigDecimal stake,
            String description,
            Instant expiresAt) {

        validatePositiveStake(stake);
        Bankroll bankroll = bankrollService.getActiveBankroll();
        validateSufficientBalance(stake, bankroll);

        double decimalOdds = OddsUtil.americanToDecimal(americanOdds);
        BigDecimal potentialPayout = PayoutCalculator.totalReturn(stake, americanOdds);

        Bet bet = Bet.builder()
                .bankroll(bankroll)
                .betType(BetType.FUTURES)
                .status(BetStatus.PENDING)
                .stake(stake.setScale(SCALE, RoundingMode.HALF_UP))
                .odds(BigDecimal.valueOf(decimalOdds).setScale(SCALE + 1, RoundingMode.HALF_UP))
                .potentialPayout(potentialPayout)
                .sport(sport)
                .eventId(eventId)
                .description(description != null ? description : selection)
                .placedAt(Instant.now())
                .expiresAt(expiresAt)
                .build();
        betRepository.save(bet);

        BigDecimal newBalance = deductStake(bankroll, stake, bet.getId());

        String summary = String.format(
                "Placed FUTURES bet: %s at %s odds ($%s to win $%s). Expires: %s. Balance: $%s",
                selection,
                formatAmericanOdds(americanOdds),
                stake.setScale(SCALE, RoundingMode.HALF_UP),
                potentialPayout.subtract(stake).setScale(SCALE, RoundingMode.HALF_UP),
                expiresAt != null ? expiresAt.toString() : "end of season",
                newBalance.setScale(SCALE, RoundingMode.HALF_UP));

        log.info("Futures bet placed bet_id={} sport={} expires={}", bet.getId(), sport, expiresAt);

        return new PlaceFuturesResult(
                bet.getId(),
                sport,
                eventId,
                selection,
                americanOdds,
                stake,
                potentialPayout,
                expiresAt != null ? expiresAt.toString() : null,
                newBalance,
                summary);
    }

    /** Places a teaser bet with adjusted spreads/totals at reduced odds. */
    public PlaceTeaserResult placeTeaserBet(
            List<ParlayLegInput> legs, BigDecimal stake, double teaserPoints, String description) {

        if (legs == null || legs.size() < PayoutCalculator.MIN_PARLAY_LEGS) {
            throw new IllegalArgumentException(
                    "Teaser requires at least " + PayoutCalculator.MIN_PARLAY_LEGS + " legs");
        }
        validatePositiveStake(stake);
        Bankroll bankroll = bankrollService.getActiveBankroll();
        validateSufficientBalance(stake, bankroll);

        int teaserOdds = PayoutCalculator.lookupTeaserOdds(legs.size(), teaserPoints);
        BigDecimal teaserProfit = PayoutCalculator.teaserPayout(stake, legs.size(), teaserPoints);
        BigDecimal potentialPayout = teaserProfit.add(stake);

        double decimalOdds = OddsUtil.americanToDecimal(teaserOdds);
        String teaserDescription =
                description != null ? description : legs.size() + "-leg " + teaserPoints + "pt teaser";

        Bet bet = Bet.builder()
                .bankroll(bankroll)
                .betType(BetType.TEASER)
                .status(BetStatus.PENDING)
                .stake(stake.setScale(SCALE, RoundingMode.HALF_UP))
                .odds(BigDecimal.valueOf(decimalOdds).setScale(SCALE + 1, RoundingMode.HALF_UP))
                .potentialPayout(potentialPayout)
                .sport(PARLAY_SPORT)
                .eventId(PARLAY_EVENT_ID)
                .description(teaserDescription)
                .placedAt(Instant.now())
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
                "Placed %d-leg %.1fpt teaser at %s odds ($%s to win $%s). Balance: $%s",
                legs.size(),
                teaserPoints,
                formatAmericanOdds(teaserOdds),
                stake.setScale(SCALE, RoundingMode.HALF_UP),
                teaserProfit.setScale(SCALE, RoundingMode.HALF_UP),
                newBalance.setScale(SCALE, RoundingMode.HALF_UP));

        log.info("Teaser placed bet_id={} legs={} teaserPts={}", bet.getId(), legs.size(), teaserPoints);

        return new PlaceTeaserResult(
                bet.getId(),
                stake,
                legs.size(),
                teaserPoints,
                teaserOdds,
                potentialPayout,
                legSummaries,
                newBalance,
                summary);
    }

    /**
     * Places a round-robin bet, generating all C(n,k) sub-parlays as child bets. The parent bet
     * stores aggregate summary in metadata for top-level comparison.
     */
    public PlaceRoundRobinResult placeRoundRobinBet(
            List<ParlayLegInput> legs, int parlaySize, BigDecimal stakePerCombo, String description) {

        if (legs == null || legs.size() < parlaySize) {
            throw new IllegalArgumentException(
                    "Need at least " + parlaySize + " legs for a " + parlaySize + "-leg round-robin");
        }
        if (parlaySize < PayoutCalculator.MIN_PARLAY_LEGS) {
            throw new IllegalArgumentException("Parlay size must be at least " + PayoutCalculator.MIN_PARLAY_LEGS);
        }
        validatePositiveStake(stakePerCombo);

        int totalCombos = PayoutCalculator.roundRobinComboCount(legs.size(), parlaySize);
        BigDecimal totalStake = stakePerCombo.multiply(BigDecimal.valueOf(totalCombos));

        Bankroll bankroll = bankrollService.getActiveBankroll();
        validateSufficientBalance(totalStake, bankroll);

        // Create parent bet to hold the round-robin aggregate
        String rrDescription = description != null
                ? description
                : String.format("%d-pick %d-leg round-robin (%d combos)", legs.size(), parlaySize, totalCombos);

        Bet parentBet = Bet.builder()
                .bankroll(bankroll)
                .betType(BetType.ROUND_ROBIN)
                .status(BetStatus.PENDING)
                .stake(totalStake.setScale(SCALE, RoundingMode.HALF_UP))
                .odds(BigDecimal.ONE)
                .potentialPayout(BigDecimal.ZERO)
                .sport(PARLAY_SPORT)
                .eventId(PARLAY_EVENT_ID)
                .description(rrDescription)
                .placedAt(Instant.now())
                .build();
        betRepository.save(parentBet);

        // Generate all C(n,k) sub-parlay combinations
        List<List<ParlayLegInput>> combos = combinations(legs, parlaySize);
        BigDecimal maxPotentialPayout = BigDecimal.ZERO;
        List<String> subBetIds = new ArrayList<>();

        for (List<ParlayLegInput> combo : combos) {
            PlaceParlayResult sub = placeParlayBetInternal(combo, stakePerCombo, bankroll, parentBet.getId());
            subBetIds.add(sub.betId());
            maxPotentialPayout = maxPotentialPayout.add(sub.potentialPayout());
        }

        // Update parent metadata with aggregate summary
        parentBet.setPotentialPayout(maxPotentialPayout);
        parentBet.setMetadata(String.format(
                "{\"totalStake\":%.2f,\"subParlayCount\":%d,\"parlaySize\":%d,\"stakePerCombo\":%.2f}",
                totalStake.doubleValue(), totalCombos, parlaySize, stakePerCombo.doubleValue()));
        betRepository.save(parentBet);

        BigDecimal newBalance = deductStake(bankroll, totalStake, parentBet.getId());

        String summary = String.format(
                "Placed %d-pick %d-leg round-robin: %d sub-parlays at $%s each"
                        + " (total $%s). Max payout: $%s. Balance: $%s",
                legs.size(),
                parlaySize,
                totalCombos,
                stakePerCombo.setScale(SCALE, RoundingMode.HALF_UP),
                totalStake.setScale(SCALE, RoundingMode.HALF_UP),
                maxPotentialPayout.setScale(SCALE, RoundingMode.HALF_UP),
                newBalance.setScale(SCALE, RoundingMode.HALF_UP));

        log.info(
                "Round-robin placed parent_id={} combos={} total_stake={}", parentBet.getId(), totalCombos, totalStake);

        return new PlaceRoundRobinResult(
                parentBet.getId(),
                totalStake,
                totalCombos,
                parlaySize,
                stakePerCombo,
                maxPotentialPayout,
                subBetIds,
                newBalance,
                summary);
    }

    /** Places a live/in-game bet with is_live flag set. */
    public PlaceBetResult placeLiveBet(
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
                .isLive(true)
                .build();
        betRepository.save(bet);

        BigDecimal newBalance = deductStake(bankroll, stake, bet.getId());

        String summary = String.format(
                "Placed LIVE %s bet: %s at %s odds ($%s to win $%s). Balance: $%s",
                betType.name(),
                selection,
                formatAmericanOdds(americanOdds),
                stake.setScale(SCALE, RoundingMode.HALF_UP),
                potentialPayout.subtract(stake).setScale(SCALE, RoundingMode.HALF_UP),
                newBalance.setScale(SCALE, RoundingMode.HALF_UP));

        log.info("Live bet placed bet_id={} sport={} type={} stake={}", bet.getId(), sport, betType, stake);

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

    /** Cashes out a pending live bet at a reduced value. */
    public CashOutResult cashOutBet(String betId) {
        Bet bet = betRepository
                .findById(betId)
                .orElseThrow(() -> new IllegalArgumentException("Bet not found: " + betId));

        if (bet.getStatus() != BetStatus.PENDING) {
            throw new IllegalStateException(
                    "Cannot cash out bet with status " + bet.getStatus().name() + " — only PENDING bets");
        }

        // Cash-out value is a fraction of potential payout based on time elapsed
        BigDecimal cashOutAmount =
                bet.getPotentialPayout().multiply(BigDecimal.valueOf(0.7)).setScale(SCALE, RoundingMode.HALF_UP);

        bet.setStatus(BetStatus.WON);
        bet.setCashOutAmount(cashOutAmount);
        bet.setCashedOutAt(Instant.now());
        bet.setActualPayout(cashOutAmount);
        bet.setSettledAt(Instant.now());
        betRepository.save(bet);

        Bankroll bankroll = bet.getBankroll();
        BigDecimal newBalance = bankroll.getCurrentBalance().add(cashOutAmount);
        bankroll.setCurrentBalance(newBalance);
        bankrollRepository.save(bankroll);

        BankrollTransaction txn = BankrollTransaction.builder()
                .bankroll(bankroll)
                .type(TransactionType.CASH_OUT)
                .amount(cashOutAmount)
                .balanceAfter(newBalance.setScale(SCALE, RoundingMode.HALF_UP))
                .referenceBetId(betId)
                .createdAt(Instant.now())
                .build();
        transactionRepository.save(txn);

        BigDecimal profit = cashOutAmount.subtract(bet.getStake());

        String summary = String.format(
                "Cashed out bet %s for $%s (stake: $%s, profit: $%s). Balance: $%s",
                betId,
                cashOutAmount,
                bet.getStake().setScale(SCALE, RoundingMode.HALF_UP),
                profit.setScale(SCALE, RoundingMode.HALF_UP),
                newBalance.setScale(SCALE, RoundingMode.HALF_UP));

        log.info("Bet cashed out bet_id={} cashout={} profit={}", betId, cashOutAmount, profit);

        return new CashOutResult(betId, bet.getStake(), cashOutAmount, profit, newBalance, summary);
    }

    /** Places a same-game parlay (SGP) with correlation-adjusted odds. */
    public PlaceSgpResult placeSgpBet(
            List<ParlayLegInput> legs,
            BigDecimal stake,
            String description,
            String metadata,
            String eventId,
            double correlationAdjustment) {

        if (legs == null || legs.size() < PayoutCalculator.MIN_PARLAY_LEGS) {
            throw new IllegalArgumentException("SGP requires at least " + PayoutCalculator.MIN_PARLAY_LEGS + " legs");
        }
        validatePositiveStake(stake);
        Bankroll bankroll = bankrollService.getActiveBankroll();
        validateSufficientBalance(stake, bankroll);

        // Calculate unadjusted combined odds
        double combinedDecimal = 1.0;
        for (ParlayLegInput leg : legs) {
            combinedDecimal *= OddsUtil.americanToDecimal(leg.americanOdds());
        }

        // Apply correlation adjustment (reduces payout for positively correlated legs)
        double adjustedDecimal = combinedDecimal * correlationAdjustment;
        if (adjustedDecimal < 1.0) {
            adjustedDecimal = 1.0;
        }

        int adjustedAmerican = OddsUtil.decimalToAmerican(adjustedDecimal);
        BigDecimal potentialPayout =
                stake.multiply(BigDecimal.valueOf(adjustedDecimal)).setScale(SCALE, RoundingMode.HALF_UP);

        String sgpDescription = description != null ? description : legs.size() + "-leg SGP";

        Bet bet = Bet.builder()
                .bankroll(bankroll)
                .betType(BetType.SGP)
                .status(BetStatus.PENDING)
                .stake(stake.setScale(SCALE, RoundingMode.HALF_UP))
                .odds(BigDecimal.valueOf(adjustedDecimal).setScale(SCALE + 1, RoundingMode.HALF_UP))
                .potentialPayout(potentialPayout)
                .sport(legs.getFirst().sport())
                .eventId(eventId)
                .description(sgpDescription)
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
                    .eventId(eventId)
                    .sport(legInput.sport())
                    .correlationGroup(eventId)
                    .build();
            betLegRepository.save(betLeg);
            legSummaries.add(
                    new LegSummary(i + 1, legInput.sport(), eventId, legInput.selection(), legInput.americanOdds()));
        }

        BigDecimal newBalance = deductStake(bankroll, stake, bet.getId());

        String summary = String.format(
                "Placed %d-leg SGP at %s adjusted odds ($%s to win $%s, %.0f%% correlation adj). Balance: $%s",
                legs.size(),
                formatAmericanOdds(adjustedAmerican),
                stake.setScale(SCALE, RoundingMode.HALF_UP),
                potentialPayout.subtract(stake).setScale(SCALE, RoundingMode.HALF_UP),
                (1.0 - correlationAdjustment) * 100,
                newBalance.setScale(SCALE, RoundingMode.HALF_UP));

        log.info(
                "SGP placed bet_id={} legs={} stake={} adj={}", bet.getId(), legs.size(), stake, correlationAdjustment);

        return new PlaceSgpResult(
                bet.getId(),
                stake,
                BigDecimal.valueOf(adjustedDecimal),
                adjustedAmerican,
                potentialPayout,
                legSummaries,
                correlationAdjustment,
                newBalance,
                summary);
    }

    /** Edits a pending bet's stake. Only PENDING bets can be edited. */
    public EditBetResult editBet(String betId, BigDecimal newStake) {
        Bet bet = betRepository
                .findById(betId)
                .orElseThrow(() -> new IllegalArgumentException("Bet not found: " + betId));

        if (bet.getStatus() != BetStatus.PENDING) {
            throw new IllegalStateException(
                    "Cannot edit bet with status " + bet.getStatus().name() + " — only PENDING bets can be edited");
        }

        BigDecimal oldStake = bet.getStake();
        if (newStake != null && newStake.compareTo(BigDecimal.ZERO) > 0) {
            BigDecimal stakeDelta = newStake.subtract(oldStake);
            Bankroll bankroll = bet.getBankroll();

            if (stakeDelta.compareTo(BigDecimal.ZERO) > 0) {
                validateSufficientBalance(stakeDelta, bankroll);
            }

            // Adjust bankroll
            BigDecimal newBalance = bankroll.getCurrentBalance().subtract(stakeDelta);
            bankroll.setCurrentBalance(newBalance);
            bankrollRepository.save(bankroll);

            // Record transaction
            TransactionType txnType = stakeDelta.compareTo(BigDecimal.ZERO) > 0
                    ? TransactionType.BET_PLACED
                    : TransactionType.BET_CANCELLED;
            BankrollTransaction txn = BankrollTransaction.builder()
                    .bankroll(bankroll)
                    .type(txnType)
                    .amount(stakeDelta.abs().setScale(SCALE, RoundingMode.HALF_UP))
                    .balanceAfter(newBalance.setScale(SCALE, RoundingMode.HALF_UP))
                    .referenceBetId(betId)
                    .createdAt(Instant.now())
                    .build();
            transactionRepository.save(txn);

            // Recalculate potential payout
            int americanOdds = OddsUtil.decimalToAmerican(bet.getOdds().doubleValue());
            BigDecimal newPotentialPayout = PayoutCalculator.totalReturn(newStake, americanOdds);

            bet.setStake(newStake.setScale(SCALE, RoundingMode.HALF_UP));
            bet.setPotentialPayout(newPotentialPayout);
            betRepository.save(bet);

            String summary = String.format(
                    "Edited bet %s: stake $%s → $%s (delta $%s). New payout: $%s. Balance: $%s",
                    betId,
                    oldStake.setScale(SCALE, RoundingMode.HALF_UP),
                    newStake.setScale(SCALE, RoundingMode.HALF_UP),
                    stakeDelta.setScale(SCALE, RoundingMode.HALF_UP),
                    newPotentialPayout.setScale(SCALE, RoundingMode.HALF_UP),
                    newBalance.setScale(SCALE, RoundingMode.HALF_UP));

            log.info("Bet edited bet_id={} old_stake={} new_stake={}", betId, oldStake, newStake);

            return new EditBetResult(betId, oldStake, newStake, stakeDelta, newPotentialPayout, newBalance, summary);
        }

        throw new IllegalArgumentException("New stake must be positive");
    }

    /**
     * Internal method to place a sub-parlay for round-robin without deducting stake (parent handles
     * the bankroll deduction).
     */
    private PlaceParlayResult placeParlayBetInternal(
            List<ParlayLegInput> legs, BigDecimal stake, Bankroll bankroll, String parentBetId) {

        List<Integer> americanOddsList =
                legs.stream().map(ParlayLegInput::americanOdds).toList();
        BigDecimal parlayProfit = PayoutCalculator.parlayPayout(stake, americanOddsList);
        BigDecimal potentialPayout = parlayProfit.add(stake);

        double combinedDecimal = 1.0;
        for (ParlayLegInput leg : legs) {
            combinedDecimal *= OddsUtil.americanToDecimal(leg.americanOdds());
        }
        int combinedAmerican = OddsUtil.decimalToAmerican(combinedDecimal);

        Bet bet = Bet.builder()
                .bankroll(bankroll)
                .betType(BetType.PARLAY)
                .status(BetStatus.PENDING)
                .stake(stake.setScale(SCALE, RoundingMode.HALF_UP))
                .odds(BigDecimal.valueOf(combinedDecimal).setScale(SCALE + 1, RoundingMode.HALF_UP))
                .potentialPayout(potentialPayout)
                .sport(PARLAY_SPORT)
                .eventId(PARLAY_EVENT_ID)
                .description(legs.size() + "-leg sub-parlay")
                .placedAt(Instant.now())
                .parentBetId(parentBetId)
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

        return new PlaceParlayResult(
                bet.getId(),
                stake,
                BigDecimal.valueOf(combinedDecimal),
                combinedAmerican,
                potentialPayout,
                legSummaries,
                bankroll.getCurrentBalance(),
                "Sub-parlay placed");
    }

    private static <T> List<List<T>> combinations(List<T> items, int k) {
        List<List<T>> result = new ArrayList<>();
        combinationsHelper(items, k, 0, new ArrayList<>(), result);
        return result;
    }

    private static <T> void combinationsHelper(List<T> items, int k, int start, List<T> current, List<List<T>> result) {
        if (current.size() == k) {
            result.add(new ArrayList<>(current));
            return;
        }
        for (int i = start; i < items.size(); i++) {
            current.add(items.get(i));
            combinationsHelper(items, k, i + 1, current, result);
            current.remove(current.size() - 1);
        }
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
