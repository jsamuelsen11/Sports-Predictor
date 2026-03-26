package com.sportspredictor.tool;

import com.sportspredictor.service.AdvancedAnalysisService;
import com.sportspredictor.service.AdvancedAnalysisService.PaceAndTotalsResult;
import com.sportspredictor.service.AdvancedAnalysisService.RestAndTravelResult;
import com.sportspredictor.service.AdvancedAnalysisService.SituationalResult;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Service;

/** MCP tools for advanced game analysis: rest/travel, pace/totals, situational stats. */
@Service
@RequiredArgsConstructor
public class AdvancedAnalysisTool {

    private final AdvancedAnalysisService advancedAnalysisService;

    /** Response for analyze_rest_and_travel. */
    public record RestAndTravelResponse(
            String teamId,
            int restDays,
            boolean isBackToBack,
            String travelEstimate,
            int timezoneChanges,
            String summary) {}

    /** Response for analyze_pace_and_totals. */
    public record PaceAndTotalsResponse(
            String team1Id,
            String team2Id,
            double team1Pace,
            double team2Pace,
            double projectedTotal,
            String paceAdvantage,
            String summary) {}

    /** Response for analyze_situational_stats. */
    public record SituationalResponse(
            String teamId,
            String situation,
            int wins,
            int losses,
            double coverRate,
            double avgMargin,
            String summary) {}

    /** Analyzes rest days, travel distance, timezone changes, and back-to-backs. */
    @Tool(
            name = "analyze_rest_and_travel",
            description = "Analyze rest days, travel burden, timezone changes, and back-to-back detection")
    public RestAndTravelResponse analyzeRestAndTravel(
            @ToolParam(description = "Sport key") String sport, @ToolParam(description = "Team ID") String teamId) {
        RestAndTravelResult r = advancedAnalysisService.analyzeRestAndTravel(sport, teamId);
        return new RestAndTravelResponse(
                r.teamId(), r.restDays(), r.isBackToBack(), r.travelEstimate(), r.timezoneChanges(), r.summary());
    }

    /** Projects game pace and total based on matchup. */
    @Tool(
            name = "analyze_pace_and_totals",
            description = "Project game pace and total based on team matchup tempo data")
    public PaceAndTotalsResponse analyzePaceAndTotals(
            @ToolParam(description = "Sport key") String sport,
            @ToolParam(description = "Team 1 ID") String team1Id,
            @ToolParam(description = "Team 2 ID") String team2Id) {
        PaceAndTotalsResult r = advancedAnalysisService.analyzePaceAndTotals(sport, team1Id, team2Id);
        return new PaceAndTotalsResponse(
                r.team1Id(),
                r.team2Id(),
                r.team1Pace(),
                r.team2Pace(),
                r.projectedTotal(),
                r.paceAdvantage(),
                r.summary());
    }

    /** Analyzes performance as favorite/underdog, indoor/outdoor, after loss, etc. */
    @Tool(
            name = "analyze_situational_stats",
            description = "Analyze team performance in specific situations:"
                    + " home/away, favorite/underdog, after loss, indoor/outdoor")
    public SituationalResponse analyzeSituationalStats(
            @ToolParam(description = "Sport key") String sport,
            @ToolParam(description = "Team ID") String teamId,
            @ToolParam(description = "Situation: home, away, favorite, underdog, after_loss, indoor, outdoor")
                    String situation) {
        SituationalResult r = advancedAnalysisService.analyzeSituationalStats(sport, teamId, situation);
        return new SituationalResponse(
                r.teamId(), r.situation(), r.wins(), r.losses(), r.coverRate(), r.avgMargin(), r.summary());
    }
}
