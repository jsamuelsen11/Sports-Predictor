package com.sportspredictor.mcpserver.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.sportspredictor.mcpserver.entity.Bankroll;
import com.sportspredictor.mcpserver.entity.Bet;
import com.sportspredictor.mcpserver.entity.enums.BetStatus;
import com.sportspredictor.mcpserver.entity.enums.BetType;
import com.sportspredictor.mcpserver.repository.BetRepository;
import com.sportspredictor.mcpserver.service.BenchmarkService.BenchmarkResult;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/** Tests for {@link BenchmarkService}. */
@ExtendWith(MockitoExtension.class)
class BenchmarkServiceTest {

    @Mock
    private BetRepository betRepository;

    @InjectMocks
    private BenchmarkService benchmarkService;

    private static Bankroll buildBankroll() {
        return Bankroll.builder()
                .id("br-1")
                .name("Test")
                .startingBalance(new BigDecimal("1000"))
                .currentBalance(new BigDecimal("900"))
                .createdAt(Instant.parse("2026-01-01T00:00:00Z"))
                .build();
    }

    private static Bet buildSettledBet(
            String id, BetStatus status, BigDecimal stake, BigDecimal decimalOdds, BigDecimal actualPayout) {
        return Bet.builder()
                .id(id)
                .bankroll(buildBankroll())
                .betType(BetType.MONEYLINE)
                .status(status)
                .stake(stake)
                .odds(decimalOdds)
                .potentialPayout(stake.multiply(decimalOdds))
                .actualPayout(actualPayout)
                .sport("nba")
                .eventId("evt-1")
                .description("Lakers ML")
                .placedAt(Instant.parse("2026-03-01T12:00:00Z"))
                .settledAt(Instant.parse("2026-03-01T22:00:00Z"))
                .build();
    }

    /** Tests for {@link BenchmarkService#compareToRandom}. */
    @Nested
    class CompareToRandom {

        @Test
        void returnsThreeStrategyMetricsForSettledBets() {
            Bet won = buildSettledBet(
                    "bet-1", BetStatus.WON, new BigDecimal("100"), new BigDecimal("1.909"), new BigDecimal("190.90"));
            Bet lost = buildSettledBet(
                    "bet-2", BetStatus.LOST, new BigDecimal("100"), new BigDecimal("1.909"), BigDecimal.ZERO);
            when(betRepository.findByStatusInAndSettledAtBetween(any(), any(), any()))
                    .thenReturn(List.of(won, lost));

            BenchmarkResult result = benchmarkService.compareToRandom("2026-03-01", "2026-03-31", null);

            assertThat(result.systemMetrics()).isNotNull();
            assertThat(result.randomMetrics()).isNotNull();
            assertThat(result.favoritesMetrics()).isNotNull();
            assertThat(result.summary()).contains("2 bets");
        }

        @Test
        void systemMetricsReflectActualBetOutcomes() {
            Bet won = buildSettledBet(
                    "bet-1", BetStatus.WON, new BigDecimal("100"), new BigDecimal("2.00"), new BigDecimal("200.00"));
            Bet lost = buildSettledBet(
                    "bet-2", BetStatus.LOST, new BigDecimal("100"), new BigDecimal("2.00"), BigDecimal.ZERO);
            when(betRepository.findByStatusInAndSettledAtBetween(any(), any(), any()))
                    .thenReturn(List.of(won, lost));

            BenchmarkResult result = benchmarkService.compareToRandom(null, null, null);

            // 1 win out of 2 = 50% win rate
            assertThat(result.systemMetrics().winRate()).isCloseTo(50.0, org.assertj.core.data.Offset.offset(0.1));
            // Total wagered 200, total returned 200 → P&L = 0
            assertThat(result.systemMetrics().profitLoss()).isEqualByComparingTo("0.00");
            assertThat(result.systemMetrics().totalBets()).isEqualTo(2);
        }

        @Test
        void returnsEmptyMetricsWhenNoBetsFound() {
            when(betRepository.findByStatusInAndSettledAtBetween(any(), any(), any()))
                    .thenReturn(List.of());

            BenchmarkResult result = benchmarkService.compareToRandom(null, null, null);

            assertThat(result.systemMetrics().totalBets()).isEqualTo(0);
            assertThat(result.systemMetrics().winRate()).isEqualTo(0.0);
            assertThat(result.summary()).containsIgnoringCase("no settled bets");
        }

        @Test
        void filtersBySportWhenProvided() {
            Bet nbaBet = buildSettledBet(
                    "bet-1", BetStatus.WON, new BigDecimal("100"), new BigDecimal("1.909"), new BigDecimal("190.90"));
            Bet nflBet = Bet.builder()
                    .id("bet-2")
                    .bankroll(buildBankroll())
                    .betType(BetType.MONEYLINE)
                    .status(BetStatus.LOST)
                    .stake(new BigDecimal("100"))
                    .odds(new BigDecimal("1.909"))
                    .potentialPayout(new BigDecimal("190.90"))
                    .actualPayout(BigDecimal.ZERO)
                    .sport("nfl")
                    .eventId("evt-2")
                    .description("Chiefs ML")
                    .placedAt(Instant.parse("2026-03-01T12:00:00Z"))
                    .settledAt(Instant.parse("2026-03-01T22:00:00Z"))
                    .build();
            when(betRepository.findByStatusInAndSettledAtBetween(any(), any(), any()))
                    .thenReturn(List.of(nbaBet, nflBet));

            BenchmarkResult result = benchmarkService.compareToRandom(null, null, "nba");

            assertThat(result.systemMetrics().totalBets()).isEqualTo(1);
        }

        @Test
        void strategyNamesArePresent() {
            Bet won = buildSettledBet(
                    "bet-1", BetStatus.WON, new BigDecimal("100"), new BigDecimal("1.909"), new BigDecimal("190.90"));
            when(betRepository.findByStatusInAndSettledAtBetween(any(), any(), any()))
                    .thenReturn(List.of(won));

            BenchmarkResult result = benchmarkService.compareToRandom(null, null, null);

            assertThat(result.systemMetrics().strategyName()).isEqualTo("System");
            assertThat(result.randomMetrics().strategyName()).isEqualTo("Random");
            assertThat(result.favoritesMetrics().strategyName()).isEqualTo("Always Favorites");
        }
    }
}
