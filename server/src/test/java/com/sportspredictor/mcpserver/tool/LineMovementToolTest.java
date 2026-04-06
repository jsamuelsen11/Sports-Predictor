package com.sportspredictor.mcpserver.tool;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.sportspredictor.mcpserver.service.LineMovementService;
import com.sportspredictor.mcpserver.service.LineMovementService.ArbitrageResult;
import com.sportspredictor.mcpserver.service.LineMovementService.LineMovementResult;
import com.sportspredictor.mcpserver.service.LineMovementService.MarketConsensusResult;
import java.util.Collections;
import java.util.Map;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/** Tests for {@link LineMovementTool}. */
@ExtendWith(MockitoExtension.class)
class LineMovementToolTest {

    @Mock
    private LineMovementService lineMovementService;

    @InjectMocks
    private LineMovementTool lineMovementTool;

    @Nested
    class DetectLineMovement {
        @Test
        void delegatesToService() {
            when(lineMovementService.detectLineMovement("evt-1"))
                    .thenReturn(
                            new LineMovementResult("evt-1", "-110", "-112", "dk", -2.0, "down", true, 2, "Summary"));
            var response = lineMovementTool.detectLineMovement("evt-1");
            assertThat(response.eventId()).isEqualTo("evt-1");
            assertThat(response.isSharpMove()).isTrue();
        }
    }

    @Nested
    class FindArbitrageOpportunities {
        @Test
        void delegatesToService() {
            when(lineMovementService.findArbitrageOpportunities("nba"))
                    .thenReturn(new ArbitrageResult(Collections.emptyList(), 5, "Summary"));
            var response = lineMovementTool.findArbitrageOpportunities("nba");
            assertThat(response.eventsScanned()).isEqualTo(5);
        }
    }

    @Nested
    class GetMarketConsensus {
        @Test
        void delegatesToService() {
            when(lineMovementService.getMarketConsensus("evt-1"))
                    .thenReturn(new MarketConsensusResult("evt-1", 0.55, 3, Map.of("dk", 0.55), "Summary"));
            var response = lineMovementTool.getMarketConsensus("evt-1");
            assertThat(response.bookmakerCount()).isEqualTo(3);
        }
    }
}
