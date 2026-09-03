package dev.danvega.store.checkout;

public class CheckoutFailedException extends RuntimeException {

    public CheckoutFailedException(String message, Throwable cause) {
        super(message, cause);
    }
}
