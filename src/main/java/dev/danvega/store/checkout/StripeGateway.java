package dev.danvega.store.checkout;

import dev.danvega.store.catalog.Sticker;

/**
 * The seam between this app and Stripe's network. Exists so the checkout flow can be
 * proved without a live Stripe account.
 */
public interface StripeGateway {

    /**
     * The order reference goes to Stripe as client_reference_id and comes back on the
     * webhook, which is how an order whose session id was never attached is still found.
     */
    CheckoutSession start(Sticker sticker, String orderReference, String successUrl, String cancelUrl);

    record CheckoutSession(String id, String url) {
    }
}
