package com.sportspredictor.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.sportspredictor.entity.Bankroll;
import com.sportspredictor.entity.Bet;
import com.sportspredictor.entity.BetLeg;
import com.sportspredictor.entity.enums.BetLegStatus;
import com.sportspredictor.entity.enums.BetStatus;
import com.sportspredictor.entity.enums.BetType;
import com.sportspredictor.repository.BankrollRepository;
import com.sportspredictor.repository.BankrollTransactionRepository;
import com.sportspredictor.repository.BetLegRepository;
import com.sportspredictor.repository.BetRepository;
import com.sportspredictor.service.SettlementService.LegSettlement;
import com.sportspredictor.service.SettlementService.SettleBetResult;
import com.sportspredictor.service.SettlementService.SettleParlayResult;
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

/** Tests for {@link SettlementService}. */
@ExtendWith(MockitoExtension.class)
class SettlementServiceTest {

    @Mock
    private BetRepository betRepository;

    @Mock
    private BetLegRepository betLegRepository;

    @Mock
    private BankrollRepository bankrollRepository;

    @Mock
    private BankrollTransactionRepository transactionRepository;

    @Mock
    private ResultsService resultsService;

    @InjectMocks
    private SettlementService settlementService;

    private static Bankroll buildBankroll(BigDecimal balance) {
        return Bankroll.builder()
                .id("br-1")
                .name("Test")
                .startingBalance(new BigDecimal("1000"))
                .currentBalance(balance)
                .createdAt(Instant.parse("2026-01-01T00:00:00Z"))
                .build();
    }

    private static Bet buildPendingBet(String id, BigDecimal stake, BigDecimal decimalOdds, Bankroll bankroll) {
        return Bet.builder()
                .id(id)
                .bankroll(bankroll)
                .betType(BetType.MONEYLINE)
                .status(BetStatus.PENDING)
                .stake(stake)
                .odds(decimalOdds)
                .potentialPayout(new BigDecimal("200"))
                .sport("nba")
                .eventId("evt-1")
                .description("Lakers ML")
                .placedAt(Instant.now())
                .build();
    }

    /** Tests for {@link SettlementService#settleBet}. */
    @Nested
    class SettleBet {

