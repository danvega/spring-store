package dev.danvega.store.cart;

import java.time.Instant;
import java.util.LinkedHashSet;
import java.util.Set;

import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;

/**
 * An aggregate root. Spring Data JDBC loads the lines with it and writes them back with
 * it, which is why the lines have no repository of their own.
 */
@Table("cart")
public record Cart(@Id Long id, String cartId, Instant createdAt, Set<CartLine> lines) {

    public static Cart forCookie(String cartId) {
        return new Cart(null, cartId, Instant.now(), new LinkedHashSet<>());
    }

    public boolean isEmpty() {
        return lines.isEmpty();
    }

    /** Total number of stickers, not number of lines. Two of one sticker counts as two. */
    public int itemCount() {
        return lines.stream().mapToInt(CartLine::quantity).sum();
    }

    public int quantityOf(Long stickerId) {
        return lines.stream()
                .filter(line -> line.stickerId().equals(stickerId))
                .mapToInt(CartLine::quantity)
                .findFirst()
                .orElse(0);
    }

    Cart add(Long stickerId, int quantity) {
        var updated = new LinkedHashSet<CartLine>();
        var found = false;
        for (var line : lines) {
            if (line.stickerId().equals(stickerId)) {
                updated.add(line.plus(quantity));
                found = true;
            }
            else {
                updated.add(line);
            }
        }
        if (!found) {
            updated.add(new CartLine(stickerId, quantity));
        }
        return new Cart(id, cartId, createdAt, updated);
    }

    Cart setQuantity(Long stickerId, int quantity) {
        var updated = new LinkedHashSet<CartLine>();
        for (var line : lines) {
            if (!line.stickerId().equals(stickerId)) {
                updated.add(line);
            }
            else if (quantity > 0) {
                updated.add(line.withQuantity(quantity));
            }
        }
        return new Cart(id, cartId, createdAt, updated);
    }

    Cart remove(Long stickerId) {
        return setQuantity(stickerId, 0);
    }

    /** Takes off what was bought, leaving anything added since. */
    Cart minus(Long stickerId, int quantity) {
        return setQuantity(stickerId, quantityOf(stickerId) - quantity);
    }

    Cart emptied() {
        return new Cart(id, cartId, createdAt, new LinkedHashSet<>());
    }
}
