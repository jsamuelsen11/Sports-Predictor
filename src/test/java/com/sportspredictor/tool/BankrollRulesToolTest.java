package com.sportspredictor.tool;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.sportspredictor.service.BankrollRulesService;
import com.sportspredictor.service.BankrollRulesService.DailyLimitsResult;
import com.sportspredictor.service.BankrollRulesService.SetRulesResult;
import com.sportspredictor.tool.BankrollRulesTool.DailyLimitsResponse;
import com.sportspredictor.tool.BankrollRulesTool.SetRulesResponse;
import java.math.BigDecimal;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/** Tests for {@link BankrollRulesTool}. */
@ExtendWith(MockitoExtension.class)
class BankrollRulesToolTest {

    @Mock
    private BankrollRulesService bankrollRulesService;

    @InjectMocks
    private BankrollRulesTool bankrollRulesTool;

    /** Tests for {@link BankrollRulesTool#setBankrollRules}. */
    @Nested
    class SetBankrollRules {

        @Test
        void delegatesToService() {
            when(bankrollRulesService.setRules(
                            any(BigDecimal.class),
                            any(BigDecimal.class),
                            any(BigDecimal.class),
                            eq(4),
                            any(BigDecimal.class),
                            any(BigDecimal.class)))
                    .thenReturn(new SetRulesResult(
                            "rules-1",
                            new BigDecimal("3.0"),
                            new BigDecimal("500"),
                            new BigDecimal("200"),
                            4,
                            new BigDecimal("0.60"),
                            new BigDecimal("25.00"),
                            "Bankroll rules updated successfully"));

            SetRulesResponse response = bankrollRulesTool.setBankrollRules(3.0, 500.0, 200.0, 4, 0.60, 25.0);

            assertThat(response.rulesId()).isEqualTo("rules-1");
            assertThat(response.maxBetUnits()).isEqualTo(3.0);
            assertThat(response.dailyExposureLimit()).isEqualTo(500.0);
            assertThat(response.stopLossThreshold()).isEqualTo(200.0);
            assertThat(response.maxParlayLegs()).isEqualTo(4);
            assertThat(response.minConfidence()).isEqualTo(0.60);
            assertThat(response.unitSize()).isEqualTo(25.0);
            assertThat(response.summary()).isEqualTo("Bankroll rules updated successfully");
            verify(bankrollRulesService)
                    .setRules(
                            any(BigDecimal.class),
                            any(BigDecimal.class),
                            any(BigDecimal.class),
                            eq(4),
                            any(BigDecimal.class),
                            any(BigDecimal.class));
        }
    }

    /** Tests for {@link BankrollRulesTool#getDailyLimitsStatus()}. */
    @Nested
    class GetDailyLimitsStatus {

        @Test
        void delegatesToService() {
            when(bankrollRulesService.getDailyLimitsStatus())
                    .thenReturn(new DailyLimitsResult(
                            new BigDecimal("500"),
                            new BigDecimal("175"),
                            new BigDecimal("325"),
                            new BigDecimal("200"),
                            new BigDecimal("-50"),
                            false,
                            new BigDecimal("3.0"),
                            new BigDecimal("25.00"),
                            new BigDecimal("75.00"),
                            4,
                            "Wagered today: $175.00. Remaining: $325.00. P/L: $-50.00. Stop-loss: OK"));

            DailyLimitsResponse response = bankrollRulesTool.getDailyLimitsStatus();

            assertThat(response.dailyExposureLimit()).isEqualTo(500.0);
            assertThat(response.wageredToday()).isEqualTo(175.0);
            assertThat(response.remainingCapacity()).isEqualTo(325.0);
            assertThat(response.stopLossThreshold()).isEqualTo(200.0);
            assertThat(response.dailyPnl()).isEqualTo(-50.0);
            assertThat(response.stopLossTriggered()).isFalse();
            assertThat(response.maxBetUnits()).isEqualTo(3.0);
            assertThat(response.unitSize()).isEqualTo(25.0);
            assertThat(response.maxBetAmount()).isEqualTo(75.0);
            assertThat(response.maxParlayLegs()).isEqualTo(4);
            verify(bankrollRulesService).getDailyLimitsStatus();
        }
    }
}
