package com.sportspredictor.repository;

import com.sportspredictor.entity.Subscription;
import com.sportspredictor.entity.enums.SubscriptionType;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

/** Spring Data repository for {@link Subscription} entities. */
public interface SubscriptionRepository extends JpaRepository<Subscription, String> {

    /** Returns subscriptions of a given type that are active. */
    List<Subscription> findByTypeAndActive(SubscriptionType type, boolean active);

    /** Returns all active subscriptions. */
    List<Subscription> findByActive(boolean active);
}
