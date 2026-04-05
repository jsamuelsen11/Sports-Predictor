package com.sportspredictor.mcpserver.tool;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.sportspredictor.mcpserver.service.HistoryService;
import com.sportspredictor.mcpserver.service.HistoryService.BankrollAnalyticsResult;
import com.sportspredictor.mcpserver.service.HistoryService.BetHistoryResult;
import com.sportspredictor.mcpserver.service.HistoryService.BetSlipResult;
import com.sportspredictor.mcpserver.service.HistoryService.DailyPerformanceResult;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/** Tests for {@link HistoryTool}. */
@ExtendWith(MockitoExtension.class)
class HistoryToolTest {

    @Mock
    private HistoryService historyService;

    @InjectMocks
    private HistoryTool historyTool;

    /** Tests for {@link HistoryTool#getBetHistory}. */
    @Nested
    class GetBetHistory {

        @Test
        void delegatesToService() {
            when(historyService.getBetHistory(any(), any(), any(), any(), any(), any(), any(), any(), any()))
                    .thenReturn(new BetHistoryResult(List.of(), 0, "", "No bets"));

            BetHistoryResult result = historyTool.getBetHistory("nba", null, null, null, null, null, null, null, null);

            assertThat(result.totalCount()).isZero();
            verify(historyService).getBetHistory("nba", null, null, null, null, null, null, null, null);
        }
    }

    /** Tests for {@link HistoryTool#getBetSlip(String)}. */
    @Nested
    class GetBetSlip {

        @Test
        void delegatesToService() {
            when(historyService.getBetSlip("b1"))
                    .thenReturn(new BetSlipResult(
                            "b1",
                            "nba",
                            "MONEYLINE",
                            "WON",
                            new BigDecimal("100"),
                            new BigDecimal("1.909"),
                            new BigDecimal("190.90"),
                            new BigDecimal("190.90"),
                            "evt-1",
                            "Test bet",
                            Instant.now(),
                            Instant.now(),
                            null,
                            List.of(),
                            "Summary"));

            BetSlipResult result = historyTool.getBetSlip("b1");

            assertThat(result.betId()).isEqualTo("b1");
        }
    }

    /** Tests for {@link HistoryTool#getBankrollAnalytics()}. */
    @Nested
    class GetBankrollAnalytics {

        @Test
        void delegatesToService() {
            when(historyService.getBankrollAnalytics())
                    .thenReturn(new BankrollAnalyticsResult(
                            Map.of(), Map.of(), Map.of(), BigDecimal.ZERO, 0.0, BigDecimal.ZERO, 0, 0, 0, "No data"));

            BankrollAnalyticsResult result = historyTool.getBankrollAnalytics();

            assertThat(result.totalBets()).isZero();
        }
    }

    /** Tests for {@link HistoryTool#getDailyPerformance()}. */
    @Nested
    class GetDailyPerformance {

        @Test
        void delegatesToService() {
            when(historyService.getDailyPerformance())
                    .thenReturn(new DailyPerformanceResult(
                            "2026-03-25",
                            0,
                            0,
                            0,
                            0,
                            0,
                            0,
                            BigDecimal.ZERO,
                            new BigDecimal("1000"),
                            List.of(),
                            "Empty day"));

            DailyPerformanceResult result = historyTool.getDailyPerformance();

            assertThat(result.date()).isEqualTo("2026-03-25");
        }
    }
}
