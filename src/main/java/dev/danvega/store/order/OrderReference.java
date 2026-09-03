package dev.danvega.store.order;

import java.security.SecureRandom;
import java.util.random.RandomGenerator;

/**
 * Human readable order references, shaped SPR-4Q8T-2M19, for matching an order
 * against the Stripe dashboard by eye. Deliberately not the Stripe session id.
 */
public final class OrderReference {

    /** No 0/O/1/I, because these get read aloud and typed by hand. */
    private static final char[] ALPHABET = "23456789ABCDEFGHJKLMNPQRSTUVWXYZ".toCharArray();

    private static final RandomGenerator DEFAULT = new SecureRandom();

    private OrderReference() {
    }

    public static String next() {
        return next(DEFAULT);
    }

    public static String next(RandomGenerator random) {
        return "SPR-" + block(random) + "-" + block(random);
    }

    private static String block(RandomGenerator random) {
        var block = new StringBuilder(4);
        for (int i = 0; i < 4; i++) {
            block.append(ALPHABET[random.nextInt(ALPHABET.length)]);
        }
        return block.toString();
    }
}
