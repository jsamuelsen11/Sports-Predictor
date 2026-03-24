package com.sportspredictor.repository;

import com.sportspredictor.entity.PredictionLog;
import com.sportspredictor.entity.enums.PredictionType;
import java.time.Instant;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

/** Spring Data repository for {@link PredictionLog} entities. */
public interface PredictionLogRepository extends JpaRepository<PredictionLog, String> {

    /** Returns all predictions for the given event. */
    List<PredictionLog> findByEventId(String eventId);

    /** Returns all predictions for the given sport. */
    List<PredictionLog> findBySport(String sport);

    /** Returns all predictions of the given type. */
    List<PredictionLog> findByPredictionType(PredictionType predictionType);

    /** Returns predictions created within the given time range (inclusive). */
    List<PredictionLog> findByCreatedAtBetween(Instant start, Instant end);

    /** Returns predictions that have not yet been settled. */
    List<PredictionLog> findByActualOutcomeIsNull();
}
