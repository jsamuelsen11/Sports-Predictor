package com.sportspredictor.mcpserver.tool;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.sportspredictor.mcpserver.service.BankrollService;
import com.sportspredictor.mcpserver.service.BankrollService.BankrollStatusResult;
import com.sportspredictor.mcpserver.service.BankrollService.DepositResult;
import com.sportspredictor.mcpserver.service.BankrollService.ResetBankrollResult;
import com.sportspredictor.mcpserver.service.BankrollService.WithdrawResult;
import com.sportspredictor.mcpserver.tool.BankrollTool.BankrollStatusResponse;
import com.sportspredictor.mcpserver.tool.BankrollTool.DepositResponse;
import com.sportspredictor.mcpserver.tool.BankrollTool.ResetBankrollResponse;
import com.sportspredictor.mcpserver.tool.BankrollTool.WithdrawResponse;
import java.math.BigDecimal;
import java.time.Instant;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/** Tests for {@link BankrollTool}. */
@ExtendWith(MockitoExtension.class)
class BankrollToolTest {

    @Mock
    private BankrollService bankrollService;

    @InjectMocks
    private BankrollTool bankrollTool;

    /** Tests for {@link BankrollTool#getBankrollStatus()}. */
    @Nested
    class GetBankrollStatus {

        @Test
        void delegatesToService() {
            when(bankrollService.getBankrollStatus())
                    .thenReturn(new BankrollStatusResult(
                            "br-1",
                            "Test",
                            new BigDecimal("1000"),
                            new BigDecimal("1100"),
                            new BigDecimal("500"),
                            new BigDecimal("300"),
                            new BigDecimal("200"),
                            new BigDecimal("100"),
                            new BigDecimal("20.00"),
                            10,
                            6,
                            4,
                            0,
                            0,
                            3,
                            "WIN",
                            Instant.parse("2026-01-01T00:00:00Z"),
                            "Summary"));

            BankrollStatusResponse response = bankrollTool.getBankrollStatus();

            assertThat(response.bankrollId()).isEqualTo("br-1");
            assertThat(response.currentBalance()).isEqualTo(1100.0);
            assertThat(response.roiPercent()).isEqualTo(20.0);
            assertThat(response.wins()).isEqualTo(6);
            verify(bankrollService).getBankrollStatus();
        }
    }

    /** Tests for {@link BankrollTool#resetBankroll(double)}. */
    @Nested
    class ResetBankroll {

        @Test
        void delegatesToService() {
            when(bankrollService.resetBankroll(any(BigDecimal.class)))
                    .thenReturn(new ResetBankrollResult("br-old", "br-new", new BigDecimal("2000"), "Reset done"));

            ResetBankrollResponse response = bankrollTool.resetBankroll(2000.0);

            assertThat(response.newBankrollId()).isEqualTo("br-new");
            assertThat(response.startingBalance()).isEqualTo(2000.0);
        }
    }

    /** Tests for {@link BankrollTool#depositFunds(double)}. */
    @Nested
    class DepositFunds {

        @Test
        void delegatesToService() {
            when(bankrollService.depositFunds(any(BigDecimal.class)))
                    .thenReturn(new DepositResult("txn-1", new BigDecimal("200"), new BigDecimal("1200"), "Deposited"));

            DepositResponse response = bankrollTool.depositFunds(200.0);

            assertThat(response.balanceAfter()).isEqualTo(1200.0);
        }
    }

    /** Tests for {@link BankrollTool#withdrawFunds(double)}. */
    @Nested
    class WithdrawFunds {

        @Test
        void delegatesToService() {
            when(bankrollService.withdrawFunds(any(BigDecimal.class)))
                    .thenReturn(new WithdrawResult("txn-2", new BigDecimal("100"), new BigDecimal("900"), "Withdrew"));

            WithdrawResponse response = bankrollTool.withdrawFunds(100.0);

            assertThat(response.balanceAfter()).isEqualTo(900.0);
        }
    }
}
