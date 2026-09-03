package dev.danvega.store.webhook;

import java.time.Instant;

import dev.danvega.store.order.PurchaseOrderRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Turns a verified Stripe event into the app's own record that the order is paid. */
@Service
public class OrderRecorder {

    private final PurchaseOrderRepository orders;
    private final StripeEventRepository events;

    OrderRecorder(PurchaseOrderRepository orders, StripeEventRepository events) {
        this.orders = orders;
        this.events = events;
    }

    /**
     * Claiming the event and recording the payment share one transaction on purpose. If
     * the recording fails, the claim rolls back with it, so Stripe's retry is still able
     * to do the work.
     *
     * The existence check handles the ordinary redelivery. The unique constraint handles
     * two identical deliveries arriving at once: one of them fails to insert, the
     * transaction rolls back, and Stripe retries into the existence check. That is why
     * there is no catch here, and why the constraint is not decoration.
     */
    @Transactional
    public RecordOutcome record(String stripeEventId, String type, String stripeSessionId, Instant paidAtStripe) {
        if (events.existsByStripeEventId(stripeEventId)) {
            return new RecordOutcome.AlreadySeen(stripeEventId);
        }
        var order = orders.findByStripeSessionId(stripeSessionId).orElse(null);
        if (order == null) {
            // Deliberately not claimed. An event we could not act on has not been handled,
            // and claiming it would burn the only retry that could ever record this
            // payment. A payment the app does not know about is the exact failure this
            // project exists to catch, so the door stays open.
            return new RecordOutcome.NoMatchingOrder(stripeSessionId);
        }

        events.save(StripeEvent.received(stripeEventId, type));
        return new RecordOutcome.Recorded(orders.save(order.recordPayment(paidAtStripe, Instant.now())));
    }
}
