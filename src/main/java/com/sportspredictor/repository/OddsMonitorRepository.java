package com.sportspredictor.repository;

import com.sportspredictor.entity.OddsMonitor;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

/** Spring Data repository for {@link OddsMonitor} entities. */
public interface OddsMonitorRepository extends JpaRepository<OddsMonitor, String> {

    /** Returns active monitors for the given event. */
    List<OddsMonitor> findByEventIdAndActiveTrue(String eventId);

    /** Returns all active monitors. */
    List<OddsMonitor> findByActiveTrue();
}
