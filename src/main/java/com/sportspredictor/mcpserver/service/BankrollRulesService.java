package com.sportspredictor.mcpserver.service;

import com.sportspredictor.mcpserver.entity.Bankroll;
import com.sportspredictor.mcpserver.entity.BankrollRules;
import com.sportspredictor.mcpserver.entity.Bet;
import com.sportspredictor.mcpserver.entity.enums.BetStatus;
import com.sportspredictor.mcpserver.repository.BankrollRulesRepository;
import com.sportspredictor.mcpserver.repository.BetRepository;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Manages bankroll wagering rules and daily limits enforcement. */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class BankrollRulesService {

    private static final int SCALE = 2;

    private final BankrollRulesRepository rulesRepository;
    private final BankrollService bankrollService;
    private final BetRepository betRepository;

    /** Result of setting bankroll rules. */
    public record SetRulesResult(
            String rulesId,
            BigDecimal maxBetUnits,
            BigDecimal dailyExposureLimit,
            BigDecimal stopLossThreshold,
            Integer maxParlayLegs,
            BigDecimal minConfidence,
            BigDecimal unitSize,
            String summary) {}

    /** Result of checking daily limits status. */
    public record DailyLimitsResult(
            BigDecimal dailyExposureLimit,
            BigDecimal wageredToday,
            BigDecimal remainingCapacity,
            BigDecimal stopLossThreshold,
            BigDecimal dailyPnl,
            boolean stopLossTriggered,
            BigDecimal maxBetUnits,
            BigDecimal unitSize,
            BigDecimal maxBetAmount,
            Integer maxParlayLegs,
            String summary) {}

    /** Sets or updates bankroll rules for the active bankroll. */
    public SetRulesResult setRules(
            BigDecimal maxBetUnits,
            BigDecimal dailyExposureLimit,
            BigDecimal stopLossThreshold,
            Integer maxParlayLegs,
            BigDecimal minConfidence,
            BigDecimal unitSize) {

        Bankroll bankroll = bankrollService.getActiveBankroll();
        BankrollRules rules = rulesRepository
                .findByBankrollId(bankroll.getId())
                .orElse(BankrollRules.builder()
                        .bankroll(bankroll)
                        .createdAt(Instant.now())
                        .build());

        if (maxBetUnits != null) {
            rules.setMaxBetUnits(maxBetUnits);
        }
        if (dailyExposureLimit != null) {
            rules.setDailyExposureLimit(dailyExposureLimit);
        }
        if (stopLossThreshold != null) {
            rules.setStopLossThreshold(stopLossThreshold);
        }
        if (maxParlayLegs != null) {
            rules.setMaxParlayLegs(maxParlayLegs);
        }
        if (minConfidence != null) {
            rules.setMinConfidence(minConfidence);
        }
        if (unitSize != null) {
            rules.setUnitSize(unitSize);
        }
        rules.setUpdatedAt(Instant.now());

        rulesRepository.save(rules);

        log.info("Bankroll rules updated rules_id={} bankroll_id={}", rules.getId(), bankroll.getId());

        return new SetRulesResult(
                rules.getId(),
                rules.getMaxBetUnits(),
                rules.getDailyExposureLimit(),
                rules.getStopLossThreshold(),
                rules.getMaxParlayLegs(),
                rules.getMinConfidence(),
                rules.getUnitSize(),
                "Bankroll rules updated successfully");
    }

    /** Checks remaining daily wagering capacity against configured limits. */
    @Transactional(readOnly = true)
    public DailyLimitsResult getDailyLimitsStatus() {
        Bankroll bankroll = bankrollService.getActiveBankroll();
        BankrollRules rules = rulesRepository.findByBankrollId(bankroll.getId()).orElse(null);

        Instant startOfDay =
                LocalDate.now(ZoneOffset.UTC).atStartOfDay(ZoneOffset.UTC).toInstant();
        Instant endOfDay = startOfDay.plusSeconds(86400);

        var todayBets = betRepository.findByPlacedAtBetween(startOfDay, endOfDay);

        BigDecimal wageredToday = todayBets.stream().map(Bet::getStake).reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal dailyPnl = todayBets.stream()
                .filter(b -> b.getStatus() == BetStatus.WON || b.getStatus() == BetStatus.LOST)
                .map(b -> {
                    if (b.getStatus() == BetStatus.WON) {
                        return b.getActualPayout() != null
                                ? b.getActualPayout().subtract(b.getStake())
                                : BigDecimal.ZERO;
                    }
                    return b.getStake().negate();
                })
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal dailyExposureLimit = rules != null && rules.getDailyExposureLimit() != null
                ? rules.getDailyExposureLimit()
                : BigDecimal.ZERO;
        BigDecimal remainingCapacity = dailyExposureLimit.compareTo(BigDecimal.ZERO) > 0
                ? dailyExposureLimit.subtract(wageredToday).max(BigDecimal.ZERO)
                : BigDecimal.ZERO;

        BigDecimal stopLossThreshold =
                rules != null && rules.getStopLossThreshold() != null ? rules.getStopLossThreshold() : BigDecimal.ZERO;
        boolean stopLossTriggered = stopLossThreshold.compareTo(BigDecimal.ZERO) > 0
                && dailyPnl.negate().compareTo(stopLossThreshold) >= 0;

        BigDecimal maxBetUnits =
                rules != null && rules.getMaxBetUnits() != null ? rules.getMaxBetUnits() : BigDecimal.ZERO;
        BigDecimal unitSize = rules != null && rules.getUnitSize() != null ? rules.getUnitSize() : BigDecimal.ZERO;
        BigDecimal maxBetAmount = maxBetUnits.compareTo(BigDecimal.ZERO) > 0 && unitSize.compareTo(BigDecimal.ZERO) > 0
                ? maxBetUnits.multiply(unitSize).setScale(SCALE, RoundingMode.HALF_UP)
                : BigDecimal.ZERO;

        Integer maxParlayLegs = rules != null ? rules.getMaxParlayLegs() : null;

        String summary = String.format(
                "Wagered today: $%s. Remaining: $%s. P/L: $%s. Stop-loss: %s",
                wageredToday.setScale(SCALE, RoundingMode.HALF_UP),
                remainingCapacity.setScale(SCALE, RoundingMode.HALF_UP),
                dailyPnl.setScale(SCALE, RoundingMode.HALF_UP),
                stopLossTriggered ? "TRIGGERED" : "OK");

        return new DailyLimitsResult(
                dailyExposureLimit,
                wageredToday,
                remainingCapacity,
                stopLossThreshold,
                dailyPnl,
                stopLossTriggered,
                maxBetUnits,
                unitSize,
                maxBetAmount,
                maxParlayLegs,
                summary);
    }
}
