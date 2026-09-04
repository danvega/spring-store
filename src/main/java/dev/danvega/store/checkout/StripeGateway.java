package dev.danvega.store.checkout;

import java.util.List;

/**
 * The seam between this app and Stripe's network. Exists so the checkout flow can be
 * proved without a live Stripe account.
 */
public interface StripeGateway {

    /**
     * The order reference goes to Stripe as client_reference_id and comes back on the
     * webhook, which is how an order whose session id was never attached is still found.
     */
    CheckoutSession start(List<Line> lines, String orderReference, String successUrl, String cancelUrl);

    /** What Stripe needs per line: a name to show, a unit price, and how many. */
    record Line(String name, int unitPriceCents, int quantity) {
    }

    record CheckoutSession(String id, String url) {
    }
}
