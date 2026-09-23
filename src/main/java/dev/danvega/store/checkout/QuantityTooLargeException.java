package dev.danvega.store.checkout;

/**
 * This project deliberately caps nothing, but Stripe caps a line item at 999,999. That
 * boundary belongs to them, so it gets reported rather than adopted as policy, and it is
 * caught here rather than surfacing as a 500 from their API.
 */
public class QuantityTooLargeException extends RuntimeException {

    public static final int STRIPE_MAX_QUANTITY = 999_999;

    public QuantityTooLargeException(String stickerName, int quantity) {
        super("Stripe will not take %d of %s in one line. Its limit is %d."
                .formatted(quantity, stickerName, STRIPE_MAX_QUANTITY));
    }
}
