package com.sportspredictor.repository;

import com.sportspredictor.entity.OddsSnapshot;
import java.time.Instant;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

/** Spring Data repository for {@link OddsSnapshot} entities. */
public interface OddsSnapshotRepository extends JpaRepository<OddsSnapshot, String> {

    /** Returns all snapshots for the given event. */
    List<OddsSnapshot> findByEventId(String eventId);

    /** Returns all snapshots for the given sport. */
    List<OddsSnapshot> findBySport(String sport);

    /** Returns snapshots for an event from a specific bookmaker. */
    List<OddsSnapshot> findByEventIdAndBookmaker(String eventId, String bookmaker);

    /** Returns snapshots captured within the given time range (inclusive). */
    List<OddsSnapshot> findByCapturedAtBetween(Instant start, Instant end);

    /** Returns snapshots for an event in a specific market. */
    List<OddsSnapshot> findByEventIdAndMarket(String eventId, String market);

    /** Returns all snapshots for the given event ordered by capture time ascending. */
    List<OddsSnapshot> findByEventIdOrderByCapturedAtAsc(String eventId);

    /** Returns all snapshots for the given sport captured after the specified instant. */
    List<OddsSnapshot> findBySportAndCapturedAtAfter(String sport, Instant after);
}
