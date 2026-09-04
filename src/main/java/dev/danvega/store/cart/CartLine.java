package dev.danvega.store.cart;

import org.springframework.data.relational.core.mapping.Table;

/** One sticker in a cart. Part of the Cart aggregate, never loaded on its own. */
@Table("cart_line")
public record CartLine(Long stickerId, int quantity) {

    CartLine plus(int more) {
        return new CartLine(stickerId, quantity + more);
    }

    CartLine withQuantity(int quantity) {
        return new CartLine(stickerId, quantity);
    }
}
