package dev.danvega.store.webhook;

import java.time.Instant;

import dev.danvega.store.cart.CartService;
import dev.danvega.store.order.PurchaseOrderRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Turns a verified Stripe event into the app's own record that the order is paid. */
@Service
public class OrderRecorder {

    private static final Logger log = LoggerFactory.getLogger(OrderRecorder.class);

    private final PurchaseOrderRepository orders;
    private final StripeEventRepository events;
    private final CartService carts;

    OrderRecorder(PurchaseOrderRepository orders, StripeEventRepository events, CartService carts) {
        this.orders = orders;
        this.events = events;
        this.carts = carts;
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
            String clientReferenceId, Long amountTotal, Instant paidAtStripe) {
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

        // Goal 8. What Stripe says it charged and what the store computed are two
        // independent facts, exactly like the two timestamps on an order. Recording a
        // payment without comparing them means trusting a number nobody checked.
        // A missing amount is refused too. This check exists to avoid trusting a number
        // nobody verified, so failing to read the number is not a reason to pass. The
        // deserializeUnsafe fallback in the controller can leave fields unmapped when
        // Stripe's API version and the SDK's disagree, and that is exactly when this
        // would otherwise fail open.
        if (amountTotal == null || amountTotal != order.totalCents()) {
            log.error("Refusing event {}: order {} totals {} cents but Stripe reported {}",
                    stripeEventId, order.reference(), order.totalCents(), amountTotal);
            return new RecordOutcome.AmountMismatch(order.reference(), order.totalCents(),
                    amountTotal == null ? -1 : amountTotal);
        }

        events.save(StripeEvent.received(stripeEventId, type));

        // Matched by reference means the session id never landed. Attach it now so the
        // confirming page and any redelivery find this order the direct way.
        var matched = order.stripeSessionId() == null ? order.attachSession(stripeSessionId) : order;
        var recorded = orders.save(matched.recordPayment(paidAtStripe, Instant.now()));

        // The cart is reduced when the payment is recorded, not when checkout starts.
        // Emptying earlier would lose the cart of anyone who cancels at Stripe, and
        // emptying it wholesale would delete anything added while Stripe was open.
        if (recorded.cartId() != null) {
            carts.removePurchased(recorded.cartId(), recorded.lines().stream()
                    .collect(java.util.stream.Collectors.toMap(
                            dev.danvega.store.order.OrderLine::stickerId,
                            dev.danvega.store.order.OrderLine::quantity)));
        }
        return new RecordOutcome.Recorded(recorded);
    }
}
