package com.sportspredictor.resource;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sportspredictor.service.BankrollService;
import com.sportspredictor.service.HistoryService;
import io.modelcontextprotocol.server.McpServerFeatures.SyncResourceSpecification;
import io.modelcontextprotocol.spec.McpSchema;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** Registers MCP resources for bankroll data: status, pending bets, today, and history summary. */
@Configuration
@RequiredArgsConstructor
@Slf4j
public class BankrollResource {

    private final BankrollService bankrollService;
    private final HistoryService historyService;
    private final ObjectMapper objectMapper;

    /** Registers the four bankroll:// resources. */
    @Bean
    public List<SyncResourceSpecification> bankrollResources() {
        return List.of(statusResource(), pendingBetsResource(), todayResource(), historySummaryResource());
    }

    private SyncResourceSpecification statusResource() {
        var resource = new McpSchema.Resource(
                "bankroll://status",
                "Bankroll Status",
                "Current bankroll balance, P/L, ROI, streak, and bet counts",
                "application/json",
                null);
        return new SyncResourceSpecification(resource, (exchange, request) -> {
            var status = bankrollService.getBankrollStatus();
            return new McpSchema.ReadResourceResult(
                    List.of(new McpSchema.TextResourceContents(request.uri(), "application/json", toJson(status))));
        });
    }

    private SyncResourceSpecification pendingBetsResource() {
        var resource = new McpSchema.Resource(
                "bankroll://pending_bets",
                "Pending Bets",
                "All currently pending/unsettled bets with details",
                "application/json",
                null);
        return new SyncResourceSpecification(resource, (exchange, request) -> {
            var pending = historyService.getBetHistory(null, null, "PENDING", null, null, null, null, null, null);
            return new McpSchema.ReadResourceResult(
                    List.of(new McpSchema.TextResourceContents(request.uri(), "application/json", toJson(pending))));
        });
    }

    private SyncResourceSpecification todayResource() {
        var resource = new McpSchema.Resource(
                "bankroll://today",
                "Today's Performance",
                "Today's betting card with placed, pending, settled counts and daily P/L",
                "application/json",
                null);
        return new SyncResourceSpecification(resource, (exchange, request) -> {
            var today = historyService.getDailyPerformance();
            return new McpSchema.ReadResourceResult(
                    List.of(new McpSchema.TextResourceContents(request.uri(), "application/json", toJson(today))));
        });
    }

    private SyncResourceSpecification historySummaryResource() {
        var resource = new McpSchema.Resource(
                "bankroll://history/summary",
                "Betting History Summary",
                "Analytics summary: profit by sport/type/month, win rate, avg odds, max drawdown",
                "application/json",
                null);
        return new SyncResourceSpecification(resource, (exchange, request) -> {
            var analytics = historyService.getBankrollAnalytics();
            return new McpSchema.ReadResourceResult(
                    List.of(new McpSchema.TextResourceContents(request.uri(), "application/json", toJson(analytics))));
        });
    }

    private String toJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException e) {
            log.error("Failed to serialize resource to JSON: {}", e.getMessage());
            throw new IllegalStateException("Resource serialization failed", e);
        }
    }
}
