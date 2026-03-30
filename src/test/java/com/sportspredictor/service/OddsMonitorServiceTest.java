package com.sportspredictor.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.sportspredictor.entity.OddsMonitor;
import com.sportspredictor.entity.OddsSnapshot;
import com.sportspredictor.repository.OddsMonitorRepository;
import com.sportspredictor.repository.OddsSnapshotRepository;
import com.sportspredictor.service.OddsMonitorService.MonitorResult;
import com.sportspredictor.service.OddsService.LiveOddsResult;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/** Tests for {@link OddsMonitorService}. */
@ExtendWith(MockitoExtension.class)
class OddsMonitorServiceTest {

    @Mock
    private OddsMonitorRepository oddsMonitorRepository;

    @Mock
    private OddsSnapshotRepository oddsSnapshotRepository;

    @Mock
    private OddsService oddsService;

    @InjectMocks
    private OddsMonitorService oddsMonitorService;

    private static OddsMonitor buildMonitor(String id, String eventId, boolean active, Instant expiresAt) {
        return OddsMonitor.builder()
                .id(id)
                .eventId(eventId)
                .intervalMinutes(15)
                .durationHours(24)
                .active(active)
                .startedAt(Instant.now().minusSeconds(3600))
                .expiresAt(expiresAt)
                .build();
    }

    private static OddsSnapshot buildSnapshot(String id, String eventId) {
        return OddsSnapshot.builder()
                .id(id)
                .eventId(eventId)
                .sport("nba")
                .bookmaker("draftkings")
                .market("h2h")
                .oddsData("-110")
                .capturedAt(Instant.now())
                .build();
    }

    private static LiveOddsResult emptyLiveOdds() {
        return new LiveOddsResult("nba", "evt-1", "h2h", List.of(), 0);
    }

    /** Tests for {@link OddsMonitorService#startMonitoring}. */
    @Nested
    class StartMonitoring {

        @Test
        void createsNewMonitorForUnmonitoredEvent() {
            when(oddsMonitorRepository.findByEventIdAndActiveTrue("evt-1")).thenReturn(List.of());
            when(oddsMonitorRepository.save(any())).thenAnswer(inv -> {
                OddsMonitor m = inv.getArgument(0);
                if (m.getId() == null) {
                    m.setId("monitor-1");
                }
                return m;
            });
            when(oddsService.getLiveOdds(any(), any(), any())).thenReturn(emptyLiveOdds());
            when(oddsSnapshotRepository.findByEventIdOrderByCapturedAtAsc("evt-1"))
                    .thenReturn(List.of());

            MonitorResult result = oddsMonitorService.startMonitoring("evt-1", null, null);

            assertThat(result.eventId()).isEqualTo("evt-1");
            assertThat(result.active()).isTrue();
            assertThat(result.intervalMinutes()).isEqualTo(15);
            assertThat(result.durationHours()).isEqualTo(24);
        }

        @Test
        void returnsExistingMonitorWhenAlreadyActive() {
            OddsMonitor existing = buildMonitor(
                    "monitor-existing", "evt-1", true, Instant.now().plusSeconds(3600));
            when(oddsMonitorRepository.findByEventIdAndActiveTrue("evt-1")).thenReturn(List.of(existing));
            when(oddsMonitorRepository.findById("monitor-existing")).thenReturn(Optional.of(existing));
            when(oddsService.getLiveOdds(any(), any(), any())).thenReturn(emptyLiveOdds());
            when(oddsSnapshotRepository.findByEventIdOrderByCapturedAtAsc("evt-1"))
                    .thenReturn(List.of());

            MonitorResult result = oddsMonitorService.startMonitoring("evt-1", null, null);

            assertThat(result.monitorId()).isEqualTo("monitor-existing");
        }

        @Test
        void usesCustomIntervalAndDurationWhenProvided() {
            when(oddsMonitorRepository.findByEventIdAndActiveTrue("evt-2")).thenReturn(List.of());
            when(oddsMonitorRepository.save(any())).thenAnswer(inv -> {
                OddsMonitor m = inv.getArgument(0);
                if (m.getId() == null) {
                    m.setId("monitor-2");
                }
                return m;
            });
            when(oddsService.getLiveOdds(any(), any(), any())).thenReturn(emptyLiveOdds());
            when(oddsSnapshotRepository.findByEventIdOrderByCapturedAtAsc("evt-2"))
                    .thenReturn(List.of());

            MonitorResult result = oddsMonitorService.startMonitoring("evt-2", 30, 48);

            assertThat(result.intervalMinutes()).isEqualTo(30);
            assertThat(result.durationHours()).isEqualTo(48);
        }

        @Test
        void snapshotCountReflectsExistingSnapshots() {
            when(oddsMonitorRepository.findByEventIdAndActiveTrue("evt-3")).thenReturn(List.of());
            when(oddsMonitorRepository.save(any())).thenAnswer(inv -> {
                OddsMonitor m = inv.getArgument(0);
                if (m.getId() == null) {
                    m.setId("monitor-3");
                }
                return m;
            });
            when(oddsService.getLiveOdds(any(), any(), any())).thenReturn(emptyLiveOdds());
            when(oddsSnapshotRepository.findByEventIdOrderByCapturedAtAsc("evt-3"))
                    .thenReturn(List.of(buildSnapshot("snap-1", "evt-3"), buildSnapshot("snap-2", "evt-3")));

            MonitorResult result = oddsMonitorService.startMonitoring("evt-3", null, null);

            assertThat(result.snapshotsCollected()).isEqualTo(2);
        }
    }

    /** Tests for {@link OddsMonitorService#getMonitorStatus}. */
    @Nested
    class GetMonitorStatus {

        @Test
        void returnsActiveStatusForNonExpiredMonitor() {
            OddsMonitor monitor =
                    buildMonitor("monitor-1", "evt-1", true, Instant.now().plusSeconds(3600));
            when(oddsMonitorRepository.findById("monitor-1")).thenReturn(Optional.of(monitor));
            when(oddsService.getLiveOdds(any(), any(), any())).thenReturn(emptyLiveOdds());
            when(oddsSnapshotRepository.findByEventIdOrderByCapturedAtAsc("evt-1"))
                    .thenReturn(List.of());

            MonitorResult result = oddsMonitorService.getMonitorStatus("monitor-1");

            assertThat(result.active()).isTrue();
            assertThat(result.monitorId()).isEqualTo("monitor-1");
            assertThat(result.eventId()).isEqualTo("evt-1");
        }

        @Test
        void expiresMonitorWhenPastExpiry() {
            OddsMonitor monitor =
                    buildMonitor("monitor-1", "evt-1", true, Instant.now().minusSeconds(60));
            when(oddsMonitorRepository.findById("monitor-1")).thenReturn(Optional.of(monitor));
            when(oddsMonitorRepository.save(any())).thenReturn(monitor);
            when(oddsSnapshotRepository.findByEventIdOrderByCapturedAtAsc("evt-1"))
                    .thenReturn(List.of());

            MonitorResult result = oddsMonitorService.getMonitorStatus("monitor-1");

            assertThat(result.active()).isFalse();
        }

        @Test
        void throwsWhenMonitorNotFound() {
            when(oddsMonitorRepository.findById("bad-id")).thenReturn(Optional.empty());

            assertThatThrownBy(() -> oddsMonitorService.getMonitorStatus("bad-id"))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("bad-id");
        }
    }
}
