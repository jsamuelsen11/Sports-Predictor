package com.sportspredictor.mcpserver.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.sportspredictor.mcpserver.entity.Bankroll;
import com.sportspredictor.mcpserver.entity.Bet;
import com.sportspredictor.mcpserver.entity.enums.BetStatus;
import com.sportspredictor.mcpserver.entity.enums.BetType;
import com.sportspredictor.mcpserver.repository.BankrollRepository;
import com.sportspredictor.mcpserver.repository.BankrollTransactionRepository;
import com.sportspredictor.mcpserver.repository.BetLegRepository;
import com.sportspredictor.mcpserver.repository.BetRepository;
import com.sportspredictor.mcpserver.service.BettingService.CancelBetResult;
import com.sportspredictor.mcpserver.service.BettingService.ParlayLegInput;
import com.sportspredictor.mcpserver.service.BettingService.PlaceBetResult;
import com.sportspredictor.mcpserver.service.BettingService.PlaceParlayResult;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

/** Tests for {@link BettingService}. */
@ExtendWith(MockitoExtension.class)
class BettingServiceTest {

    @Mock
    private BetRepository betRepository;

    @Mock
    private BetLegRepository betLegRepository;

    @Mock
    private BankrollService bankrollService;

    @Mock
    private BankrollRepository bankrollRepository;

    @Mock
    private BankrollTransactionRepository transactionRepository;

    @Spy
    private MeterRegistry meterRegistry = new SimpleMeterRegistry();

    @InjectMocks
    private BettingService bettingService;

    private static Bankroll buildBankroll(BigDecimal balance) {
        return Bankroll.builder()
                .id("br-1")
                .name("Test")
                .startingBalance(new BigDecimal("1000"))
                .currentBalance(balance)
                .createdAt(Instant.parse("2026-01-01T00:00:00Z"))
                .build();
    }

    /** Tests for {@link BettingService#placeBet}. */
    @Nested
    class PlaceBet {

        @Test
        void placesBetSuccessfully() {
            when(bankrollService.getActiveBankroll()).thenReturn(buildBankroll(new BigDecimal("1000")));
            when(betRepository.save(any(Bet.class))).thenAnswer(inv -> {
                Bet bet = inv.getArgument(0);
                if (bet.getId() == null) {
                    bet.setId("bet-gen-1");
                }
                return bet;
            });
            when(transactionRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            PlaceBetResult result = bettingService.placeBet(
                    "nba", "evt-1", "MONEYLINE", "Lakers ML", -150, new BigDecimal("100"), "Lakers ML bet", null);

            assertThat(result.betId()).isEqualTo("bet-gen-1");
            assertThat(result.sport()).isEqualTo("nba");
            assertThat(result.betType()).isEqualTo("MONEYLINE");
            assertThat(result.americanOdds()).isEqualTo(-150);
            assertThat(result.balanceAfter()).isEqualByComparingTo("900");
            verify(betRepository).save(any(Bet.class));
        }

        @Test
        void rejectsInsufficientBalance() {
            when(bankrollService.getActiveBankroll()).thenReturn(buildBankroll(new BigDecimal("50")));

            assertThatThrownBy(() -> bettingService.placeBet(
                            "nba", "evt-1", "MONEYLINE", "Lakers ML", -150, new BigDecimal("100"), "Test", null))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Insufficient");
        }

        @Test
        void rejectsInvalidBetType() {
            assertThatThrownBy(() -> bettingService.placeBet(
                            "nba", "evt-1", "INVALID_TYPE", "Lakers ML", -150, new BigDecimal("100"), "Test", null))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Invalid bet type");
        }

        @Test
        void rejectsNegativeStake() {
            assertThatThrownBy(() -> bettingService.placeBet(
                            "nba", "evt-1", "MONEYLINE", "Lakers ML", -150, new BigDecimal("-10"), "Test", null))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("positive");
        }
    }

    /** Tests for {@link BettingService#placeParlayBet}. */
    @Nested
    class PlaceParlayBet {

        @Test
        void placesParlaySuccessfully() {
            when(bankrollService.getActiveBankroll()).thenReturn(buildBankroll(new BigDecimal("1000")));
            when(betRepository.save(any(Bet.class))).thenAnswer(inv -> {
                Bet bet = inv.getArgument(0);
                if (bet.getId() == null) {
                    bet.setId("parlay-1");
                }
                return bet;
            });
            when(betLegRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
            when(transactionRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            List<ParlayLegInput> legs = List.of(
                    new ParlayLegInput("nba", "evt-1", "Lakers ML", -150),
                    new ParlayLegInput("nfl", "evt-2", "Chiefs ML", +110));

            PlaceParlayResult result = bettingService.placeParlayBet(legs, new BigDecimal("50"), "Test parlay", null);

            assertThat(result.betId()).isEqualTo("parlay-1");
            assertThat(result.legs()).hasSize(2);
            assertThat(result.combinedDecimalOdds().doubleValue()).isGreaterThan(1.0);
            assertThat(result.balanceAfter()).isEqualByComparingTo("950");
        }

        @Test
        void rejectsSingleLeg() {
            List<ParlayLegInput> legs = List.of(new ParlayLegInput("nba", "evt-1", "Lakers ML", -150));

            assertThatThrownBy(() -> bettingService.placeParlayBet(legs, new BigDecimal("50"), "Test", null))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("at least 2");
        }
    }

    /** Tests for {@link BettingService#cancelBet(String)}. */
    @Nested
    class CancelBet {

        @Test
        void cancelsPendingBet() {
            Bankroll bankroll = buildBankroll(new BigDecimal("900"));
            Bet bet = Bet.builder()
                    .id("bet-1")
                    .bankroll(bankroll)
                    .betType(BetType.MONEYLINE)
                    .status(BetStatus.PENDING)
                    .stake(new BigDecimal("100"))
                    .odds(new BigDecimal("1.667"))
                    .potentialPayout(new BigDecimal("166.70"))
                    .sport("nba")
                    .eventId("evt-1")
                    .description("Test")
                    .placedAt(Instant.now())
                    .build();
            when(betRepository.findById("bet-1")).thenReturn(Optional.of(bet));
            when(transactionRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            CancelBetResult result = bettingService.cancelBet("bet-1");

            assertThat(result.refundedStake()).isEqualByComparingTo("100");
            assertThat(result.balanceAfter()).isEqualByComparingTo("1000");
            assertThat(bet.getStatus()).isEqualTo(BetStatus.CANCELLED);
        }

        @Test
        void rejectsSettledBet() {
            Bet bet = Bet.builder()
                    .id("bet-1")
                    .bankroll(buildBankroll(new BigDecimal("1000")))
                    .betType(BetType.MONEYLINE)
                    .status(BetStatus.WON)
                    .stake(new BigDecimal("100"))
                    .odds(new BigDecimal("1.667"))
                    .potentialPayout(new BigDecimal("166.70"))
                    .actualPayout(new BigDecimal("166.70"))
                    .sport("nba")
                    .eventId("evt-1")
                    .description("Test")
                    .placedAt(Instant.now())
                    .settledAt(Instant.now())
                    .build();
            when(betRepository.findById("bet-1")).thenReturn(Optional.of(bet));

            assertThatThrownBy(() -> bettingService.cancelBet("bet-1"))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("PENDING");
        }
    }
}
