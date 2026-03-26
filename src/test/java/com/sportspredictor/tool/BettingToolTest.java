package com.sportspredictor.tool;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sportspredictor.service.BettingService;
import com.sportspredictor.service.BettingService.CancelBetResult;
import com.sportspredictor.service.BettingService.LegSummary;
import com.sportspredictor.service.BettingService.PlaceBetResult;
import com.sportspredictor.service.BettingService.PlaceParlayResult;
import com.sportspredictor.tool.BettingTool.CancelBetResponse;
import com.sportspredictor.tool.BettingTool.PlaceBetResponse;
import com.sportspredictor.tool.BettingTool.PlaceParlayResponse;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

/** Tests for {@link BettingTool}. */
@ExtendWith(MockitoExtension.class)
class BettingToolTest {

    @Mock
    private BettingService bettingService;

    @Spy
    private ObjectMapper objectMapper = new ObjectMapper();

    @InjectMocks
    private BettingTool bettingTool;

    /** Tests for {@link BettingTool#placeBet}. */
    @Nested
    class PlaceBet {

        @Test
        void delegatesToService() {
            when(bettingService.placeBet(
                            eq("nba"),
                            eq("evt-1"),
                            eq("MONEYLINE"),
                            eq("Lakers ML"),
                            eq(-150),
                            any(),
                            eq("Test"),
                            eq(null)))
                    .thenReturn(new PlaceBetResult(
                            "bet-1",
                            "nba",
                            "evt-1",
                            "MONEYLINE",
                            "Lakers ML",
                            "Test",
                            new BigDecimal("100"),
                            -150,
                            new BigDecimal("1.667"),
                            new BigDecimal("166.70"),
                            new BigDecimal("900"),
                            "Summary"));

            PlaceBetResponse response =
                    bettingTool.placeBet("nba", "evt-1", "MONEYLINE", "Lakers ML", -150, 100.0, "Test", null);

            assertThat(response.betId()).isEqualTo("bet-1");
            assertThat(response.balanceAfter()).isEqualTo(900.0);
        }
    }

    /** Tests for {@link BettingTool#placeParlay}. */
    @Nested
    class PlaceParlay {

        @Test
        void delegatesToService() {
            when(bettingService.placeParlayBet(any(), any(), any(), any()))
                    .thenReturn(new PlaceParlayResult(
                            "bet-2",
                            new BigDecimal("50"),
                            new BigDecimal("3.50"),
                            +250,
                            new BigDecimal("175"),
                            List.of(
                                    new LegSummary(1, "nba", "evt-1", "Lakers ML", -150),
                                    new LegSummary(2, "nfl", "evt-2", "Chiefs ML", +110)),
                            new BigDecimal("950"),
                            "Summary"));

            String legsJson = "[{\"sport\":\"nba\",\"eventId\":\"evt-1\","
                    + "\"selection\":\"Lakers ML\",\"americanOdds\":-150},"
                    + "{\"sport\":\"nfl\",\"eventId\":\"evt-2\","
                    + "\"selection\":\"Chiefs ML\",\"americanOdds\":110}]";

            PlaceParlayResponse response = bettingTool.placeParlay(legsJson, 50.0, "Test parlay", null);

            assertThat(response.betId()).isEqualTo("bet-2");
            assertThat(response.legs()).hasSize(2);
        }
    }

    /** Tests for {@link BettingTool#cancelBet(String)}. */
    @Nested
    class CancelBet {

        @Test
        void delegatesToService() {
            when(bettingService.cancelBet("bet-1"))
                    .thenReturn(
                            new CancelBetResult("bet-1", new BigDecimal("100"), new BigDecimal("1000"), "Cancelled"));

            CancelBetResponse response = bettingTool.cancelBet("bet-1");

            assertThat(response.refundedStake()).isEqualTo(100.0);
            verify(bettingService).cancelBet("bet-1");
        }
    }
}