        @Test
        void settlesBetAsWon() {
            Bankroll bankroll = buildBankroll(new BigDecimal("900"));
            Bet bet = buildPendingBet("bet-1", new BigDecimal("100"), new BigDecimal("2.50"), bankroll);
            when(betRepository.findById("bet-1")).thenReturn(Optional.of(bet));
            when(transactionRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            SettleBetResult result = settlementService.settleBet("bet-1", "WON");

            assertThat(result.newStatus()).isEqualTo("WON");
            assertThat(result.payout()).isEqualByComparingTo("250.00");
            assertThat(result.balanceAfter()).isEqualByComparingTo("1150.00");
        }

        @Test
        void settlesBetAsLost() {
            Bankroll bankroll = buildBankroll(new BigDecimal("900"));
            Bet bet = buildPendingBet("bet-1", new BigDecimal("100"), new BigDecimal("2.50"), bankroll);
            when(betRepository.findById("bet-1")).thenReturn(Optional.of(bet));
            when(transactionRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            SettleBetResult result = settlementService.settleBet("bet-1", "LOST");

            assertThat(result.newStatus()).isEqualTo("LOST");
            assertThat(result.payout()).isEqualByComparingTo("0.00");
            assertThat(result.balanceAfter()).isEqualByComparingTo("900.00");
        }

        @Test
        void settlesBetAsPush() {
            Bankroll bankroll = buildBankroll(new BigDecimal("900"));
            Bet bet = buildPendingBet("bet-1", new BigDecimal("100"), new BigDecimal("2.50"), bankroll);
            when(betRepository.findById("bet-1")).thenReturn(Optional.of(bet));
            when(transactionRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            SettleBetResult result = settlementService.settleBet("bet-1", "PUSH");

            assertThat(result.newStatus()).isEqualTo("PUSHED");
            assertThat(result.payout()).isEqualByComparingTo("100.00");
            assertThat(result.balanceAfter()).isEqualByComparingTo("1000.00");
        }

        @Test
        void rejectsNonPendingBet() {
            Bankroll bankroll = buildBankroll(new BigDecimal("1000"));
            Bet bet = Bet.builder()
                    .id("bet-1")
                    .bankroll(bankroll)
                    .betType(BetType.MONEYLINE)
                    .status(BetStatus.WON)
                    .stake(new BigDecimal("100"))
                    .odds(new BigDecimal("2.50"))
                    .potentialPayout(new BigDecimal("250"))
                    .actualPayout(new BigDecimal("250"))
                    .sport("nba")
                    .eventId("evt-1")
                    .description("Test")
                    .placedAt(Instant.now())
                    .settledAt(Instant.now())
                    .build();
            when(betRepository.findById("bet-1")).thenReturn(Optional.of(bet));

            assertThatThrownBy(() -> settlementService.settleBet("bet-1", "WON"))
                    .isInstanceOf(IllegalStateException.class);
        }
    }

    /** Tests for {@link SettlementService#settleParlay}. */
    @Nested
    class SettleParlay {

        @Test
        void settlesWinningParlay() {
            Bankroll bankroll = buildBankroll(new BigDecimal("950"));
            Bet bet = Bet.builder()
                    .id("bet-1")
                    .bankroll(bankroll)
                    .betType(BetType.PARLAY)
                    .status(BetStatus.PENDING)
                    .stake(new BigDecimal("50"))
                    .odds(new BigDecimal("3.50"))
                    .potentialPayout(new BigDecimal("175"))
                    .sport("nba")
                    .eventId("evt-1")
                    .description("2-leg parlay")
                    .placedAt(Instant.now())
                    .build();
            when(betRepository.findById("bet-1")).thenReturn(Optional.of(bet));

            BetLeg leg1 = BetLeg.builder()
                    .id("leg-1")
                    .bet(bet)
                    .legNumber(1)
                    .selection("Lakers ML")
                    .odds(new BigDecimal("1.667"))
                    .status(BetLegStatus.PENDING)
                    .eventId("evt-1")
                    .sport("nba")
                    .build();
            BetLeg leg2 = BetLeg.builder()
                    .id("leg-2")
                    .bet(bet)
                    .legNumber(2)
                    .selection("Chiefs ML")
                    .odds(new BigDecimal("2.10"))
                    .status(BetLegStatus.PENDING)
                    .eventId("evt-2")
                    .sport("nfl")
                    .build();
            when(betLegRepository.findByBetId("bet-1")).thenReturn(List.of(leg1, leg2));
            when(betLegRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
            when(transactionRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            List<LegSettlement> outcomes =
                    List.of(new LegSettlement(1, "WON", "Lakers 110-105"), new LegSettlement(2, "WON", "Chiefs 24-17"));

            SettleParlayResult result = settlementService.settleParlay("bet-1", outcomes);

            assertThat(result.newStatus()).isEqualTo("WON");
            assertThat(result.payout().doubleValue()).isGreaterThan(50.0);
            assertThat(result.legs()).hasSize(2);
        }

        @Test
        void losingLegMeansParlayLost() {
            Bankroll bankroll = buildBankroll(new BigDecimal("950"));
            Bet bet = Bet.builder()
                    .id("bet-1")
                    .bankroll(bankroll)
                    .betType(BetType.PARLAY)
                    .status(BetStatus.PENDING)
                    .stake(new BigDecimal("50"))
                    .odds(new BigDecimal("3.50"))
                    .potentialPayout(new BigDecimal("175"))
                    .sport("nba")
                    .eventId("evt-1")
                    .description("2-leg parlay")
                    .placedAt(Instant.now())
                    .build();
            when(betRepository.findById("bet-1")).thenReturn(Optional.of(bet));

            BetLeg leg1 = BetLeg.builder()
                    .id("leg-1")
                    .bet(bet)
                    .legNumber(1)
                    .selection("Lakers ML")
                    .odds(new BigDecimal("1.667"))
                    .status(BetLegStatus.PENDING)
                    .eventId("evt-1")
                    .sport("nba")
                    .build();
            BetLeg leg2 = BetLeg.builder()
                    .id("leg-2")
                    .bet(bet)
                    .legNumber(2)
                    .selection("Chiefs ML")
                    .odds(new BigDecimal("2.10"))
                    .status(BetLegStatus.PENDING)
                    .eventId("evt-2")
                    .sport("nfl")
                    .build();
            when(betLegRepository.findByBetId("bet-1")).thenReturn(List.of(leg1, leg2));
            when(betLegRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
            when(transactionRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            List<LegSettlement> outcomes =
                    List.of(new LegSettlement(1, "WON", null), new LegSettlement(2, "LOST", null));

            SettleParlayResult result = settlementService.settleParlay("bet-1", outcomes);

            assertThat(result.newStatus()).isEqualTo("LOST");
            assertThat(result.payout()).isEqualByComparingTo("0.00");
        }
    }
}
