package dev.danvega.store.webhook;

import dev.danvega.store.order.PurchaseOrder;

/** What happened when a verified Stripe event reached the recorder. */
public sealed interface RecordOutcome {

    /** First time seeing this event, and it matched an order. */
    record Recorded(PurchaseOrder order) implements RecordOutcome {
    }

    /** Stripe redelivered something already handled. Nothing was written. */
    record AlreadySeen(String stripeEventId) implements RecordOutcome {
    }

    /** A genuine event for a checkout session this app never created. */
    record NoMatchingOrder(String stripeSessionId) implements RecordOutcome {
    }

    /**
     * Stripe says it charged an amount the order does not agree with. Nothing is
     * recorded and the event is not claimed, so a corrected redelivery can still work.
     */
    record AmountMismatch(String reference, int expectedCents, long chargedCents) implements RecordOutcome {
    }
}
