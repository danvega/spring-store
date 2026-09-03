package dev.danvega.store.webhook;

import java.time.Instant;

import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;

/** A Stripe event this app has already handled. Written once, never updated. */
@Table("stripe_event")
public record StripeEvent(@Id Long id, String stripeEventId, String type, Instant receivedAt) {

    public static StripeEvent received(String stripeEventId, String type) {
        return new StripeEvent(null, stripeEventId, type, Instant.now());
    }
}
