package com.sportspredictor.mcpserver.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.sportspredictor.mcpserver.entity.Bankroll;
import com.sportspredictor.mcpserver.entity.BankrollRules;
import com.sportspredictor.mcpserver.entity.Bet;
import com.sportspredictor.mcpserver.entity.enums.BetStatus;
import com.sportspredictor.mcpserver.entity.enums.BetType;
import com.sportspredictor.mcpserver.repository.BankrollRulesRepository;
import com.sportspredictor.mcpserver.repository.BetRepository;
import com.sportspredictor.mcpserver.service.BankrollRulesService.DailyLimitsResult;
import com.sportspredictor.mcpserver.service.BankrollRulesService.SetRulesResult;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/** Tests for {@link BankrollRulesService}. */
@ExtendWith(MockitoExtension.class)
class BankrollRulesServiceTest {

    @Mock
    private BankrollRulesRepository rulesRepository;

    @Mock
    private BankrollService bankrollService;

    @Mock
    private BetRepository betRepository;

    @InjectMocks
    private BankrollRulesService bankrollRulesService;

    private static Bankroll buildBankroll(String id) {
        return Bankroll.builder()
                .id(id)
                .name("Test")
                .startingBalance(new BigDecimal("1000"))
                .currentBalance(new BigDecimal("1000"))
                .createdAt(Instant.parse("2026-01-01T00:00:00Z"))
                .build();
    }

    private static BankrollRules buildRules(Bankroll bankroll) {
        return BankrollRules.builder()
                .bankroll(bankroll)
                .maxBetUnits(new BigDecimal("3.0"))
                .dailyExposureLimit(new BigDecimal("500"))
                .stopLossThreshold(new BigDecimal("200"))
                .maxParlayLegs(4)
                .minConfidence(new BigDecimal("0.60"))
                .unitSize(new BigDecimal("25.00"))
                .createdAt(Instant.parse("2026-01-01T00:00:00Z"))
                .updatedAt(Instant.parse("2026-01-01T00:00:00Z"))
                .build();
    }

    private static Bet buildBet(String id, BetStatus status, BigDecimal stake, BigDecimal actualPayout) {
        Bankroll bankroll = buildBankroll("br-1");
        return Bet.builder()
                .id(id)
                .bankroll(bankroll)
                .betType(BetType.MONEYLINE)
                .status(status)
                .stake(stake)
                .odds(new BigDecimal("1.909"))
                .potentialPayout(stake.multiply(new BigDecimal("1.909")))
                .actualPayout(actualPayout)
                .sport("nba")
                .eventId("evt-1")
                .description("Test bet")
                .placedAt(Instant.now())
                .build();
    }

    /** Tests for {@link BankrollRulesService#setRules}. */
    @Nested
    class SetRules {

        @Test
        void createsNewRulesWhenNoneExist() {
            Bankroll bankroll = buildBankroll("br-1");
            when(bankrollService.getActiveBankroll()).thenReturn(bankroll);
            when(rulesRepository.findByBankrollId("br-1")).thenReturn(Optional.empty());
            when(rulesRepository.save(any(BankrollRules.class))).thenAnswer(inv -> {
                BankrollRules r = inv.getArgument(0);
                return r;
            });

            SetRulesResult result = bankrollRulesService.setRules(
                    new BigDecimal("3.0"),
                    new BigDecimal("500"),
                    new BigDecimal("200"),
                    4,
                    new BigDecimal("0.60"),
                    new BigDecimal("25.00"));

            assertThat(result.maxBetUnits()).isEqualByComparingTo("3.0");
            assertThat(result.dailyExposureLimit()).isEqualByComparingTo("500");
            assertThat(result.stopLossThreshold()).isEqualByComparingTo("200");
            assertThat(result.maxParlayLegs()).isEqualTo(4);
            assertThat(result.minConfidence()).isEqualByComparingTo("0.60");
            assertThat(result.unitSize()).isEqualByComparingTo("25.00");
            assertThat(result.summary()).isEqualTo("Bankroll rules updated successfully");
            verify(rulesRepository).save(any(BankrollRules.class));
        }

        @Test
        void updatesExistingRules() {
            Bankroll bankroll = buildBankroll("br-1");
            BankrollRules existing = buildRules(bankroll);
            when(bankrollService.getActiveBankroll()).thenReturn(bankroll);
            when(rulesRepository.findByBankrollId("br-1")).thenReturn(Optional.of(existing));
            when(rulesRepository.save(any(BankrollRules.class))).thenAnswer(inv -> inv.getArgument(0));

            SetRulesResult result = bankrollRulesService.setRules(
                    new BigDecimal("5.0"),
                    new BigDecimal("1000"),
                    new BigDecimal("300"),
                    6,
                    new BigDecimal("0.70"),
                    new BigDecimal("50.00"));

            assertThat(result.maxBetUnits()).isEqualByComparingTo("5.0");
            assertThat(result.dailyExposureLimit()).isEqualByComparingTo("1000");
            assertThat(result.stopLossThreshold()).isEqualByComparingTo("300");
            assertThat(result.maxParlayLegs()).isEqualTo(6);
            assertThat(result.unitSize()).isEqualByComparingTo("50.00");
            verify(rulesRepository).save(existing);
        }

