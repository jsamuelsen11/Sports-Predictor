package com.sportspredictor.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import com.sportspredictor.entity.Bankroll;
import com.sportspredictor.entity.Bet;
import com.sportspredictor.entity.OddsSnapshot;
import com.sportspredictor.entity.enums.BetStatus;
import com.sportspredictor.entity.enums.BetType;
import com.sportspredictor.repository.BetRepository;
import com.sportspredictor.repository.OddsSnapshotRepository;
import com.sportspredictor.service.ClvService.ClvHistoryResult;
import com.sportspredictor.service.ClvService.ClvReportResult;
import com.sportspredictor.service.ClvService.SingleClvResult;
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

/** Tests for {@link ClvService}. */
@ExtendWith(MockitoExtension.class)
class ClvServiceTest {

    @Mock
    private BetRepository betRepository;

    @Mock
    private OddsSnapshotRepository oddsSnapshotRepository;

    @InjectMocks
    private ClvService clvService;

    private static Bankroll buildBankroll() {
        return Bankroll.builder()
                .id("br-1")
                .name("Test")
                .startingBalance(new BigDecimal("1000"))
                .currentBalance(new BigDecimal("900"))
                .createdAt(Instant.parse("2026-01-01T00:00:00Z"))
                .build();
    }

    private static Bet buildWonBet(String id, String eventId, String sport, BigDecimal decimalOdds) {
        return Bet.builder()
                .id(id)
                .bankroll(buildBankroll())
                .betType(BetType.MONEYLINE)
                .status(BetStatus.WON)
                .stake(new BigDecimal("100"))
                .odds(decimalOdds)
                .potentialPayout(new BigDecimal("190.90"))
                .sport(sport)
                .eventId(eventId)
                .description("Lakers ML")
                .placedAt(Instant.parse("2026-03-01T12:00:00Z"))
                .settledAt(Instant.parse("2026-03-01T22:00:00Z"))
                .build();
    }

    private static OddsSnapshot buildSnapshot(String id, String eventId, String oddsData) {
        return OddsSnapshot.builder()
                .id(id)
                .eventId(eventId)
                .sport("nba")
                .bookmaker("draftkings")
                .market("h2h")
                .oddsData(oddsData)
                .capturedAt(Instant.now())
                .build();
    }

    /** Tests for {@link ClvService#calculateClosingLineValue}. */
    @Nested
    class CalculateClosingLineValue {

        @Test
        void calculatesClvForBetWithClosingSnapshot() {
            Bet bet = buildWonBet("bet-1", "evt-1", "nba", new BigDecimal("1.909"));
            when(betRepository.findById("bet-1")).thenReturn(Optional.of(bet));
            // Snapshot shows closing line at -120 (less favourable than -110 placed)
            when(oddsSnapshotRepository.findByEventIdOrderByCapturedAtAsc("evt-1"))
                    .thenReturn(List.of(buildSnapshot("snap-1", "evt-1", "-120")));

            SingleClvResult result = clvService.calculateClosingLineValue("bet-1");

            assertThat(result.betId()).isEqualTo("bet-1");
            assertThat(result.closingOdds()).isEqualTo(-120);
            assertThat(result.summary()).contains("bet-1");
        }

        @Test
        void usesEvenOddsWhenNoSnapshotExists() {
            Bet bet = buildWonBet("bet-1", "evt-1", "nba", new BigDecimal("1.909"));
            when(betRepository.findById("bet-1")).thenReturn(Optional.of(bet));
            when(oddsSnapshotRepository.findByEventIdOrderByCapturedAtAsc("evt-1"))
                    .thenReturn(List.of());

            SingleClvResult result = clvService.calculateClosingLineValue("bet-1");

            // Closing odds should fall back to even (100)
            assertThat(result.closingOdds()).isEqualTo(100);
        }

        @Test
        void throwsWhenBetNotFound() {
            when(betRepository.findById("bad-id")).thenReturn(Optional.empty());

            assertThatThrownBy(() -> clvService.calculateClosingLineValue("bad-id"))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("bad-id");
        }
    }

    /** Tests for {@link ClvService#getClosingLineValueReport}. */
    @Nested
    class GetClosingLineValueReport {

        @Test
        void aggregatesClvAcrossMultipleBets() {
            Bet bet1 = buildWonBet("bet-1", "evt-1", "nba", new BigDecimal("1.909"));
            Bet bet2 = buildWonBet("bet-2", "evt-2", "nba", new BigDecimal("2.100"));
            when(betRepository.findByStatusInAndSettledAtBetween(any(), any(), any()))
                    .thenReturn(List.of(bet1, bet2));
            when(oddsSnapshotRepository.findByEventIdOrderByCapturedAtAsc(any()))
                    .thenReturn(List.of());

            ClvReportResult report = clvService.getClosingLineValueReport("2026-03-01", "2026-03-31", "nba");

            assertThat(report.totalBets()).isEqualTo(2);
            assertThat(report.bySport()).hasSize(1);
            assertThat(report.bySport().getFirst().sport()).isEqualTo("nba");
            assertThat(report.summary()).contains("2 bets");
        }

        @Test
        void returnsEmptyReportWhenNoBetsFound() {
            when(betRepository.findByStatusInAndSettledAtBetween(any(), any(), any()))
                    .thenReturn(List.of());

            ClvReportResult report = clvService.getClosingLineValueReport("2026-03-01", "2026-03-31", null);

            assertThat(report.totalBets()).isEqualTo(0);
            assertThat(report.bySport()).isEmpty();
            assertThat(report.summary()).containsIgnoringCase("no settled bets");
        }

        @Test
        void filtersBySportWhenProvided() {
            Bet nbaBet = buildWonBet("bet-1", "evt-1", "nba", new BigDecimal("1.909"));
            Bet nflBet = buildWonBet("bet-2", "evt-2", "nfl", new BigDecimal("1.909"));
            when(betRepository.findByStatusInAndSettledAtBetween(any(), any(), any()))
                    .thenReturn(List.of(nbaBet, nflBet));
            when(oddsSnapshotRepository.findByEventIdOrderByCapturedAtAsc(any()))
                    .thenReturn(List.of());

            ClvReportResult report = clvService.getClosingLineValueReport(null, null, "nba");

            assertThat(report.totalBets()).isEqualTo(1);
            assertThat(report.bySport().getFirst().sport()).isEqualTo("nba");
        }
    }

    /** Tests for {@link ClvService#compareToClosingLine}. */
    @Nested
    class CompareToClosingLine {

        @Test
        void returnsEmptyHistoryWhenNoBets() {
            when(betRepository.findByStatusInAndSettledAtBetween(any(), any(), any()))
                    .thenReturn(List.of());

            ClvHistoryResult history = clvService.compareToClosingLine("2026-03-01", "2026-03-31", null);

            assertThat(history.dataPoints()).isEmpty();
            assertThat(history.trendSlope()).isEqualTo(0.0);
        }

        @Test
        void groupsBetsByDateIntoDataPoints() {
            Bet bet = buildWonBet("bet-1", "evt-1", "nba", new BigDecimal("1.909"));
            when(betRepository.findByStatusInAndSettledAtBetween(any(), any(), any()))
                    .thenReturn(List.of(bet));
            when(oddsSnapshotRepository.findByEventIdOrderByCapturedAtAsc(eq("evt-1")))
                    .thenReturn(List.of(buildSnapshot("snap-1", "evt-1", "-110")));

            ClvHistoryResult history = clvService.compareToClosingLine(null, null, null);

            assertThat(history.dataPoints()).hasSize(1);
            assertThat(history.summary()).isNotBlank();
        }
    }
}
