package dev.danvega.store.checkout;

import java.util.LinkedHashSet;
import java.util.List;

import dev.danvega.store.cart.Cart;
import dev.danvega.store.catalog.Sticker;
import dev.danvega.store.catalog.StickerRepository;
import dev.danvega.store.order.OrderLine;
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
    private final StickerRepository stickers;
    private final StripeProperties properties;

    CheckoutService(StripeGateway stripe, PurchaseOrderRepository orders, StickerRepository stickers,
            StripeProperties properties) {
        this.stripe = stripe;
        this.orders = orders;
        this.stickers = stickers;
        this.properties = properties;
    }

    /**
     * Every price here is read from the sticker table. Nothing about the amount comes from
     * the browser, which is the whole of goal 7 and the reason a cart was worth building.
     */
    public String start(Cart cart) {
        var orderLines = new LinkedHashSet<OrderLine>();
        var stripeLines = new java.util.ArrayList<StripeGateway.Line>();

        for (var line : cart.lines()) {
            var sticker = stickers.findById(line.stickerId())
                    .orElseThrow(() -> new CheckoutFailedException(
                            "Cart holds sticker " + line.stickerId() + " which no longer exists",
                            new IllegalStateException()));
            if (line.quantity() > QuantityTooLargeException.STRIPE_MAX_QUANTITY) {
                throw new QuantityTooLargeException(sticker.name(), line.quantity());
            }
            orderLines.add(new OrderLine(sticker.id(), line.quantity(), sticker.priceCents()));
            stripeLines.add(new StripeGateway.Line(sticker.name(), sticker.priceCents(), line.quantity()));
        }

        var order = orders.save(PurchaseOrder.pending(OrderReference.next(), orderLines, cart.cartId()));
        var session = stripe.start(List.copyOf(stripeLines), order.reference(), successUrl(), cancelUrl());
        orders.save(order.attachSession(session.id()));
        return session.url();
    }

    private String successUrl() {
        return properties.baseUrl() + "/order/confirm?session_id=" + SESSION_ID_PLACEHOLDER;
    }

    /** No sticker id any more. Cancelling returns to a cart that still holds everything. */
    private String cancelUrl() {
        return properties.baseUrl() + "/cancelled";
    }
}
