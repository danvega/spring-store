package dev.danvega.store.checkout;

import dev.danvega.store.catalog.Sticker;
import dev.danvega.store.order.OrderReference;
import dev.danvega.store.order.PurchaseOrder;
import dev.danvega.store.order.PurchaseOrderRepository;
import org.springframework.stereotype.Service;

/**
 * Order first, Stripe second, session id third.
 *
 * Two systems cannot share a transaction, so an orphan is always possible somewhere. What
 * this ordering decides is which orphan you get. Calling Stripe first and writing second
 * leaves a payable session this app has never heard of, which is the worst one. Writing
 * first leaves a pending order that can never be paid, which is harmless, and the order
 * reference travels to Stripe as client_reference_id so the webhook can still find the
 * order even if the third step never happens.
 *
 * Deliberately not @Transactional. Holding a database connection open across a network
 * call to Stripe buys nothing here, and the two writes are independently meaningful.
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

    public String start(Sticker sticker) {
        var order = orders.save(
                PurchaseOrder.pending(OrderReference.next(), sticker.id(), sticker.priceCents()));

        var session = stripe.start(sticker, order.reference(), successUrl(), cancelUrl(sticker));

        orders.save(order.attachSession(session.id()));
        return session.url();
    }

    private String successUrl() {
        return properties.baseUrl() + "/order/confirm?session_id=" + SESSION_ID_PLACEHOLDER;
    }

    private String cancelUrl(Sticker sticker) {
        return properties.baseUrl() + "/cancelled?sticker=" + sticker.slug();
    }
}
