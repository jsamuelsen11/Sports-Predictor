package com.sportspredictor.mcpserver.tool;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sportspredictor.mcpserver.service.BettingService;
import com.sportspredictor.mcpserver.service.BettingService.CancelBetResult;
import com.sportspredictor.mcpserver.service.BettingService.CashOutResult;
import com.sportspredictor.mcpserver.service.BettingService.EditBetResult;
import com.sportspredictor.mcpserver.service.BettingService.LegSummary;
import com.sportspredictor.mcpserver.service.BettingService.ParlayLegInput;
import com.sportspredictor.mcpserver.service.BettingService.PlaceBetResult;
import com.sportspredictor.mcpserver.service.BettingService.PlaceFuturesResult;
import com.sportspredictor.mcpserver.service.BettingService.PlaceParlayResult;
import com.sportspredictor.mcpserver.service.BettingService.PlaceRoundRobinResult;
import com.sportspredictor.mcpserver.service.BettingService.PlaceSgpResult;
import com.sportspredictor.mcpserver.service.BettingService.PlaceTeaserResult;
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

    /** Response for a placed teaser bet. */
    public record PlaceTeaserResponse(
            String betId,
            double stake,
            int legCount,
            double teaserPoints,
            int teaserOdds,
            double potentialPayout,
            List<LegSummary> legs,
            double balanceAfter,
            String summary) {}

    /** Response for a placed round-robin bet. */
    public record PlaceRoundRobinResponse(
            String parentBetId,
            double totalStake,
            int totalCombinations,
            int parlaySize,
            double stakePerCombo,
            double maxPotentialPayout,
            List<String> subBetIds,
            double balanceAfter,
            String summary) {}

    /** Response for a placed futures bet. */
    public record PlaceFuturesResponse(
            String betId,
            String sport,
            String eventId,
            String selection,
            int americanOdds,
            double stake,
            double potentialPayout,
            String expiresAt,
            double balanceAfter,
            String summary) {}

    /** Response for a cashed-out bet. */
    public record CashOutResponse(
            String betId,
            double originalStake,
            double cashOutAmount,
            double profit,
            double balanceAfter,
            String summary) {}

    /** Response for a placed SGP bet. */
    public record PlaceSgpResponse(
            String betId,
            double stake,
            double adjustedDecimalOdds,
            int adjustedAmericanOdds,
            double potentialPayout,
            List<LegSummary> legs,
            double correlationAdjustment,
            double balanceAfter,
            String summary) {}

    /** Response for an edited bet. */
    public record EditBetResponse(
            String betId,
            double oldStake,
            double newStake,
            double stakeDelta,
            double newPotentialPayout,
            double balanceAfter,
            String summary) {}

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

    /** Places a game prop bet. */
    @Tool(name = "place_game_prop", description = "Place a game prop bet (e.g., first team to score, will there be OT)")
    public PlaceBetResponse placeGameProp(
            @ToolParam(description = "Sport key (e.g., nfl, nba)") String sport,
            @ToolParam(description = "Event ID") String eventId,
            @ToolParam(description = "Selection (e.g., 'First TD scorer: P.Mahomes')") String selection,
            @ToolParam(description = "American odds") int odds,
            @ToolParam(description = "Stake amount in dollars") double stake,
            @ToolParam(description = "Bet description") String description) {

        PlaceBetResult r = bettingService.placeGamePropBet(
                sport, eventId, selection, odds, BigDecimal.valueOf(stake), description);
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

    /** Places a first-half or first-5-innings bet. */
    @Tool(
            name = "place_first_half_bet",
            description = "Place a first-half or first-5-innings bet. Settled on halftime/5th-inning score")
    public PlaceBetResponse placeFirstHalfBet(
            @ToolParam(description = "Sport key") String sport,
            @ToolParam(description = "Event ID") String eventId,
            @ToolParam(description = "Selection (e.g., 'Lakers 1H ML')") String selection,
            @ToolParam(description = "American odds") int odds,
            @ToolParam(description = "Stake amount in dollars") double stake,
            @ToolParam(description = "Bet description") String description) {

        PlaceBetResult r = bettingService.placeFirstHalfBet(
                sport, eventId, selection, odds, BigDecimal.valueOf(stake), description);
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

    /** Places a futures bet with optional expiration. */
    @Tool(
            name = "place_futures",
            description = "Place a futures bet on a season outcome (e.g., Super Bowl winner, MVP)."
                    + " Odds locked at placement, settled at season end")
    public PlaceFuturesResponse placeFutures(
            @ToolParam(description = "Sport key") String sport,
            @ToolParam(description = "Event/market ID (e.g., 'superbowl-2026', 'nba-mvp-2026')") String eventId,
            @ToolParam(description = "Selection (e.g., 'Chiefs to win Super Bowl')") String selection,
            @ToolParam(description = "American odds") int odds,
            @ToolParam(description = "Stake amount in dollars") double stake,
            @ToolParam(description = "Bet description") String description,
            @ToolParam(description = "Expiration date in ISO-8601 (e.g., 2026-02-08T00:00:00Z)", required = false)
                    String expiresAt) {

        java.time.Instant expiry = expiresAt != null ? java.time.Instant.parse(expiresAt) : null;
        PlaceFuturesResult r = bettingService.placeFuturesBet(
                sport, eventId, selection, odds, BigDecimal.valueOf(stake), description, expiry);
        return new PlaceFuturesResponse(
                r.betId(),
                r.sport(),
                r.eventId(),
                r.selection(),
                r.americanOdds(),
                r.stake().doubleValue(),
                r.potentialPayout().doubleValue(),
                r.expiresAt(),
                r.balanceAfter().doubleValue(),
                r.summary());
    }

    /** Places a teaser bet with adjusted spreads/totals. */
    @Tool(
            name = "place_teaser",
            description = "Place a teaser bet. Adjusts spreads/totals in your favor at reduced odds."
                    + " All legs must hit. Standard NFL: 6/6.5/7 points, NBA: 4/4.5/5 points")
    public PlaceTeaserResponse placeTeaser(
            @ToolParam(description = "JSON array of leg objects (same format as place_parlay)") String legsJson,
            @ToolParam(description = "Stake amount in dollars") double stake,
            @ToolParam(description = "Teaser points (e.g., 6.0, 6.5, 7.0)") double teaserPoints,
            @ToolParam(description = "Teaser description", required = false) String description) {

        List<ParlayLegInput> legs = parseLegsJson(legsJson);
        PlaceTeaserResult r = bettingService.placeTeaserBet(legs, BigDecimal.valueOf(stake), teaserPoints, description);
        return new PlaceTeaserResponse(
                r.betId(),
                r.stake().doubleValue(),
                r.legCount(),
                r.teaserPoints(),
                r.teaserOdds(),
                r.potentialPayout().doubleValue(),
                r.legs(),
                r.balanceAfter().doubleValue(),
                r.summary());
    }

    /** Places a round-robin bet generating all sub-parlay combinations. */
    @Tool(
            name = "place_round_robin",
            description = "Place a round-robin bet. Generates all possible parlay combinations"
                    + " of a given size from your selections. Each sub-parlay settles independently")
    public PlaceRoundRobinResponse placeRoundRobin(
            @ToolParam(description = "JSON array of leg objects (same format as place_parlay)") String legsJson,
            @ToolParam(description = "Number of legs per sub-parlay (e.g., 2 for two-team RR)") int parlaySize,
            @ToolParam(description = "Stake per sub-parlay in dollars") double stakePerCombo,
            @ToolParam(description = "Round-robin description", required = false) String description) {

        List<ParlayLegInput> legs = parseLegsJson(legsJson);
        PlaceRoundRobinResult r =
                bettingService.placeRoundRobinBet(legs, parlaySize, BigDecimal.valueOf(stakePerCombo), description);
        return new PlaceRoundRobinResponse(
                r.parentBetId(),
                r.totalStake().doubleValue(),
                r.totalCombinations(),
                r.parlaySize(),
                r.stakePerCombo().doubleValue(),
                r.maxPotentialPayout().doubleValue(),
                r.subBetIds(),
                r.balanceAfter().doubleValue(),
                r.summary());
    }

    /** Places a live/in-game bet with odds captured at the current moment. */
    @Tool(
            name = "place_live_bet",
            description = "Place a live/in-game bet. Odds are captured at the moment of placement."
                    + " Works the same as place_bet but marks the bet as live")
    public PlaceBetResponse placeLiveBet(
            @ToolParam(description = "Sport key (e.g., nfl, nba)") String sport,
            @ToolParam(description = "Event ID for the live game") String eventId,
            @ToolParam(description = "Bet type: MONEYLINE, SPREAD, TOTAL, PLAYER_PROP") String betType,
            @ToolParam(description = "Selection description") String selection,
            @ToolParam(description = "American odds at time of placement") int odds,
            @ToolParam(description = "Stake amount in dollars") double stake,
            @ToolParam(description = "Bet description") String description,
            @ToolParam(description = "Optional metadata JSON", required = false) String metadata) {

        PlaceBetResult r = bettingService.placeLiveBet(
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

    /** Cashes out a pending bet at a reduced value based on current game state. */
    @Tool(
            name = "cash_out_bet",
            description = "Cash out a pending bet early at a reduced value."
                    + " Credits the cash-out amount to your bankroll")
    public CashOutResponse cashOutBet(@ToolParam(description = "Bet ID to cash out") String betId) {
        CashOutResult r = bettingService.cashOutBet(betId);
        return new CashOutResponse(
                r.betId(),
                r.originalStake().doubleValue(),
                r.cashOutAmount().doubleValue(),
                r.profit().doubleValue(),
                r.balanceAfter().doubleValue(),
                r.summary());
    }

    /** Places a same-game parlay with correlation-adjusted odds. */
    @Tool(
            name = "place_sgp",
            description = "Place a same-game parlay (SGP). All legs must be from the same game."
                    + " Odds are adjusted for correlation between legs — use analyze_correlations first")
    public PlaceSgpResponse placeSgp(
            @ToolParam(description = "Event ID — all legs must be from this game") String eventId,
            @ToolParam(description = "JSON array of leg objects (same format as place_parlay)") String legsJson,
            @ToolParam(description = "Stake amount in dollars") double stake,
            @ToolParam(description = "SGP description", required = false) String description,
            @ToolParam(description = "Correlation adjustment factor from analyze_correlations (0.0-1.0)")
                    double correlationAdjustment) {

        List<ParlayLegInput> legs = parseLegsJson(legsJson);
        PlaceSgpResult r = bettingService.placeSgpBet(
                legs, BigDecimal.valueOf(stake), description, null, eventId, correlationAdjustment);
        return new PlaceSgpResponse(
                r.betId(),
                r.stake().doubleValue(),
                r.adjustedDecimalOdds().doubleValue(),
                r.adjustedAmericanOdds(),
                r.potentialPayout().doubleValue(),
                r.legs(),
                r.correlationAdjustment(),
                r.balanceAfter().doubleValue(),
                r.summary());
    }

    /** Edits a pending bet's stake before the game starts. */
    @Tool(
            name = "edit_bet",
            description = "Modify the stake on a pending bet before the game starts."
                    + " Recalculates potential payout and adjusts bankroll")
    public EditBetResponse editBet(
            @ToolParam(description = "Bet ID to edit") String betId,
            @ToolParam(description = "New stake amount in dollars") double newStake) {

        EditBetResult r = bettingService.editBet(betId, BigDecimal.valueOf(newStake));
        return new EditBetResponse(
                r.betId(),
                r.oldStake().doubleValue(),
                r.newStake().doubleValue(),
                r.stakeDelta().doubleValue(),
                r.newPotentialPayout().doubleValue(),
                r.balanceAfter().doubleValue(),
                r.summary());
    }

    private List<ParlayLegInput> parseLegsJson(String legsJson) {
        try {
            return objectMapper.readValue(legsJson, new TypeReference<>() {});
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException("Invalid legs JSON: " + e.getMessage(), e);
        }
    }
}
