package com.sportspredictor.repository;

import com.sportspredictor.entity.Bet;
import com.sportspredictor.entity.enums.BetStatus;
import java.time.Instant;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

/** Spring Data repository for {@link Bet} entities. */
public interface BetRepository extends JpaRepository<Bet, String> {

    /** Returns all bets belonging to the given bankroll. */
    List<Bet> findByBankrollId(String bankrollId);

    /** Returns all bets with the given status. */
    List<Bet> findByStatus(BetStatus status);

    /** Returns bets for a bankroll filtered by status. */
    List<Bet> findByBankrollIdAndStatus(String bankrollId, BetStatus status);

    /** Returns all bets for the given sport. */
    List<Bet> findBySport(String sport);

    /** Returns all bets for the given event. */
    List<Bet> findByEventId(String eventId);

    /** Returns bets placed within the given time range (inclusive). */
    List<Bet> findByPlacedAtBetween(Instant start, Instant end);
}
