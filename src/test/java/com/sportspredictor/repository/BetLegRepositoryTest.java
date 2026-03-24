package com.sportspredictor.repository;

import static org.assertj.core.api.Assertions.assertThat;

import com.sportspredictor.entity.Bankroll;
import com.sportspredictor.entity.Bet;
import com.sportspredictor.entity.BetLeg;
import com.sportspredictor.entity.enums.BetLegStatus;
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

/** Tests for {@link BetLegRepository}. */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class BetLegRepositoryTest {

    @Autowired
    private BetLegRepository betLegRepository;

    @Autowired
    private BetRepository betRepository;

    @Autowired
    private BankrollRepository bankrollRepository;

    private Bet bet;

    @BeforeEach
    void setUp() {
        Bankroll bankroll = bankrollRepository.saveAndFlush(Bankroll.builder()
                .name("Test Bankroll")
                .startingBalance(new BigDecimal("1000.00"))
                .currentBalance(new BigDecimal("1000.00"))
                .createdAt(Instant.parse("2026-01-01T00:00:00Z"))
                .build());

        bet = betRepository.saveAndFlush(Bet.builder()
                .bankroll(bankroll)
                .betType(BetType.PARLAY)
                .status(BetStatus.PENDING)
                .stake(new BigDecimal("50.00"))
                .odds(new BigDecimal("+250"))
                .potentialPayout(new BigDecimal("175.00"))
                .sport("NFL")
                .eventId("evt-1")
                .description("Parlay bet")
                .placedAt(Instant.parse("2026-01-10T00:00:00Z"))
                .build());
    }

    private BetLeg saveLeg(int legNumber, BetLegStatus status, String eventId, String sport) {
        BetLeg leg = BetLeg.builder()
                .bet(bet)
                .legNumber(legNumber)
                .selection("Team A ML")
                .odds(new BigDecimal("-110"))
                .status(status)
                .eventId(eventId)
                .sport(sport)
                .build();
        return betLegRepository.saveAndFlush(leg);
    }

    @Nested
    class FindByBetId {

        @Test
        void returnsLegsForBet() {
            saveLeg(1, BetLegStatus.PENDING, "evt-1", "NFL");
            saveLeg(2, BetLegStatus.PENDING, "evt-2", "NBA");

            List<BetLeg> result = betLegRepository.findByBetId(bet.getId());

            assertThat(result).hasSize(2);
        }

        @Test
        void returnsEmptyForUnknownBet() {
            List<BetLeg> result = betLegRepository.findByBetId("unknown");

            assertThat(result).isEmpty();
        }
    }

    @Nested
    class FindByEventId {

        @Test
        void returnsLegsForEvent() {
            saveLeg(1, BetLegStatus.PENDING, "evt-123", "NFL");
            saveLeg(2, BetLegStatus.PENDING, "evt-456", "NBA");

            List<BetLeg> result = betLegRepository.findByEventId("evt-123");

            assertThat(result).hasSize(1);
            assertThat(result.get(0).getEventId()).isEqualTo("evt-123");
        }
    }

    @Nested
    class FindByStatus {

        @Test
        void returnsLegsWithMatchingStatus() {
            saveLeg(1, BetLegStatus.WON, "evt-1", "NFL");
            saveLeg(2, BetLegStatus.PENDING, "evt-2", "NBA");

            List<BetLeg> result = betLegRepository.findByStatus(BetLegStatus.WON);

            assertThat(result).hasSize(1);
            assertThat(result.get(0).getStatus()).isEqualTo(BetLegStatus.WON);
        }
    }
}
