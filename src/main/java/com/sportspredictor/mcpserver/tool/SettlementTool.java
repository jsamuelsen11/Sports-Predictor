package com.sportspredictor.mcpserver.tool;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sportspredictor.mcpserver.service.SettlementService;
import com.sportspredictor.mcpserver.service.SettlementService.AutoSettleResult;
import com.sportspredictor.mcpserver.service.SettlementService.LegSettlement;
import com.sportspredictor.mcpserver.service.SettlementService.LegSettlementDetail;
import com.sportspredictor.mcpserver.service.SettlementService.SettleBetResult;
import com.sportspredictor.mcpserver.service.SettlementService.SettleFuturesResult;
import com.sportspredictor.mcpserver.service.SettlementService.SettleParlayResult;
import com.sportspredictor.mcpserver.service.SettlementService.VoidLegResult;
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

    /** Response for void_leg. */
    public record VoidLegResponse(
            String betId, int legNumber, double newPotentialPayout, int remainingLegs, String summary) {}

    /** Response for settle_futures. */
    public record SettleFuturesResponse(int totalFutures, int settled, int won, int lost, String summary) {}

    /** Voids a single leg of a parlay (e.g., game cancelled). */
    @Tool(
            name = "void_leg",
            description = "Void a single parlay leg (e.g., game cancelled)."
                    + " Recalculates parlay odds with remaining legs")
    public VoidLegResponse voidLeg(
            @ToolParam(description = "Bet ID") String betId,
            @ToolParam(description = "Leg number to void") int legNumber) {
        VoidLegResult r = settlementService.voidLeg(betId, legNumber);
        return new VoidLegResponse(
                r.betId(), r.legNumber(), r.newPotentialPayout().doubleValue(), r.remainingLegs(), r.summary());
    }

    /** Settles a same-game parlay by providing the outcome for each leg. */
    @Tool(
            name = "settle_sgp",
            description = "Settle a same-game parlay (SGP) by providing outcome for each leg."
                    + " Same mechanics as settle_parlay but for SGP bets")
    public SettleParlayResponse settleSgp(
            @ToolParam(description = "SGP bet ID to settle") String betId,
            @ToolParam(
                            description = "JSON array of leg outcomes,"
                                    + " each with: legNumber (int), outcome (WON/LOST/PUSH),"
                                    + " resultDetail (string, optional)")
                    String legOutcomesJson) {

        List<LegSettlement> legOutcomes = parseLegOutcomes(legOutcomesJson);
        SettleParlayResult r = settlementService.settleSgp(betId, legOutcomes);
        return new SettleParlayResponse(
                r.betId(),
                r.newStatus(),
                r.stake().doubleValue(),
                r.payout().doubleValue(),
                r.legs(),
                r.balanceAfter().doubleValue(),
                r.summary());
    }

    /** Settles futures bets at end of season/tournament. */
    @Tool(
            name = "settle_futures",
            description = "Settle futures bets for a sport based on final outcome" + " (e.g., championship winner)")
    public SettleFuturesResponse settleFutures(
            @ToolParam(description = "Sport key") String sport,
            @ToolParam(description = "Winning outcome (e.g., 'Chiefs', 'Celtics')") String outcome) {
        SettleFuturesResult r = settlementService.settleFutures(sport, outcome);
        return new SettleFuturesResponse(r.totalFutures(), r.settled(), r.won(), r.lost(), r.summary());
    }

    private List<LegSettlement> parseLegOutcomes(String json) {
        try {
            return objectMapper.readValue(json, new TypeReference<>() {});
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException("Invalid leg outcomes JSON: " + e.getMessage(), e);
        }
    }
}
