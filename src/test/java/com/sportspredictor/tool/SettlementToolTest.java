package com.sportspredictor.tool;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sportspredictor.service.SettlementService;
import com.sportspredictor.service.SettlementService.AutoSettleResult;
import com.sportspredictor.service.SettlementService.SettleBetResult;
import com.sportspredictor.service.SettlementService.SettleParlayResult;
import com.sportspredictor.tool.SettlementTool.AutoSettleResponse;
import com.sportspredictor.tool.SettlementTool.SettleBetResponse;
import com.sportspredictor.tool.SettlementTool.SettleParlayResponse;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

/** Tests for {@link SettlementTool}. */
@ExtendWith(MockitoExtension.class)
class SettlementToolTest {

    @Mock
    private SettlementService settlementService;

    @Spy
    private ObjectMapper objectMapper = new ObjectMapper();

    @InjectMocks
    private SettlementTool settlementTool;

    /** Tests for {@link SettlementTool#settleBet}. */
    @Nested
    class SettleBet {

        @Test
        void delegatesToService() {
            when(settlementService.settleBet("bet-1", "WON"))
                    .thenReturn(new SettleBetResult(
                            "bet-1",
                            "PENDING",
                            "WON",
                            new BigDecimal("100"),
                            new BigDecimal("250"),
                            new BigDecimal("1150"),
                            "Summary"));

            SettleBetResponse response = settlementTool.settleBet("bet-1", "WON");

            assertThat(response.newStatus()).isEqualTo("WON");
            assertThat(response.payout()).isEqualTo(250.0);
            verify(settlementService).settleBet("bet-1", "WON");
        }
    }

    /** Tests for {@link SettlementTool#settleParlay}. */
    @Nested
    class SettleParlay {

        @Test
        void delegatesToService() {
            when(settlementService.settleParlay(eq("bet-1"), any()))
                    .thenReturn(new SettleParlayResult(
                            "bet-1",
                            "WON",
                            new BigDecimal("50"),
                            new BigDecimal("175"),
                            List.of(),
                            new BigDecimal("1125"),
                            "Summary"));

            String json = "[{\"legNumber\":1,\"outcome\":\"WON\",\"resultDetail\":\"110-105\"},"
                    + "{\"legNumber\":2,\"outcome\":\"WON\",\"resultDetail\":\"24-17\"}]";

            SettleParlayResponse response = settlementTool.settleParlay("bet-1", json);

            assertThat(response.newStatus()).isEqualTo("WON");
            assertThat(response.payout()).isEqualTo(175.0);
        }
    }

    /** Tests for {@link SettlementTool#autoSettleBets}. */
    @Nested
    class AutoSettleBets {

        @Test
        void delegatesToService() {
            when(settlementService.autoSettleBets("nba"))
                    .thenReturn(new AutoSettleResult(5, 3, 2, 1, 1, 0, 0, List.of(), "Summary"));

            AutoSettleResponse response = settlementTool.autoSettleBets("nba");

            assertThat(response.settled()).isEqualTo(2);
            assertThat(response.won()).isEqualTo(1);
            verify(settlementService).autoSettleBets("nba");
        }
    }
}
