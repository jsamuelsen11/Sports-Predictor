package com.sportspredictor.mcpserver.service;

import com.sportspredictor.mcpserver.entity.OddsMonitor;
import com.sportspredictor.mcpserver.entity.OddsSnapshot;
import com.sportspredictor.mcpserver.repository.OddsMonitorRepository;
import com.sportspredictor.mcpserver.repository.OddsSnapshotRepository;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Locale;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Manages odds monitoring subscriptions and captures odds snapshots for events. */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class OddsMonitorService {

    private static final int DEFAULT_INTERVAL_MINUTES = 15;
    private static final int DEFAULT_DURATION_HOURS = 24;
    private static final int MAX_DURATION_HOURS = 168;

    private final OddsMonitorRepository oddsMonitorRepository;
    private final OddsSnapshotRepository oddsSnapshotRepository;
    private final OddsService oddsService;

    /** Result of starting or querying a monitor. */
    public record MonitorResult(
            String monitorId,
            String eventId,
            int intervalMinutes,
            int durationHours,
            String expiresAt,
            int snapshotsCollected,
            boolean active,
            String summary) {}

    /** Starts monitoring odds for an event at the given interval. */
    public MonitorResult startMonitoring(String eventId, Integer intervalMinutes, Integer durationHours) {
        int interval = intervalMinutes != null && intervalMinutes > 0 ? intervalMinutes : DEFAULT_INTERVAL_MINUTES;
        int duration = durationHours != null && durationHours > 0
                ? Math.min(durationHours, MAX_DURATION_HOURS)
                : DEFAULT_DURATION_HOURS;

        List<OddsMonitor> existing = oddsMonitorRepository.findByEventIdAndActiveTrue(eventId);
        if (!existing.isEmpty()) {
            OddsMonitor active = existing.getFirst();
            return getMonitorStatus(active.getId());
        }

        Instant now = Instant.now();
        Instant expires = now.plus(duration, ChronoUnit.HOURS);

        OddsMonitor monitor = OddsMonitor.builder()
                .eventId(eventId)
                .intervalMinutes(interval)
                .durationHours(duration)
                .active(true)
                .startedAt(now)
                .expiresAt(expires)
                .build();
        oddsMonitorRepository.save(monitor);

        captureSnapshot(eventId);

        int snapshots = oddsSnapshotRepository
                .findByEventIdOrderByCapturedAtAsc(eventId)
                .size();

        String summary = String.format(
                Locale.ROOT,
                "Started monitoring event %s every %d minutes for %d hours (expires %s). %d snapshots collected.",
                eventId,
                interval,
                duration,
                expires,
                snapshots);

        log.info("Odds monitor started: event={}, interval={}m, duration={}h", eventId, interval, duration);

        return new MonitorResult(
                monitor.getId(), eventId, interval, duration, expires.toString(), snapshots, true, summary);
    }

    /** Gets the current status of a monitor, expiring it if past duration. */
    public MonitorResult getMonitorStatus(String monitorId) {
        OddsMonitor monitor = oddsMonitorRepository
                .findById(monitorId)
                .orElseThrow(() -> new IllegalArgumentException("Monitor not found: " + monitorId));

        if (monitor.isActive() && Instant.now().isAfter(monitor.getExpiresAt())) {
            monitor.setActive(false);
            oddsMonitorRepository.save(monitor);
        }

        if (monitor.isActive()) {
            captureSnapshot(monitor.getEventId());
        }

        int snapshots = oddsSnapshotRepository
                .findByEventIdOrderByCapturedAtAsc(monitor.getEventId())
                .size();

        String status = monitor.isActive() ? "active" : "expired";
        String summary = String.format(
                Locale.ROOT,
                "Monitor %s for event %s: %s. %d snapshots collected.",
                monitorId,
                monitor.getEventId(),
                status,
                snapshots);

        return new MonitorResult(
                monitorId,
                monitor.getEventId(),
                monitor.getIntervalMinutes(),
                monitor.getDurationHours(),
                monitor.getExpiresAt().toString(),
                snapshots,
                monitor.isActive(),
                summary);
    }

    private void captureSnapshot(String eventId) {
        try {
            var oddsResult = oddsService.getLiveOdds("", eventId, "");
            for (var event : oddsResult.events()) {
                for (var bookmaker : event.bookmakers()) {
                    for (var market : bookmaker.markets()) {
                        String oddsData = market.outcomes().stream()
                                .map(o -> o.name() + ":" + (int) o.price())
                                .reduce((a, b) -> a + "," + b)
                                .orElse("");

                        OddsSnapshot snapshot = OddsSnapshot.builder()
                                .eventId(eventId)
                                .sport(oddsResult.sport() != null ? oddsResult.sport() : "unknown")
                                .bookmaker(bookmaker.key())
                                .market(market.key())
                                .oddsData(oddsData)
                                .capturedAt(Instant.now())
                                .build();
                        oddsSnapshotRepository.save(snapshot);
                    }
                }
            }
        } catch (Exception e) {
            log.warn("Failed to capture odds snapshot for event {}: {}", eventId, e.getMessage());
        }
    }
}
