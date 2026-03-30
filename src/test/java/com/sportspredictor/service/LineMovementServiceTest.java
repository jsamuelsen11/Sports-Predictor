package com.sportspredictor.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.sportspredictor.entity.OddsSnapshot;
import com.sportspredictor.repository.OddsSnapshotRepository;
import com.sportspredictor.service.LineMovementService.ArbitrageResult;
import com.sportspredictor.service.LineMovementService.LineMovementResult;
import com.sportspredictor.service.LineMovementService.MarketConsensusResult;
import java.time.Instant;
import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/** Tests for {@link LineMovementService}. */
@ExtendWith(MockitoExtension.class)
class LineMovementServiceTest {

    @Mock
    private OddsSnapshotRepository oddsSnapshotRepository;

    @InjectMocks
    private LineMovementService lineMovementService;

    private static OddsSnapshot snap(String eventId, String bookmaker, String odds, Instant captured) {
        return OddsSnapshot.builder()
                .id("snap-" + captured.toString())
                .eventId(eventId)
                .sport("nba")
                .bookmaker(bookmaker)
                .market("h2h")
                .oddsData(odds)
                .capturedAt(captured)
                .build();
    }

    @Nested
    class DetectLineMovement {

        @Test
        void detectsSharpMove() {
            Instant t1 = Instant.parse("2026-01-01T10:00:00Z");
            Instant t2 = Instant.parse("2026-01-01T12:00:00Z");
            when(oddsSnapshotRepository.findByEventIdOrderByCapturedAtAsc("evt-1"))
                    .thenReturn(List.of(snap("evt-1", "dk", "-110", t1), snap("evt-1", "dk", "-112", t2)));

            LineMovementResult result = lineMovementService.detectLineMovement("evt-1");

            assertThat(result.delta()).isEqualTo(-2.0);
            assertThat(result.isSharpMove()).isTrue();
            assertThat(result.snapshotCount()).isEqualTo(2);
        }

        @Test
        void returnsEmptyForNoData() {
            when(oddsSnapshotRepository.findByEventIdOrderByCapturedAtAsc("evt-x"))
                    .thenReturn(Collections.emptyList());

            LineMovementResult result = lineMovementService.detectLineMovement("evt-x");

            assertThat(result.snapshotCount()).isZero();
        }
    }

    @Nested
    class FindArbitrageOpportunities {

        @Test
        void returnsEmptyWhenNoArbs() {
            when(oddsSnapshotRepository.findBySportAndCapturedAtAfter(
                            org.mockito.ArgumentMatchers.eq("nba"), org.mockito.ArgumentMatchers.any()))
                    .thenReturn(Collections.emptyList());

            ArbitrageResult result = lineMovementService.findArbitrageOpportunities("nba");

            assertThat(result.opportunities()).isEmpty();
        }
    }

    @Nested
    class GetMarketConsensus {

        @Test
        void computesAverageAcrossBookmakers() {
            Instant now = Instant.now();
            when(oddsSnapshotRepository.findByEventIdOrderByCapturedAtAsc("evt-1"))
                    .thenReturn(List.of(snap("evt-1", "dk", "-110", now), snap("evt-1", "fd", "-115", now)));

            MarketConsensusResult result = lineMovementService.getMarketConsensus("evt-1");

            assertThat(result.bookmakerCount()).isEqualTo(2);
            assertThat(result.averageImpliedProbability()).isGreaterThan(0.0);
        }
    }
}
