package com.sportspredictor.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.sportspredictor.entity.Bankroll;
import com.sportspredictor.entity.Bet;
import com.sportspredictor.entity.enums.BetStatus;
import com.sportspredictor.entity.enums.BetType;
import com.sportspredictor.repository.BankrollRepository;
import com.sportspredictor.repository.BankrollTransactionRepository;
import com.sportspredictor.repository.BetRepository;
import com.sportspredictor.service.BankrollService.BankrollStatusResult;
import com.sportspredictor.service.BankrollService.DepositResult;
import com.sportspredictor.service.BankrollService.ResetBankrollResult;
import com.sportspredictor.service.BankrollService.WithdrawResult;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/** Tests for {@link BankrollService}. */
@ExtendWith(MockitoExtension.class)
class BankrollServiceTest {

    @Mock
    private BankrollRepository bankrollRepository;

    @Mock
    private BankrollTransactionRepository transactionRepository;

    @Mock
    private BetRepository betRepository;

    @InjectMocks
    private BankrollService bankrollService;

    private static Bankroll buildBankroll(String id, BigDecimal starting, BigDecimal current) {
        return Bankroll.builder()
                .id(id)
                .name("Test")
                .startingBalance(starting)
                .currentBalance(current)
                .createdAt(Instant.parse("2026-01-01T00:00:00Z"))
                .build();
    }

    private static Bet buildBet(String id, BetStatus status, BigDecimal stake, BigDecimal actualPayout) {
        return Bet.builder()
                .id(id)
                .bankroll(buildBankroll("br-1", new BigDecimal("1000"), new BigDecimal("1000")))
                .betType(BetType.MONEYLINE)
                .status(status)
                .stake(stake)
                .odds(new BigDecimal("1.909"))
                .potentialPayout(new BigDecimal("190.90"))
                .actualPayout(actualPayout)
                .sport("nba")
                .eventId("evt-1")
                .description("Test bet")
                .placedAt(Instant.parse("2026-01-15T12:00:00Z"))
                .settledAt(status == BetStatus.PENDING ? null : Instant.parse("2026-01-15T22:00:00Z"))
                .build();
    }

    /** Tests for {@link BankrollService#getActiveBankroll()}. */
    @Nested
    class GetActiveBankroll {

        @Test
        void returnsExistingActiveBankroll() {
            Bankroll existing = buildBankroll("br-1", new BigDecimal("1000"), new BigDecimal("800"));
            when(bankrollRepository.findByArchivedAtIsNull()).thenReturn(List.of(existing));

            Bankroll result = bankrollService.getActiveBankroll();

            assertThat(result.getId()).isEqualTo("br-1");
        }

        @Test
        void createsDefaultWhenNoneExists() {
            when(bankrollRepository.findByArchivedAtIsNull()).thenReturn(List.of());
            when(bankrollRepository.save(any(Bankroll.class))).thenAnswer(inv -> inv.getArgument(0));

            Bankroll result = bankrollService.getActiveBankroll();

            assertThat(result.getName()).isEqualTo("Default");
            assertThat(result.getStartingBalance()).isEqualByComparingTo("1000.00");
        }
    }

    /** Tests for {@link BankrollService#getBankrollStatus()}. */
    @Nested
    class GetBankrollStatus {

        @Test
        void computesStatusCorrectly() {
            Bankroll bankroll = buildBankroll("br-1", new BigDecimal("1000"), new BigDecimal("1100"));
            when(bankrollRepository.findByArchivedAtIsNull()).thenReturn(List.of(bankroll));

            Bet won = buildBet("b1", BetStatus.WON, new BigDecimal("100"), new BigDecimal("190.90"));
            Bet lost = buildBet("b2", BetStatus.LOST, new BigDecimal("50"), null);
            Bet pending = buildBet("b3", BetStatus.PENDING, new BigDecimal("25"), null);
            when(betRepository.findByBankrollId("br-1")).thenReturn(List.of(won, lost, pending));

            BankrollStatusResult result = bankrollService.getBankrollStatus();

            assertThat(result.wins()).isEqualTo(1);
            assertThat(result.losses()).isEqualTo(1);
            assertThat(result.pending()).isEqualTo(1);
            assertThat(result.totalBets()).isEqualTo(3);
            assertThat(result.netProfitLoss()).isEqualByComparingTo("100.00");
        }

        @Test
        void handlesEmptyBets() {
            Bankroll bankroll = buildBankroll("br-1", new BigDecimal("1000"), new BigDecimal("1000"));
            when(bankrollRepository.findByArchivedAtIsNull()).thenReturn(List.of(bankroll));
            when(betRepository.findByBankrollId("br-1")).thenReturn(List.of());

            BankrollStatusResult result = bankrollService.getBankrollStatus();

            assertThat(result.totalBets()).isZero();
            assertThat(result.currentStreak()).isZero();
            assertThat(result.streakType()).isEqualTo("NONE");
        }
    }

    /** Tests for {@link BankrollService#depositFunds(BigDecimal)}. */
    @Nested
    class DepositFunds {

        @Test
        void depositsCorrectly() {
            Bankroll bankroll = buildBankroll("br-1", new BigDecimal("1000"), new BigDecimal("800"));
            when(bankrollRepository.findByArchivedAtIsNull()).thenReturn(List.of(bankroll));
            when(transactionRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            DepositResult result = bankrollService.depositFunds(new BigDecimal("200"));

            assertThat(result.balanceAfter()).isEqualByComparingTo("1000");
            verify(bankrollRepository).save(bankroll);
        }

        @Test
        void rejectsNegativeAmount() {
            assertThatThrownBy(() -> bankrollService.depositFunds(new BigDecimal("-50")))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("positive");
        }
    }

    /** Tests for {@link BankrollService#withdrawFunds(BigDecimal)}. */
    @Nested
    class WithdrawFunds {

        @Test
        void withdrawsCorrectly() {
            Bankroll bankroll = buildBankroll("br-1", new BigDecimal("1000"), new BigDecimal("800"));
            when(bankrollRepository.findByArchivedAtIsNull()).thenReturn(List.of(bankroll));
            when(transactionRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            WithdrawResult result = bankrollService.withdrawFunds(new BigDecimal("200"));

            assertThat(result.balanceAfter()).isEqualByComparingTo("600");
        }

        @Test
        void rejectsInsufficientFunds() {
            Bankroll bankroll = buildBankroll("br-1", new BigDecimal("1000"), new BigDecimal("100"));
            when(bankrollRepository.findByArchivedAtIsNull()).thenReturn(List.of(bankroll));

            assertThatThrownBy(() -> bankrollService.withdrawFunds(new BigDecimal("500")))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Insufficient");
        }
    }

    /** Tests for {@link BankrollService#resetBankroll(BigDecimal)}. */
    @Nested
    class ResetBankroll {

        @Test
        void archivesAndCreatesNew() {
            Bankroll current = buildBankroll("br-1", new BigDecimal("1000"), new BigDecimal("800"));
            when(bankrollRepository.findByArchivedAtIsNull()).thenReturn(List.of(current));
            when(bankrollRepository.findByArchivedAtIsNotNull()).thenReturn(List.of());
            when(bankrollRepository.save(any(Bankroll.class))).thenAnswer(inv -> inv.getArgument(0));

            ResetBankrollResult result = bankrollService.resetBankroll(new BigDecimal("2000"));

            assertThat(result.archivedBankrollId()).isEqualTo("br-1");
            assertThat(result.startingBalance()).isEqualByComparingTo("2000");
            assertThat(current.getArchivedAt()).isNotNull();
        }
    }
}
