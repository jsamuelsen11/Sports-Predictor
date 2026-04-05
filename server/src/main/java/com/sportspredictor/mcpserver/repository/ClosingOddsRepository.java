package com.sportspredictor.mcpserver.repository;

import com.sportspredictor.mcpserver.entity.ClosingOdds;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

/** Spring Data repository for {@link ClosingOdds} entities. */
public interface ClosingOddsRepository extends JpaRepository<ClosingOdds, String> {

    /** Returns closing odds for the given event. */
    List<ClosingOdds> findByEventId(String eventId);

    /** Returns closing odds for the given event and market. */
    Optional<ClosingOdds> findByEventIdAndMarket(String eventId, String market);

    /** Returns closing odds for the given sport. */
    List<ClosingOdds> findBySport(String sport);
}