        @Test
        void onlyUpdatesNonNullFields() {
            Bankroll bankroll = buildBankroll("br-1");
            BankrollRules existing = buildRules(bankroll);
            when(bankrollService.getActiveBankroll()).thenReturn(bankroll);
            when(rulesRepository.findByBankrollId("br-1")).thenReturn(Optional.of(existing));
            when(rulesRepository.save(any(BankrollRules.class))).thenAnswer(inv -> inv.getArgument(0));

            // Only update unitSize; leave all others null
            SetRulesResult result =
                    bankrollRulesService.setRules(null, null, null, null, null, new BigDecimal("50.00"));

            // Fields that were not null remain unchanged
            assertThat(result.maxBetUnits()).isEqualByComparingTo("3.0");
            assertThat(result.dailyExposureLimit()).isEqualByComparingTo("500");
            assertThat(result.stopLossThreshold()).isEqualByComparingTo("200");
            assertThat(result.maxParlayLegs()).isEqualTo(4);
            assertThat(result.minConfidence()).isEqualByComparingTo("0.60");
            // Only unitSize is updated
            assertThat(result.unitSize()).isEqualByComparingTo("50.00");
        }
    }

    /** Tests for {@link BankrollRulesService#getDailyLimitsStatus()}. */
    @Nested
    class GetDailyLimitsStatus {

        @Test
        void returnsCorrectWageredAmountFromTodaysBets() {
            Bankroll bankroll = buildBankroll("br-1");
            BankrollRules rules = buildRules(bankroll);
            when(bankrollService.getActiveBankroll()).thenReturn(bankroll);
            when(rulesRepository.findByBankrollId("br-1")).thenReturn(Optional.of(rules));
            when(betRepository.findByPlacedAtBetween(any(Instant.class), any(Instant.class)))
                    .thenReturn(List.of(
                            buildBet("b1", BetStatus.PENDING, new BigDecimal("100"), null),
                            buildBet("b2", BetStatus.PENDING, new BigDecimal("75"), null)));

            DailyLimitsResult result = bankrollRulesService.getDailyLimitsStatus();

            assertThat(result.wageredToday()).isEqualByComparingTo("175");
        }

        @Test
        void returnsCorrectRemainingCapacity() {
            Bankroll bankroll = buildBankroll("br-1");
            BankrollRules rules = buildRules(bankroll); // dailyExposureLimit = 500
            when(bankrollService.getActiveBankroll()).thenReturn(bankroll);
            when(rulesRepository.findByBankrollId("br-1")).thenReturn(Optional.of(rules));
            when(betRepository.findByPlacedAtBetween(any(Instant.class), any(Instant.class)))
                    .thenReturn(List.of(buildBet("b1", BetStatus.PENDING, new BigDecimal("200"), null)));

            DailyLimitsResult result = bankrollRulesService.getDailyLimitsStatus();

            assertThat(result.dailyExposureLimit()).isEqualByComparingTo("500");
            assertThat(result.wageredToday()).isEqualByComparingTo("200");
            assertThat(result.remainingCapacity()).isEqualByComparingTo("300");
        }

        @Test
        void detectsStopLossTriggered() {
            Bankroll bankroll = buildBankroll("br-1");
            BankrollRules rules = buildRules(bankroll); // stopLossThreshold = 200
            when(bankrollService.getActiveBankroll()).thenReturn(bankroll);
            when(rulesRepository.findByBankrollId("br-1")).thenReturn(Optional.of(rules));

            // Two settled losses totalling $250 loss
            Bet lost1 = buildBet("b1", BetStatus.LOST, new BigDecimal("150"), null);
            Bet lost2 = buildBet("b2", BetStatus.LOST, new BigDecimal("100"), null);
            when(betRepository.findByPlacedAtBetween(any(Instant.class), any(Instant.class)))
                    .thenReturn(List.of(lost1, lost2));

            DailyLimitsResult result = bankrollRulesService.getDailyLimitsStatus();

            assertThat(result.stopLossTriggered()).isTrue();
            assertThat(result.dailyPnl()).isEqualByComparingTo("-250");
        }

        @Test
        void handlesNoRulesConfigured() {
            Bankroll bankroll = buildBankroll("br-1");
            when(bankrollService.getActiveBankroll()).thenReturn(bankroll);
            when(rulesRepository.findByBankrollId("br-1")).thenReturn(Optional.empty());
            when(betRepository.findByPlacedAtBetween(any(Instant.class), any(Instant.class)))
                    .thenReturn(List.of());

            DailyLimitsResult result = bankrollRulesService.getDailyLimitsStatus();

            assertThat(result.dailyExposureLimit()).isEqualByComparingTo("0");
            assertThat(result.remainingCapacity()).isEqualByComparingTo("0");
            assertThat(result.stopLossThreshold()).isEqualByComparingTo("0");
            assertThat(result.stopLossTriggered()).isFalse();
            assertThat(result.maxBetUnits()).isEqualByComparingTo("0");
            assertThat(result.unitSize()).isEqualByComparingTo("0");
            assertThat(result.maxBetAmount()).isEqualByComparingTo("0");
            assertThat(result.maxParlayLegs()).isNull();
        }
    }
}
