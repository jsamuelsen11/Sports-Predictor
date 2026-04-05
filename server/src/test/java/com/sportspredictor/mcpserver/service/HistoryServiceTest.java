package com.sportspredictor.mcpserver.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import com.sportspredictor.mcpserver.entity.Bankroll;
import com.sportspredictor.mcpserver.entity.Bet;
import com.sportspredictor.mcpserver.entity.BetLeg;
import com.sportspredictor.mcpserver.entity.enums.BetLegStatus;
import com.sportspredictor.mcpserver.entity.enums.BetStatus;
import com.sportspredictor.mcpserver.entity.enums.BetType;
import com.sportspredictor.mcpserver.repository.BetLegRepository;
import com.sportspredictor.mcpserver.repository.BetRepository;
import com.sportspredictor.mcpserver.service.HistoryService.BankrollAnalyticsResult;
import com.sportspredictor.mcpserver.service.HistoryService.BetHistoryResult;
import com.sportspredictor.mcpserver.service.HistoryService.BetSlipResult;
import com.sportspredictor.mcpserver.service.HistoryService.DailyPerformanceResult;
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

/** Tests for {@link HistoryService}. */
@ExtendWith(MockitoExtension.class)
class HistoryServiceTest {

    @Mock
    private BetRepository betRepository;

    @Mock
    private BetLegRepository betLegRepository;

    @Mock
    private BankrollService bankrollService;

    @InjectMocks
    private HistoryService historyService;

    private static Bankroll buildBankroll() {
        return Bankroll.builder()
                .id("br-1")
                .name("Test")
                .startingBalance(new BigDecimal("1000"))
                .currentBalance(new BigDecimal("1100"))
                .createdAt(Instant.parse("2026-01-01T00:00:00Z"))
                .build();
    }

    private static Bet buildBet(String id, BetStatus status, String sport, BigDecimal stake) {
        Bankroll bankroll = buildBankroll();
        return Bet.builder()
                .id(id)
                .bankroll(bankroll)
                .betType(BetType.MONEYLINE)
                .status(status)
                .stake(stake)
                .odds(new BigDecimal("1.909"))
                .potentialPayout(new BigDecimal("190.90"))
                .actualPayout(status == BetStatus.WON ? new BigDecimal("190.90") : null)
                .sport(sport)
                .eventId("evt-1")
                .description("Test bet")
                .placedAt(Instant.parse("2026-03-25T12:00:00Z"))
                .settledAt(status == BetStatus.PENDING ? null : Instant.parse("2026-03-25T22:00:00Z"))
                .build();
    }

    /** Tests for {@link HistoryService#getBetHistory}. */
    @Nested
    class GetBetHistory {

        @Test
        void returnsAllBetsWhenNoFilters() {
            Bankroll bankroll = buildBankroll();
            when(bankrollService.getActiveBankroll()).thenReturn(bankroll);
            when(betRepository.findByBankrollId("br-1"))
                    .thenReturn(List.of(
                            buildBet("b1", BetStatus.WON, "nba", new BigDecimal("100")),
                            buildBet("b2", BetStatus.LOST, "nfl", new BigDecimal("50"))));

            BetHistoryResult result =
                    historyService.getBetHistory(null, null, null, null, null, null, null, null, null);

            assertThat(result.totalCount()).isEqualTo(2);
        }

        @Test
        void filtersBySport() {
            Bankroll bankroll = buildBankroll();
            when(bankrollService.getActiveBankroll()).thenReturn(bankroll);
            when(betRepository.findByBankrollId("br-1"))
                    .thenReturn(List.of(
                            buildBet("b1", BetStatus.WON, "nba", new BigDecimal("100")),
                            buildBet("b2", BetStatus.LOST, "nfl", new BigDecimal("50"))));

            BetHistoryResult result =
                    historyService.getBetHistory("nba", null, null, null, null, null, null, null, null);

            assertThat(result.totalCount()).isEqualTo(1);
            assertThat(result.bets().getFirst().sport()).isEqualTo("nba");
        }
    }

    /** Tests for {@link HistoryService#getBetSlip(String)}. */
    @Nested
    class GetBetSlip {

        @Test
        void returnsSlipWithLegs() {
            Bet bet = buildBet("b1", BetStatus.PENDING, "nba", new BigDecimal("100"));
            when(betRepository.findById("b1")).thenReturn(Optional.of(bet));

            BetLeg leg = BetLeg.builder()
                    .id("leg-1")
                    .bet(bet)
                    .legNumber(1)
                    .selection("Lakers ML")
                    .odds(new BigDecimal("2.10"))
                    .status(BetLegStatus.PENDING)
                    .eventId("evt-1")
                    .sport("nba")
                    .build();
            when(betLegRepository.findByBetId("b1")).thenReturn(List.of(leg));

            BetSlipResult result = historyService.getBetSlip("b1");

            assertThat(result.betId()).isEqualTo("b1");
            assertThat(result.legs()).hasSize(1);
            assertThat(result.legs().getFirst().selection()).isEqualTo("Lakers ML");
        }

