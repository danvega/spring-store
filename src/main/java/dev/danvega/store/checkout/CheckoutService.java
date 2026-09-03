package dev.danvega.store.checkout;

import dev.danvega.store.catalog.Sticker;
import dev.danvega.store.order.OrderReference;
import dev.danvega.store.order.PurchaseOrder;
import dev.danvega.store.order.PurchaseOrderRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Creates the Stripe session and the pending order together. The order row exists
 * before the webhook arrives, so the webhook updates a row rather than racing to
 * create one, and the confirming page has something to poll.
 */
@Service
public class CheckoutService {

    /** Stripe substitutes the real session id into this placeholder on redirect. */
    private static final String SESSION_ID_PLACEHOLDER = "{CHECKOUT_SESSION_ID}";

    private final StripeGateway stripe;
    private final PurchaseOrderRepository orders;
    private final StripeProperties properties;

    CheckoutService(StripeGateway stripe, PurchaseOrderRepository orders, StripeProperties properties) {
        this.stripe = stripe;
        this.orders = orders;
        this.properties = properties;
    }

    @Transactional
    public String start(Sticker sticker) {
        var session = stripe.start(sticker, successUrl(), cancelUrl(sticker));
        orders.save(PurchaseOrder.pending(OrderReference.next(), sticker.id(), sticker.priceCents(), session.id()));
        return session.url();
    }

    private String successUrl() {
        return properties.baseUrl() + "/order/confirm?session_id=" + SESSION_ID_PLACEHOLDER;
    }

    private String cancelUrl(Sticker sticker) {
        return properties.baseUrl() + "/cancelled?sticker=" + sticker.slug();
    }
}
