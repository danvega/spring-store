package dev.danvega.store.catalog;

/** One place that turns cents into the string every screen shows. */
public final class Money {

    private Money() {
    }

    public static String display(int cents) {
        return "$%d.%02d".formatted(cents / 100, cents % 100);
    }
}
