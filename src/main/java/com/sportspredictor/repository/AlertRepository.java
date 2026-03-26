package com.sportspredictor.repository;

import com.sportspredictor.entity.Alert;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

/** Spring Data repository for {@link Alert} entities. */
public interface AlertRepository extends JpaRepository<Alert, String> {

    /** Returns all unread alerts. */
    List<Alert> findByReadFalse();

    /** Returns alerts for a specific subscription. */
    List<Alert> findBySubscriptionId(String subscriptionId);
}
