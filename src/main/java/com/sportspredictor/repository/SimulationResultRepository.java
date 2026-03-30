package com.sportspredictor.repository;

import com.sportspredictor.entity.SimulationResult;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

/** Spring Data repository for {@link SimulationResult} entities. */
public interface SimulationResultRepository extends JpaRepository<SimulationResult, String> {

    /** Returns simulations for the given event. */
    List<SimulationResult> findByEventId(String eventId);

    /** Returns simulations for the given sport. */
    List<SimulationResult> findBySport(String sport);
}
