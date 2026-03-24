package com.sportspredictor.repository;

import static org.assertj.core.api.Assertions.assertThat;

import com.sportspredictor.entity.Bankroll;
import com.sportspredictor.entity.Bet;
import com.sportspredictor.entity.enums.BetStatus;
import com.sportspredictor.entity.enums.BetType;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

/** Tests for {@link BetRepository}. */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class BetRepositoryTest {

    @Autowired
    private BetRepository betRepository;

    @Autowired
    private BankrollRepository bankrollRepository;

    private Bankroll bankroll;

    @BeforeEach
    void setUp() {
        bankroll = bankrollRepository.saveAndFlush(Bankroll.builder()
                .name("Test Bankroll")
                .startingBalance(new BigDecimal("1000.00"))
                .currentBalance(new BigDecimal("1000.00"))
                .createdAt(Instant.parse("2026-01-01T00:00:00Z"))
                .build());
    }

    private Bet saveBet(BetStatus status, String sport, String eventId, Instant placedAt) {
        Bet bet = Bet.builder()
                .bankroll(bankroll)
                .betType(BetType.MONEYLINE)
                .status(status)
                .stake(new BigDecimal("50.00"))
                .odds(new BigDecimal("-110"))
                .potentialPayout(new BigDecimal("95.45"))
                .sport(sport)
                .eventId(eventId)
                .description("Test bet")
                .placedAt(placedAt)
                .build();
        return betRepository.saveAndFlush(bet);
    }

    @Nested
    class FindByBankrollId {

        @Test
        void returnsBetsForBankroll() {
            saveBet(BetStatus.PENDING, "NFL", "evt-1", Instant.parse("2026-01-10T00:00:00Z"));

            List<Bet> result = betRepository.findByBankrollId(bankroll.getId());

            assertThat(result).hasSize(1);
        }

        @Test
        void returnsEmptyForUnknownBankroll() {
            List<Bet> result = betRepository.findByBankrollId("unknown");

            assertThat(result).isEmpty();
        }
    }

    @Nested
    class FindByStatus {

        @Test
        void returnsBetsWithMatchingStatus() {
            saveBet(BetStatus.PENDING, "NFL", "evt-1", Instant.parse("2026-01-10T00:00:00Z"));
            saveBet(BetStatus.WON, "NFL", "evt-2", Instant.parse("2026-01-11T00:00:00Z"));

            List<Bet> result = betRepository.findByStatus(BetStatus.PENDING);

            assertThat(result).hasSize(1);
            assertThat(result.get(0).getStatus()).isEqualTo(BetStatus.PENDING);
        }
    }

    @Nested
    class FindByBankrollIdAndStatus {

        @Test
        void filtersByBothBankrollAndStatus() {
            saveBet(BetStatus.PENDING, "NFL", "evt-1", Instant.parse("2026-01-10T00:00:00Z"));
            saveBet(BetStatus.WON, "NFL", "evt-2", Instant.parse("2026-01-11T00:00:00Z"));

            List<Bet> result = betRepository.findByBankrollIdAndStatus(bankroll.getId(), BetStatus.WON);

            assertThat(result).hasSize(1);
            assertThat(result.get(0).getStatus()).isEqualTo(BetStatus.WON);
        }
    }

    @Nested
    class FindBySport {

        @Test
        void returnsBetsForSport() {
            saveBet(BetStatus.PENDING, "NFL", "evt-1", Instant.parse("2026-01-10T00:00:00Z"));
            saveBet(BetStatus.PENDING, "NBA", "evt-2", Instant.parse("2026-01-11T00:00:00Z"));

            List<Bet> result = betRepository.findBySport("NBA");

            assertThat(result).hasSize(1);
            assertThat(result.get(0).getSport()).isEqualTo("NBA");
        }
    }

    @Nested
    class FindByEventId {

        @Test
        void returnsBetsForEvent() {
            saveBet(BetStatus.PENDING, "NFL", "evt-123", Instant.parse("2026-01-10T00:00:00Z"));

            List<Bet> result = betRepository.findByEventId("evt-123");

            assertThat(result).hasSize(1);
            assertThat(result.get(0).getEventId()).isEqualTo("evt-123");
        }
    }

    @Nested
    class FindByPlacedAtBetween {

        @Test
        void returnsBetsInDateRange() {
            saveBet(BetStatus.PENDING, "NFL", "evt-1", Instant.parse("2026-01-10T00:00:00Z"));
            saveBet(BetStatus.PENDING, "NFL", "evt-2", Instant.parse("2026-02-10T00:00:00Z"));
            saveBet(BetStatus.PENDING, "NFL", "evt-3", Instant.parse("2026-03-10T00:00:00Z"));

            List<Bet> result = betRepository.findByPlacedAtBetween(
                    Instant.parse("2026-01-01T00:00:00Z"), Instant.parse("2026-01-31T00:00:00Z"));

            assertThat(result).hasSize(1);
        }
    }
}
