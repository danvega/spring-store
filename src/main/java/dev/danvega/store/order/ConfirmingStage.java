package dev.danvega.store.order;

import java.time.Duration;
import java.time.Instant;

/**
 * What the confirming page is allowed to say, based on how long the buyer has been
 * waiting.
 *
 * Measured from when they arrived back from Stripe, never from when the order was
 * created. The gap between those two is however long they spent typing their card, so a
 * created_at clock would tell an unhurried buyer that the store has no record before the
 * page had waited at all.
 */
public enum ConfirmingStage {

    /** Normal. The webhook is expected within a second or two. */
    WAITING,

    /** Slower than usual. Say so, and say the payment itself is safe. */
    TAKING_LONGER,

    /** Long enough that claiming it will resolve would be dishonest. */
    NOT_RECORDED;

    public static final Duration SLOW_AFTER = Duration.ofSeconds(15);

    public static final Duration GIVE_UP_AFTER = Duration.ofSeconds(60);

    public static ConfirmingStage of(Instant arrivedAt, Instant now) {
        var waited = Duration.between(arrivedAt, now);
        if (waited.compareTo(GIVE_UP_AFTER) >= 0) {
            return NOT_RECORDED;
        }
        if (waited.compareTo(SLOW_AFTER) >= 0) {
            return TAKING_LONGER;
        }
        return WAITING;
    }

    /** A page that has stopped claiming it will resolve should stop acting like it will. */
    public boolean keepRefreshing() {
        return this != NOT_RECORDED;
    }
}
