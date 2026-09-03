package dev.danvega.store.order;

import java.time.Instant;

import dev.danvega.store.catalog.Money;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;

/**
 * An order is two independent facts, not one status.
 * paidAtStripe is what Stripe says. recordedAt is what this app knows.
 * They are set together today, but they answer different questions and the
 * confirming screens depend on being able to tell them apart.
 */
@Table("purchase_order")
public record PurchaseOrder(
        @Id Long id,
        String reference,
        Long stickerId,
        int amountCents,
        String stripeSessionId,
        Instant createdAt,
        Instant paidAtStripe,
        Instant recordedAt) {

    public static PurchaseOrder pending(String reference, Long stickerId, int amountCents, String stripeSessionId) {
        return new PurchaseOrder(null, reference, stickerId, amountCents, stripeSessionId, Instant.now(), null, null);
    }

    public boolean isRecorded() {
        return recordedAt != null;
    }

    public PurchaseOrder recordPayment(Instant paidAtStripe, Instant recordedAt) {
        return new PurchaseOrder(id, reference, stickerId, amountCents, stripeSessionId,
                createdAt, paidAtStripe, recordedAt);
    }

    public String amountDisplay() {
        return Money.display(amountCents);
    }
}
