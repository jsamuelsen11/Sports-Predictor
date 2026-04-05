package com.sportspredictor.mcpserver.tool;

import com.sportspredictor.mcpserver.service.BankrollRulesService;
import com.sportspredictor.mcpserver.service.BankrollRulesService.DailyLimitsResult;
import com.sportspredictor.mcpserver.service.BankrollRulesService.SetRulesResult;
import java.math.BigDecimal;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Service;

/** MCP tools for bankroll rules and daily limits management. */
@Service
@RequiredArgsConstructor
public class BankrollRulesTool {

    private final BankrollRulesService bankrollRulesService;

    /** Response for set_bankroll_rules. */
    public record SetRulesResponse(
            String rulesId,
            Double maxBetUnits,
            Double dailyExposureLimit,
            Double stopLossThreshold,
            Integer maxParlayLegs,
            Double minConfidence,
            Double unitSize,
            String summary) {}

    /** Response for get_daily_limits_status. */
    public record DailyLimitsResponse(
            double dailyExposureLimit,
            double wageredToday,
            double remainingCapacity,
            double stopLossThreshold,
            double dailyPnl,
            boolean stopLossTriggered,
            double maxBetUnits,
            double unitSize,
            double maxBetAmount,
            Integer maxParlayLegs,
            String summary) {}

    /** Configures bankroll wagering rules and limits. */
    @Tool(
            name = "set_bankroll_rules",
            description = "Configure bankroll rules: max bet size (in units), daily exposure limit,"
                    + " stop-loss threshold, max parlay legs, minimum confidence for auto-bets, and unit size")
    public SetRulesResponse setBankrollRules(
            @ToolParam(description = "Max single bet in units (e.g., 3.0 = 3 units)", required = false)
                    Double maxBetUnits,
            @ToolParam(description = "Max total daily wagering in dollars", required = false) Double dailyExposureLimit,
            @ToolParam(description = "Stop betting after losing this amount today", required = false)
                    Double stopLossThreshold,
            @ToolParam(description = "Max legs allowed in a parlay", required = false) Integer maxParlayLegs,
            @ToolParam(description = "Min confidence (0.0-1.0) for auto-bet suggestions", required = false)
                    Double minConfidence,
            @ToolParam(description = "Unit size in dollars (e.g., 25.0)", required = false) Double unitSize) {

        SetRulesResult r = bankrollRulesService.setRules(
                toBd(maxBetUnits),
                toBd(dailyExposureLimit),
                toBd(stopLossThreshold),
                maxParlayLegs,
                toBd(minConfidence),
                toBd(unitSize));

        return new SetRulesResponse(
                r.rulesId(),
                toDouble(r.maxBetUnits()),
                toDouble(r.dailyExposureLimit()),
                toDouble(r.stopLossThreshold()),
                r.maxParlayLegs(),
                toDouble(r.minConfidence()),
                toDouble(r.unitSize()),
                r.summary());
    }

    /** Checks remaining daily wagering capacity against configured limits. */
    @Tool(
            name = "get_daily_limits_status",
            description = "Check remaining daily wagering capacity, stop-loss status,"
                    + " and other configured bankroll limits")
    public DailyLimitsResponse getDailyLimitsStatus() {
        DailyLimitsResult r = bankrollRulesService.getDailyLimitsStatus();
        return new DailyLimitsResponse(
                r.dailyExposureLimit().doubleValue(),
                r.wageredToday().doubleValue(),
                r.remainingCapacity().doubleValue(),
                r.stopLossThreshold().doubleValue(),
                r.dailyPnl().doubleValue(),
                r.stopLossTriggered(),
                r.maxBetUnits().doubleValue(),
                r.unitSize().doubleValue(),
                r.maxBetAmount().doubleValue(),
                r.maxParlayLegs(),
                r.summary());
    }

    private static BigDecimal toBd(Double value) {
        return value != null ? BigDecimal.valueOf(value) : null;
    }

    private static Double toDouble(BigDecimal value) {
        return value != null ? value.doubleValue() : null;
    }
}
