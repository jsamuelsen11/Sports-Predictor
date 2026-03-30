package com.sportspredictor.tool;

import com.sportspredictor.service.OddsMonitorService;
import com.sportspredictor.service.OddsMonitorService.MonitorResult;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Service;

/** MCP tool for monitoring odds changes over time. */
@Service
@RequiredArgsConstructor
public class OddsMonitorTool {

    private final OddsMonitorService oddsMonitorService;

    /** Response for monitor_odds_for_event. */
    public record MonitorOddsResponse(
            String monitorId,
            String eventId,
            int intervalMinutes,
            int durationHours,
            String expiresAt,
            int snapshotsCollected,
            boolean active,
            String summary) {}

    /** Starts or resumes odds monitoring for an event. */
    @Tool(
            name = "monitor_odds_for_event",
            description = "Start monitoring odds for an event at regular intervals."
                    + " Captures snapshots over time for line movement tracking."
                    + " Call again to get updated status and trigger a new snapshot")
    public MonitorOddsResponse monitorOddsForEvent(
            @ToolParam(description = "Event ID to monitor") String eventId,
            @ToolParam(description = "Polling interval in minutes (default 15)", required = false)
                    Integer intervalMinutes,
            @ToolParam(description = "Duration to monitor in hours (default 24, max 168)", required = false)
                    Integer durationHours) {

        MonitorResult r = oddsMonitorService.startMonitoring(eventId, intervalMinutes, durationHours);
        return new MonitorOddsResponse(
                r.monitorId(),
                r.eventId(),
                r.intervalMinutes(),
                r.durationHours(),
                r.expiresAt(),
                r.snapshotsCollected(),
                r.active(),
                r.summary());
    }
}
