package com.sportspredictor.tool;

import com.sportspredictor.service.SportSpecificService;
import com.sportspredictor.service.SportSpecificService.DvoaResult;
import com.sportspredictor.service.SportSpecificService.FormTableResult;
import com.sportspredictor.service.SportSpecificService.GoalieResult;
import com.sportspredictor.service.SportSpecificService.KenpomResult;
import com.sportspredictor.service.SportSpecificService.SpPlusResult;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Service;

/** MCP tools for sport-specific analysis: NFL DVOA, NHL goalies, soccer form, CFB SP+, CBB KenPom. */
@Service
@RequiredArgsConstructor
public class SportSpecificTool {

    private final SportSpecificService sportSpecificService;

    /** Response records. */
    public record DvoaResponse(
            String teamId, double offDvoa, double defDvoa, double totalDvoa, int rank, String summary) {}

    /** Goalie confirmation response. */
    public record GoalieResponse(String teamId, String goalieName, String status, String summary) {}

    /** Form table response. */
    public record FormTableResponse(
            String teamId, int recentGames, int points, int goalsFor, int goalsAgainst, String summary) {}

    /** SP+ ratings response. */
    public record SpPlusResponse(
            String teamId, double offRating, double defRating, double overall, int rank, String summary) {}

    /** KenPom ratings response. */
    public record KenpomResponse(
            String teamId, double adjustedEm, double offEff, double defEff, double tempo, int rank, String summary) {}

    /** Gets NFL DVOA approximation ratings. */
    @Tool(name = "get_nfl_dvoa_ratings", description = "Efficiency ratings adjusted for opponent (DVOA approximation)")
    public DvoaResponse getNflDvoaRatings(@ToolParam(description = "Team ID") String teamId) {
        DvoaResult r = sportSpecificService.getNflDvoaRatings(teamId);
        return new DvoaResponse(r.teamId(), r.offensiveDvoa(), r.defensiveDvoa(), r.totalDvoa(), r.rank(), r.summary());
    }

    /** Confirms NHL starting goaltender. */
    @Tool(name = "get_nhl_goalie_confirmation", description = "Confirm starting goaltender for an NHL team")
    public GoalieResponse getNhlGoalieConfirmation(@ToolParam(description = "Team ID") String teamId) {
        GoalieResult r = sportSpecificService.getNhlGoalieConfirmation(teamId);
        return new GoalieResponse(r.teamId(), r.goalieName(), r.status(), r.summary());
    }

    /** Gets soccer form table for last N games. */
    @Tool(name = "get_soccer_form_table", description = "Last-N-games form table for a soccer team")
    public FormTableResponse getSoccerFormTable(
            @ToolParam(description = "Sport/league key (e.g., soccer_england_premier_league)") String sport,
            @ToolParam(description = "Team ID") String teamId,
            @ToolParam(description = "Number of recent games (e.g., 5)") int recentGames) {
        FormTableResult r = sportSpecificService.getSoccerFormTable(sport, teamId, recentGames);
        return new FormTableResponse(
                r.teamId(), r.recentGames(), r.points(), r.goalsFor(), r.goalsAgainst(), r.summary());
    }

    /** Gets CFB SP+ composite team ratings approximation. */
    @Tool(name = "get_cfb_sp_plus_ratings", description = "Composite efficiency ratings (SP+ approximation) for CFB")
    public SpPlusResponse getCfbSpPlusRatings(@ToolParam(description = "Team ID") String teamId) {
        SpPlusResult r = sportSpecificService.getCfbSpPlusRatings(teamId);
        return new SpPlusResponse(
                r.teamId(), r.offenseRating(), r.defenseRating(), r.overallRating(), r.rank(), r.summary());
    }

    /** Gets CBB KenPom-style adjusted efficiency metrics. */
    @Tool(name = "get_cbb_kenpom_ratings", description = "Adjusted efficiency metrics (KenPom approximation) for CBB")
    public KenpomResponse getCbbKenpomRatings(@ToolParam(description = "Team ID") String teamId) {
        KenpomResult r = sportSpecificService.getCbbKenpomRatings(teamId);
        return new KenpomResponse(
                r.teamId(),
                r.adjustedEfficiencyMargin(),
                r.offensiveEfficiency(),
                r.defensiveEfficiency(),
                r.tempo(),
                r.rank(),
                r.summary());
    }
}
