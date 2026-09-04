package dev.danvega.store.order;

import java.time.Instant;
import java.util.LinkedHashSet;
import java.util.Set;

import dev.danvega.store.catalog.Money;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;

/**
 * An order is two independent facts, not one status.
 * paidAtStripe is what Stripe says. recordedAt is what this app knows.
 * They are always set together by recordPayment, and confirmation.jte relies on that,
 * so anything setting one without the other brings back a NullPointerException.
 *
 * The total is stored rather than summed on read, because it is what the Stripe session
 * was created for and goal 8 compares Stripe's amount against it.
 */
@Table("purchase_order")
public record PurchaseOrder(
        @Id Long id,
        String reference,
        int totalCents,
        String stripeSessionId,
        Instant createdAt,
        Instant paidAtStripe,
        Instant recordedAt,
        String cartId,
        Set<OrderLine> lines) {

    /**
     * Written before the Stripe session exists, so there is no session id yet. That
     * ordering is deliberate: a failure now leaves a harmless pending order rather than a
     * payable Stripe session this app has never heard of.
     */
    public static PurchaseOrder pending(String reference, Set<OrderLine> lines, String cartId) {
        return new PurchaseOrder(null, reference, totalOf(lines), null, Instant.now(), null, null,
                cartId, new LinkedHashSet<>(lines));
    }

    private static int totalOf(Set<OrderLine> lines) {
        return lines.stream().mapToInt(OrderLine::lineTotalCents).sum();
    }

    public PurchaseOrder attachSession(String stripeSessionId) {
        return new PurchaseOrder(id, reference, totalCents, stripeSessionId,
                createdAt, paidAtStripe, recordedAt, cartId, lines);
    }

    public boolean isRecorded() {
        return recordedAt != null;
    }

    public PurchaseOrder recordPayment(Instant paidAtStripe, Instant recordedAt) {
        return new PurchaseOrder(id, reference, totalCents, stripeSessionId,
                createdAt, paidAtStripe, recordedAt, cartId, lines);
    }

    public int itemCount() {
        return lines.stream().mapToInt(OrderLine::quantity).sum();
    }

    public String totalDisplay() {
        return Money.display(totalCents);
    }
}
