package dev.danvega.store.webhook;

import org.springframework.data.repository.ListCrudRepository;

public interface StripeEventRepository extends ListCrudRepository<StripeEvent, Long> {

    boolean existsByStripeEventId(String stripeEventId);

    long countByStripeEventId(String stripeEventId);
}
