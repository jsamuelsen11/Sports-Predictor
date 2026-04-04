package com.sportspredictor.mcpserver.repository;

import com.sportspredictor.mcpserver.entity.BetLeg;
import com.sportspredictor.mcpserver.entity.enums.BetLegStatus;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

/** Spring Data repository for {@link BetLeg} entities. */
public interface BetLegRepository extends JpaRepository<BetLeg, String> {

    /** Returns all legs belonging to the given bet. */
    List<BetLeg> findByBetId(String betId);

    /** Returns all legs for the given event. */
    List<BetLeg> findByEventId(String eventId);

    /** Returns all legs with the given status. */
    List<BetLeg> findByStatus(BetLegStatus status);
}
