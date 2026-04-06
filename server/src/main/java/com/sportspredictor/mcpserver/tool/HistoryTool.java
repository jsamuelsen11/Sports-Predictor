package com.sportspredictor.mcpserver.tool;

import com.sportspredictor.mcpserver.service.HistoryService;
import com.sportspredictor.mcpserver.service.HistoryService.BankrollAnalyticsResult;
import com.sportspredictor.mcpserver.service.HistoryService.BetHistoryResult;
import com.sportspredictor.mcpserver.service.HistoryService.BetSlipResult;
import com.sportspredictor.mcpserver.service.HistoryService.DailyPerformanceResult;
import java.math.BigDecimal;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Service;

/** MCP tools for bet history, bet slips, bankroll analytics, and daily performance. */
@Service
@RequiredArgsConstructor
public class HistoryTool {

    private final HistoryService historyService;

    /** Returns filtered bet history. */
    @Tool(
            name = "get_bet_history",
            description = "Get filtered bet history. Filter by sport, bet type, result status,"
                    + " date range, odds range, and stake range")
    public BetHistoryResult getBetHistory(
            @ToolParam(description = "Sport filter (e.g., nfl, nba)", required = false) String sport,
            @ToolParam(description = "Bet type filter (e.g., MONEYLINE, SPREAD, PARLAY)", required = false)
                    String betType,
            @ToolParam(description = "Status filter (e.g., WON, LOST, PENDING)", required = false) String status,
            @ToolParam(description = "Start date in ISO format (e.g., 2026-01-01)", required = false) String startDate,
            @ToolParam(description = "End date in ISO format (e.g., 2026-03-25)", required = false) String endDate,
            @ToolParam(description = "Minimum decimal odds filter", required = false) Double minOdds,
            @ToolParam(description = "Maximum decimal odds filter", required = false) Double maxOdds,
            @ToolParam(description = "Minimum stake filter", required = false) Double minStake,
            @ToolParam(description = "Maximum stake filter", required = false) Double maxStake) {

        return historyService.getBetHistory(
                sport,
                betType,
                status,
                startDate,
                endDate,
                minOdds != null ? BigDecimal.valueOf(minOdds) : null,
                maxOdds != null ? BigDecimal.valueOf(maxOdds) : null,
                minStake != null ? BigDecimal.valueOf(minStake) : null,
                maxStake != null ? BigDecimal.valueOf(maxStake) : null);
    }

    /** Returns full details of a single bet including all parlay legs. */
    @Tool(
            name = "get_bet_slip",
            description =
                    "Get full details of a single bet including all parlay legs, odds," + " and settlement information")
    public BetSlipResult getBetSlip(@ToolParam(description = "Bet ID (UUID)") String betId) {
        return historyService.getBetSlip(betId);
    }

    /** Returns advanced bankroll analytics. */
    @Tool(
            name = "get_bankroll_analytics",
            description = "Get advanced bankroll analytics: profit by sport, bet type, and month,"
                    + " win rate, average odds, and max drawdown")
    public BankrollAnalyticsResult getBankrollAnalytics() {
        return historyService.getBankrollAnalytics();
    }

    /** Returns today's betting performance card. */
    @Tool(
            name = "get_daily_performance",
            description = "Get today's betting card with placed, pending, settled counts and daily P/L")
    public DailyPerformanceResult getDailyPerformance() {
        return historyService.getDailyPerformance();
    }
}
