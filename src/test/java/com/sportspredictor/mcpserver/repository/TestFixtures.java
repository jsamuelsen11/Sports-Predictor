package com.sportspredictor.mcpserver.repository;

import com.sportspredictor.mcpserver.entity.Bankroll;
import com.sportspredictor.mcpserver.entity.BankrollTransaction;
import com.sportspredictor.mcpserver.entity.Bet;
import com.sportspredictor.mcpserver.entity.BetLeg;
import com.sportspredictor.mcpserver.entity.OddsSnapshot;
import com.sportspredictor.mcpserver.entity.PredictionLog;
import com.sportspredictor.mcpserver.entity.enums.BetLegStatus;
import com.sportspredictor.mcpserver.entity.enums.BetStatus;
import com.sportspredictor.mcpserver.entity.enums.BetType;
import com.sportspredictor.mcpserver.entity.enums.PredictionType;
import com.sportspredictor.mcpserver.entity.enums.TransactionType;
import java.math.BigDecimal;
import java.time.Instant;

/**
 * Shared builder helpers for repository integration tests. Centralises entity construction so that
 * changes to required fields are fixed in one place.
 */
final class TestFixtures {

    static final Instant DEFAULT_CREATED_AT = Instant.parse("2026-01-01T00:00:00Z");

    private TestFixtures() {}

    /** Creates a {@link Bankroll} builder pre-filled with sensible defaults. */
    static Bankroll.BankrollBuilder<?, ?> bankroll() {
        return Bankroll.builder()
                .name("Test Bankroll")
                .startingBalance(new BigDecimal("1000.00"))
                .currentBalance(new BigDecimal("1000.00"))
                .createdAt(DEFAULT_CREATED_AT);
    }

    /** Creates a {@link Bet} builder pre-filled with sensible defaults. */
    static Bet.BetBuilder<?, ?> bet(Bankroll bankroll) {
        return Bet.builder()
                .bankroll(bankroll)
                .betType(BetType.MONEYLINE)
                .status(BetStatus.PENDING)
                .stake(new BigDecimal("50.00"))
                .odds(new BigDecimal("-110"))
                .potentialPayout(new BigDecimal("95.45"))
                .sport("NFL")
                .eventId("evt-1")
                .description("Test bet")
                .placedAt(DEFAULT_CREATED_AT);
    }

    /** Creates a {@link BetLeg} builder pre-filled with sensible defaults. */
    static BetLeg.BetLegBuilder<?, ?> betLeg(Bet bet) {
        return BetLeg.builder()
                .bet(bet)
                .legNumber(1)
                .selection("Team A ML")
                .odds(new BigDecimal("-110"))
                .status(BetLegStatus.PENDING)
                .eventId("evt-1")
                .sport("NFL");
    }

    /** Creates a {@link BankrollTransaction} builder pre-filled with sensible defaults. */
    static BankrollTransaction.BankrollTransactionBuilder<?, ?> transaction(Bankroll bankroll) {
        return BankrollTransaction.builder()
                .bankroll(bankroll)
                .type(TransactionType.DEPOSIT)
                .amount(new BigDecimal("100.00"))
                .balanceAfter(new BigDecimal("1100.00"))
                .createdAt(DEFAULT_CREATED_AT);
    }

    /** Creates an {@link OddsSnapshot} builder pre-filled with sensible defaults. */
    static OddsSnapshot.OddsSnapshotBuilder<?, ?> oddsSnapshot() {
        return OddsSnapshot.builder()
                .eventId("evt-1")
                .sport("NFL")
                .bookmaker("DraftKings")
                .market("spread")
                .oddsData("{\"home\": -110, \"away\": +100}")
                .capturedAt(DEFAULT_CREATED_AT);
    }

    /** Creates a {@link PredictionLog} builder pre-filled with sensible defaults. */
    static PredictionLog.PredictionLogBuilder<?, ?> predictionLog() {
        return PredictionLog.builder()
                .eventId("evt-1")
                .sport("NFL")
                .predictionType(PredictionType.MONEYLINE)
                .predictedOutcome("Team A wins")
                .confidence(0.85)
                .keyFactors("{\"factor1\": \"value1\"}")
                .createdAt(DEFAULT_CREATED_AT);
    }
}
