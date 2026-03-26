package com.sportspredictor.tool;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sportspredictor.service.BettingService;
import com.sportspredictor.service.BettingService.CancelBetResult;
import com.sportspredictor.service.BettingService.LegSummary;
import com.sportspredictor.service.BettingService.ParlayLegInput;
import com.sportspredictor.service.BettingService.PlaceBetResult;
import com.sportspredictor.service.BettingService.PlaceParlayResult;
import java.math.BigDecimal;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Service;

/** MCP tools for placing and cancelling bets. */
@Service
@RequiredArgsConstructor
public class BettingTool {

    private final BettingService bettingService;
    private final ObjectMapper objectMapper;

    /** Response for a placed single bet. */
    public record PlaceBetResponse(
            String betId,
            String sport,
            String eventId,
            String betType,
            String selection,
            String description,
            double stake,
            int americanOdds,
            double decimalOdds,
            double potentialPayout,
            double balanceAfter,
            String summary) {}

    /** Response for a placed parlay bet. */
    public record PlaceParlayResponse(
            String betId,
            double stake,
            double combinedDecimalOdds,
            int combinedAmericanOdds,
            double potentialPayout,
            List<LegSummary> legs,
            double balanceAfter,
            String summary) {}

    /** Response for a cancelled bet. */
    public record CancelBetResponse(String betId, double refundedStake, double balanceAfter, String summary) {}

    /** Places a single bet (moneyline, spread, total, player prop, etc.). */
    @Tool(
            name = "place_bet",
            description = "Place a single bet (moneyline, spread, total, player prop, etc.)."
                    + " Locks in odds, deducts stake from bankroll, and returns a bet slip ID")
    public PlaceBetResponse placeBet(
            @ToolParam(description = "Sport key (e.g., nfl, nba, mlb, nhl)") String sport,
            @ToolParam(description = "Event ID for the game") String eventId,
            @ToolParam(
                            description = "Bet type: MONEYLINE, SPREAD, TOTAL,"
                                    + " PLAYER_PROP, GAME_PROP, FUTURES, LIVE, FIRST_HALF")
                    String betType,
            @ToolParam(description = "Selection description (e.g., 'Lakers ML', 'Over 220.5')") String selection,
            @ToolParam(description = "American odds (e.g., -110, +150)") int odds,
            @ToolParam(description = "Stake amount in dollars (e.g., 50.0)") double stake,
            @ToolParam(description = "Bet description") String description,
            @ToolParam(description = "Optional metadata JSON string", required = false) String metadata) {

        PlaceBetResult r = bettingService.placeBet(
                sport, eventId, betType, selection, odds, BigDecimal.valueOf(stake), description, metadata);

        return new PlaceBetResponse(
                r.betId(),
                r.sport(),
                r.eventId(),
                r.betType(),
                r.selection(),
                r.description(),
                r.stake().doubleValue(),
                r.americanOdds(),
                r.decimalOdds().doubleValue(),
                r.potentialPayout().doubleValue(),
                r.balanceAfter().doubleValue(),
                r.summary());
    }

    /** Places a parlay bet with 2+ legs. */
    @Tool(
            name = "place_parlay",
            description = "Place a parlay bet with 2+ legs. Calculates combined odds and potential payout."
                    + " All legs must win for the parlay to pay out")
    public PlaceParlayResponse placeParlay(
            @ToolParam(
                            description = "JSON array of leg objects, each with: sport, eventId, selection,"
                                    + " americanOdds (e.g., [{\"sport\":\"nba\",\"eventId\":\"evt-1\","
                                    + "\"selection\":\"Lakers ML\",\"americanOdds\":-150}])")
                    String legsJson,
            @ToolParam(description = "Stake amount in dollars") double stake,
            @ToolParam(description = "Parlay description") String description,
            @ToolParam(description = "Optional metadata JSON", required = false) String metadata) {

        List<ParlayLegInput> legs = parseLegsJson(legsJson);
        PlaceParlayResult r = bettingService.placeParlayBet(legs, BigDecimal.valueOf(stake), description, metadata);

        return new PlaceParlayResponse(
                r.betId(),
                r.stake().doubleValue(),
                r.combinedDecimalOdds().doubleValue(),
                r.combinedAmericanOdds(),
                r.potentialPayout().doubleValue(),
                r.legs(),
                r.balanceAfter().doubleValue(),
                r.summary());
    }

    /** Cancels a pending bet and refunds the stake to the bankroll. */
    @Tool(
            name = "cancel_bet",
            description = "Cancel a pending bet and refund the stake to the bankroll."
                    + " Only works for bets that have not been settled")
    public CancelBetResponse cancelBet(@ToolParam(description = "Bet ID to cancel") String betId) {
        CancelBetResult r = bettingService.cancelBet(betId);
        return new CancelBetResponse(
                r.betId(), r.refundedStake().doubleValue(), r.balanceAfter().doubleValue(), r.summary());
    }

    private List<ParlayLegInput> parseLegsJson(String legsJson) {
        try {
            return objectMapper.readValue(legsJson, new TypeReference<>() {});
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException("Invalid legs JSON: " + e.getMessage(), e);
        }
    }
}
