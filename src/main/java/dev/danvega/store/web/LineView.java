package dev.danvega.store.web;

import dev.danvega.store.catalog.Money;
import dev.danvega.store.catalog.Sticker;

/**
 * A line joined to its sticker for rendering. Cart and order lines both store only a
 * sticker id, because a line is about what was bought, not about how to draw it.
 */
public record LineView(Sticker sticker, int quantity, int unitPriceCents) {

    public int lineTotalCents() {
        return quantity * unitPriceCents;
    }

    public String unitPriceDisplay() {
        return Money.display(unitPriceCents);
    }

    public String lineTotalDisplay() {
        return Money.display(lineTotalCents());
    }
}
