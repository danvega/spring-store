package dev.danvega.store.checkout;

import dev.danvega.store.catalog.Sticker;

/**
 * The seam between this app and Stripe's network. Exists so the checkout flow can be
 * proved without a live Stripe account.
 */
public interface StripeGateway {

    CheckoutSession start(Sticker sticker, String successUrl, String cancelUrl);

    record CheckoutSession(String id, String url) {
    }
}
