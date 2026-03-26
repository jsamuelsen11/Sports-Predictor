package com.sportspredictor.tool;

import com.sportspredictor.service.BankrollService;
import com.sportspredictor.service.BankrollService.BankrollStatusResult;
import com.sportspredictor.service.BankrollService.DepositResult;
import com.sportspredictor.service.BankrollService.ResetBankrollResult;
import com.sportspredictor.service.BankrollService.WithdrawResult;
import java.math.BigDecimal;
import java.time.Instant;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Service;

/** MCP tools for bankroll management: status, reset, deposit, and withdraw. */
@Service
@RequiredArgsConstructor
public class BankrollTool {

    private final BankrollService bankrollService;

    /** Bankroll status response. */
    public record BankrollStatusResponse(
            String bankrollId,
            String name,
            double startingBalance,
            double currentBalance,
            double totalWagered,
            double totalWon,
            double totalLost,
            double netProfitLoss,
            double roiPercent,
            int totalBets,
            int wins,
            int losses,
            int pushes,
            int pending,
            int currentStreak,
            String streakType,
            Instant createdAt,
            String summary) {}

    /** Response for bankroll reset. */
    public record ResetBankrollResponse(
            String archivedBankrollId, String newBankrollId, double startingBalance, String summary) {}

    /** Response for deposit. */
    public record DepositResponse(String transactionId, double amount, double balanceAfter, String summary) {}

    /** Response for withdrawal. */
    public record WithdrawResponse(String transactionId, double amount, double balanceAfter, String summary) {}

    /** Returns current bankroll status including balance, P/L, ROI%, win rate, streak, and pending bet count. */
    @Tool(
            name = "get_bankroll_status",
            description = "Get current bankroll status including balance, P/L, ROI%, win rate, streak,"
                    + " and pending bet count")
    public BankrollStatusResponse getBankrollStatus() {
        BankrollStatusResult r = bankrollService.getBankrollStatus();
        return new BankrollStatusResponse(
                r.bankrollId(),
                r.name(),
                r.startingBalance().doubleValue(),
                r.currentBalance().doubleValue(),
                r.totalWagered().doubleValue(),
                r.totalWon().doubleValue(),
                r.totalLost().doubleValue(),
                r.netProfitLoss().doubleValue(),
                r.roiPercent().doubleValue(),
                r.totalBets(),
                r.wins(),
                r.losses(),
                r.pushes(),
                r.pending(),
                r.currentStreak(),
                r.streakType(),
                r.createdAt(),
                r.summary());
    }

    /** Archives current bankroll and starts fresh with a new starting balance. */
    @Tool(
            name = "reset_bankroll",
            description = "Archive current bankroll and start fresh with a new starting balance."
                    + " The old bankroll is preserved as a historical season")
    public ResetBankrollResponse resetBankroll(
            @ToolParam(description = "Starting balance for the new bankroll (e.g., 1000.00)") double startingAmount) {
        ResetBankrollResult r = bankrollService.resetBankroll(BigDecimal.valueOf(startingAmount));
        return new ResetBankrollResponse(
                r.archivedBankrollId(), r.newBankrollId(), r.startingBalance().doubleValue(), r.summary());
    }

    /** Adds funds to the active bankroll. */
    @Tool(name = "deposit_funds", description = "Add funds to the active bankroll")
    public DepositResponse depositFunds(
            @ToolParam(description = "Amount to deposit (must be positive)") double amount) {
        DepositResult r = bankrollService.depositFunds(BigDecimal.valueOf(amount));
        return new DepositResponse(
                r.transactionId(), r.amount().doubleValue(), r.balanceAfter().doubleValue(), r.summary());
    }

    /** Withdraws funds from the active bankroll. */
    @Tool(name = "withdraw_funds", description = "Withdraw funds from the active bankroll")
    public WithdrawResponse withdrawFunds(
            @ToolParam(description = "Amount to withdraw (must be positive, cannot exceed current balance)")
                    double amount) {
        WithdrawResult r = bankrollService.withdrawFunds(BigDecimal.valueOf(amount));
        return new WithdrawResponse(
                r.transactionId(), r.amount().doubleValue(), r.balanceAfter().doubleValue(), r.summary());
    }
}
