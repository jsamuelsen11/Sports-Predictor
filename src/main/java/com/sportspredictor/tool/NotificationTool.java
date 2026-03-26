package com.sportspredictor.tool;

import com.sportspredictor.service.NotificationService;
import com.sportspredictor.service.NotificationService.AlertsResult;
import com.sportspredictor.service.NotificationService.BriefingResult;
import com.sportspredictor.service.NotificationService.SubscribeResult;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Service;

/** MCP tools for notifications: daily briefing, subscriptions, alerts. */
@Service
@RequiredArgsConstructor
public class NotificationTool {

    private final NotificationService notificationService;

    /** Response records. */
    public record BriefingResponse(int pendingBetCount, int alertCount, String summary) {}

    /** Response for subscription actions. */
    public record SubscribeResponse(String subscriptionId, String type, String sport, String summary) {}

    /** Response for alerts retrieval. */
    public record AlertsResponse(int alertCount, String summary) {}

    /** Generates a daily briefing. */
    @Tool(
            name = "get_daily_briefing",
            description = "Day's summary: pending bets, unread alerts, and game slate overview")
    public BriefingResponse getDailyBriefing() {
        BriefingResult r = notificationService.getDailyBriefing();
        return new BriefingResponse(r.pendingBetCount(), r.alertCount(), r.summary());
    }

    /** Subscribes to line movement alerts. */
    @Tool(name = "subscribe_line_movement", description = "Subscribe to alerts when a line moves past a threshold")
    public SubscribeResponse subscribeLineMovement(
            @ToolParam(description = "Sport key") String sport,
            @ToolParam(description = "Movement threshold in points (e.g., 1.0)") double threshold) {
        SubscribeResult r = notificationService.subscribeLineMovement(sport, threshold);
        return new SubscribeResponse(r.subscriptionId(), r.type(), r.sport(), r.summary());
    }

    /** Subscribes to injury update alerts. */
    @Tool(name = "subscribe_injury_updates", description = "Subscribe to injury status changes for a sport")
    public SubscribeResponse subscribeInjuryUpdates(@ToolParam(description = "Sport key") String sport) {
        SubscribeResult r = notificationService.subscribeInjuryUpdates(sport);
        return new SubscribeResponse(r.subscriptionId(), r.type(), r.sport(), r.summary());
    }

    /** Retrieves all pending unread alerts. */
    @Tool(name = "get_alerts", description = "Retrieve all pending unread alerts")
    public AlertsResponse getAlerts() {
        AlertsResult r = notificationService.getAlerts();
        return new AlertsResponse(r.count(), r.summary());
    }
}
