package com.sportspredictor.mcpserver.tool;

import com.sportspredictor.mcpserver.service.MonteCarloService;
import com.sportspredictor.mcpserver.service.MonteCarloService.ConfidenceInterval;
import com.sportspredictor.mcpserver.service.MonteCarloService.ScoreDistribution;
import com.sportspredictor.mcpserver.service.MonteCarloService.SimulationOutput;
import com.sportspredictor.mcpserver.service.MonteCarloService.SpreadDistribution;
import com.sportspredictor.mcpserver.service.MonteCarloService.TotalDistribution;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Service;

/** MCP tool for Monte Carlo game simulation. */
@Service
@RequiredArgsConstructor
public class MonteCarloTool {

    private final MonteCarloService monteCarloService;

    /** Response for simulate_game. */
    public record SimulateGameResponse(
            String sport,
            String team1Id,
            String team2Id,
            int numSimulations,
            ScoreDistribution team1Scores,
            ScoreDistribution team2Scores,
            double team1WinProbability,
            double team2WinProbability,
            double drawProbability,
            SpreadDistribution spreadDistribution,
            TotalDistribution totalDistribution,
            ConfidenceInterval confidenceInterval,
            String summary) {}

    /** Runs a Monte Carlo simulation for a game between two teams. */
    @Tool(
            name = "simulate_game",
            description = "Run a Monte Carlo simulation for a game between two teams."
                    + " Produces score distributions, win probabilities, and spread/total"
                    + " distributions with confidence intervals")
    public SimulateGameResponse simulateGame(
            @ToolParam(description = "Sport key (e.g., nfl, nba, mlb, nhl)") String sport,
            @ToolParam(description = "Team 1 ID") String team1Id,
            @ToolParam(description = "Team 2 ID") String team2Id,
            @ToolParam(description = "Number of simulations (default 10000, max 100000)", required = false)
                    Integer numSimulations) {

        SimulationOutput r = monteCarloService.simulateGame(sport, team1Id, team2Id, numSimulations);

        return new SimulateGameResponse(
                r.sport(),
                r.team1Id(),
                r.team2Id(),
                r.numSimulations(),
                r.team1Scores(),
                r.team2Scores(),
                r.team1WinProbability(),
                r.team2WinProbability(),
                r.drawProbability(),
                r.spreadDistribution(),
                r.totalDistribution(),
                r.confidenceInterval(),
                r.summary());
    }
}
