package dev.danvega.store.order;

import java.util.Optional;

import org.springframework.data.repository.ListCrudRepository;

public interface PurchaseOrderRepository extends ListCrudRepository<PurchaseOrder, Long> {

    Optional<PurchaseOrder> findByStripeSessionId(String stripeSessionId);

    Optional<PurchaseOrder> findByReference(String reference);
}
