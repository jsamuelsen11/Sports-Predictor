package com.sportspredictor.tool;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sportspredictor.service.SettlementService;
import com.sportspredictor.service.SettlementService.AutoSettleResult;
import com.sportspredictor.service.SettlementService.LegSettlement;
import com.sportspredictor.service.SettlementService.LegSettlementDetail;
import com.sportspredictor.service.SettlementService.SettleBetResult;
import com.sportspredictor.service.SettlementService.SettleParlayResult;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Service;

/** MCP tools for settling bets: single, parlay, and auto-settle. */
@Service
@RequiredArgsConstructor
public class SettlementTool {

    private final SettlementService settlementService;
    private final ObjectMapper objectMapper;

    /** Response for a settled single bet. */
    public record SettleBetResponse(
            String betId,
            String previousStatus,
            String newStatus,
            double stake,
            double payout,
            double balanceAfter,
            String summary) {}

    /** Response for a settled parlay. */
    public record SettleParlayResponse(
            String betId,
            String newStatus,
            double stake,
            double payout,
            List<LegSettlementDetail> legs,
            double balanceAfter,
            String summary) {}

    /** Response for auto-settle. */
    public record AutoSettleResponse(
            int totalPending,
            int matched,
            int settled,
            int won,
            int lost,
            int pushed,
            int errors,
            List<SettlementService.AutoSettledBet> settledBets,
            String summary) {}

    /** Settles a pending bet as WON, LOST, or PUSH. */
    @Tool(
            name = "settle_bet",
            description = "Settle a pending bet as WON, LOST, or PUSH."
                    + " Calculates payout and updates bankroll accordingly")
    public SettleBetResponse settleBet(
            @ToolParam(description = "Bet ID to settle") String betId,
            @ToolParam(description = "Settlement outcome: WON, LOST, or PUSH") String outcome) {

        SettleBetResult r = settlementService.settleBet(betId, outcome);
        return new SettleBetResponse(
                r.betId(),
                r.previousStatus(),
                r.newStatus(),
                r.stake().doubleValue(),
                r.payout().doubleValue(),
                r.balanceAfter().doubleValue(),
                r.summary());
    }

    /** Settles a parlay bet by providing the outcome for each leg. */
    @Tool(
            name = "settle_parlay",
            description = "Settle a parlay bet by providing the outcome for each leg."
                    + " One loss = parlay lost, all wins = full payout,"
                    + " pushes reduce the parlay")
    public SettleParlayResponse settleParlay(
            @ToolParam(description = "Parlay bet ID to settle") String betId,
            @ToolParam(
                            description = "JSON array of leg outcomes,"
                                    + " each with: legNumber (int), outcome (WON/LOST/PUSH),"
                                    + " resultDetail (string, optional)")
                    String legOutcomesJson) {

        List<LegSettlement> legOutcomes = parseLegOutcomes(legOutcomesJson);
        SettleParlayResult r = settlementService.settleParlay(betId, legOutcomes);
        return new SettleParlayResponse(
                r.betId(),
                r.newStatus(),
                r.stake().doubleValue(),
                r.payout().doubleValue(),
                r.legs(),
                r.balanceAfter().doubleValue(),
                r.summary());
    }

    /** Auto-settles all pending bets whose games have completed. */
    @Tool(
            name = "auto_settle_bets",
            description = "Automatically settle all pending bets whose games"
                    + " have completed. Fetches results and matches them"
                    + " to pending bets")
    public AutoSettleResponse autoSettleBets(
            @ToolParam(
                            description = "Sport filter to only settle bets for a specific" + " sport (e.g., nba)",
                            required = false)
                    String sport) {

        AutoSettleResult r = settlementService.autoSettleBets(sport);
        return new AutoSettleResponse(
                r.totalPending(),
                r.matched(),
                r.settled(),
                r.won(),
                r.lost(),
                r.pushed(),
                r.errors(),
                r.settledBets(),
                r.summary());
    }

    private List<LegSettlement> parseLegOutcomes(String json) {
        try {
            return objectMapper.readValue(json, new TypeReference<>() {});
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException("Invalid leg outcomes JSON: " + e.getMessage(), e);
        }
    }
}
