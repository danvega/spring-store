package dev.danvega.store.order;

import dev.danvega.store.catalog.Money;
import org.springframework.data.relational.core.mapping.Table;

/**
 * The unit price is copied here at checkout rather than joined from the sticker at read
 * time. A later price change must not rewrite what somebody already paid.
 */
@Table("order_line")
public record OrderLine(Long stickerId, int quantity, int unitPriceCents) {

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