        @Test
        void throwsForMissingBet() {
            when(betRepository.findById("nonexistent")).thenReturn(Optional.empty());

            assertThatThrownBy(() -> historyService.getBetSlip("nonexistent"))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("not found");
        }
    }

    /** Tests for {@link HistoryService#getBankrollAnalytics()}. */
    @Nested
    class GetBankrollAnalytics {

        @Test
        void computesAnalytics() {
            Bankroll bankroll = buildBankroll();
            when(bankrollService.getActiveBankroll()).thenReturn(bankroll);
            when(betRepository.findByBankrollId("br-1"))
                    .thenReturn(List.of(
                            buildBet("b1", BetStatus.WON, "nba", new BigDecimal("100")),
                            buildBet("b2", BetStatus.LOST, "nfl", new BigDecimal("50"))));

            BankrollAnalyticsResult result = historyService.getBankrollAnalytics();

            assertThat(result.totalBets()).isEqualTo(2);
            assertThat(result.wins()).isEqualTo(1);
            assertThat(result.losses()).isEqualTo(1);
            assertThat(result.winRate()).isEqualTo(0.5);
            assertThat(result.profitBySport()).containsKey("nba");
            assertThat(result.profitBySport()).containsKey("nfl");
        }
    }

    /** Tests for {@link HistoryService#getDailyPerformance()}. */
    @Nested
    class GetDailyPerformance {

        @Test
        void handlesEmptyDay() {
            Bankroll bankroll = buildBankroll();
            when(bankrollService.getActiveBankroll()).thenReturn(bankroll);
            when(betRepository.findByPlacedAtBetween(
                            org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any()))
                    .thenReturn(List.of());

            DailyPerformanceResult result = historyService.getDailyPerformance();

            assertThat(result.placed()).isZero();
            assertThat(result.pending()).isZero();
            assertThat(result.dailyProfitLoss()).isEqualByComparingTo("0.00");
        }

        @Test
        void aggregatesMixedBetStatuses() {
            Bankroll bankroll = buildBankroll();
            when(bankrollService.getActiveBankroll()).thenReturn(bankroll);

            Bet won = buildBetWithPayout("b1", BetStatus.WON, "nba", new BigDecimal("100"), new BigDecimal("250"));
            Bet lost = buildBetWithPayout("b2", BetStatus.LOST, "nfl", new BigDecimal("50"), null);
            Bet pushed = buildBetWithPayout("b3", BetStatus.PUSHED, "mlb", new BigDecimal("25"), null);
            Bet pending = buildBetWithPayout("b4", BetStatus.PENDING, "nba", new BigDecimal("75"), null);

            when(betRepository.findByPlacedAtBetween(
                            org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any()))
                    .thenReturn(List.of(won, lost, pushed, pending));

            DailyPerformanceResult result = historyService.getDailyPerformance();

            assertThat(result.placed()).isEqualTo(4);
            assertThat(result.pending()).isEqualTo(1);
            assertThat(result.settled()).isEqualTo(3);
            assertThat(result.wins()).isEqualTo(1);
            assertThat(result.losses()).isEqualTo(1);
            assertThat(result.pushes()).isEqualTo(1);
            // WON: 250 - 100 = +150; LOST: -50; PUSHED: 0; PENDING: ignored
            assertThat(result.dailyProfitLoss()).isEqualByComparingTo("100.00");
        }
    }

    private static Bet buildBetWithPayout(
            String id, BetStatus status, String sport, BigDecimal stake, BigDecimal actualPayout) {
        Bankroll bankroll = buildBankroll();
        return Bet.builder()
                .id(id)
                .bankroll(bankroll)
                .betType(BetType.MONEYLINE)
                .status(status)
                .stake(stake)
                .odds(new BigDecimal("1.909"))
                .potentialPayout(new BigDecimal("190.90"))
                .actualPayout(actualPayout)
                .sport(sport)
                .eventId("evt-1")
                .description("Test bet")
                .placedAt(Instant.parse("2026-03-25T12:00:00Z"))
                .settledAt(status == BetStatus.PENDING ? null : Instant.parse("2026-03-25T22:00:00Z"))
                .build();
    }
}
