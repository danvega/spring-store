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
    public RecordOutcome record(String stripeEventId, String type, String stripeSessionId,
            String clientReferenceId, Instant paidAtStripe) {
        if (events.existsByStripeEventId(stripeEventId)) {
            return new RecordOutcome.AlreadySeen(stripeEventId);
        }
        // The session id is the normal match. The reference is the fallback for an order
        // whose session id was never attached, which happens if checkout died between
        // creating the Stripe session and writing it back.
        var order = orders.findByStripeSessionId(stripeSessionId)
                .or(() -> clientReferenceId == null ? java.util.Optional.<dev.danvega.store.order.PurchaseOrder>empty()
                        : orders.findByReference(clientReferenceId))
                .orElse(null);
        if (order == null) {
            // Deliberately not claimed. An event we could not act on has not been handled,
            // and claiming it would burn the only retry that could ever record this
            // payment. A payment the app does not know about is the exact failure this
            // project exists to catch, so the door stays open.
            return new RecordOutcome.NoMatchingOrder(stripeSessionId);
        }

        events.save(StripeEvent.received(stripeEventId, type));

        // Matched by reference means the session id never landed. Attach it now so the
        // confirming page and any redelivery find this order the direct way.
        var matched = order.stripeSessionId() == null ? order.attachSession(stripeSessionId) : order;
        return new RecordOutcome.Recorded(orders.save(matched.recordPayment(paidAtStripe, Instant.now())));
    }
}
