package com.sportspredictor.repository;

import com.sportspredictor.entity.Bankroll;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

/** Spring Data repository for {@link Bankroll} entities. */
public interface BankrollRepository extends JpaRepository<Bankroll, String> {

    /** Finds a bankroll by its unique name. */
    Optional<Bankroll> findByName(String name);

    /** Returns all active (non-archived) bankrolls. */
    List<Bankroll> findByArchivedAtIsNull();

    /** Returns all archived bankrolls. */
    List<Bankroll> findByArchivedAtIsNotNull();

    /** Returns bankrolls created within the given time range (inclusive). */
    List<Bankroll> findByCreatedAtBetween(Instant start, Instant end);
}
