package dev.danvega.store.cart;

import java.util.Map;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CartService {

    private final CartRepository carts;

    CartService(CartRepository carts) {
        this.carts = carts;
    }

    /** Read only. Does not create a row for a visitor who is just looking. */
    public Cart forCookie(String cartId) {
        return carts.findByCartId(cartId).orElseGet(() -> Cart.forCookie(cartId));
    }

    @Transactional
    public Cart add(String cartId, Long stickerId, int quantity) {
        var cart = carts.findByCartId(cartId).orElseGet(() -> Cart.forCookie(cartId));
        return carts.save(cart.add(stickerId, Math.max(1, quantity)));
    }

    @Transactional
    public Cart setQuantity(String cartId, Long stickerId, int quantity) {
        return carts.findByCartId(cartId)
                .map(cart -> carts.save(cart.setQuantity(stickerId, quantity)))
                .orElseGet(() -> Cart.forCookie(cartId));
    }

    @Transactional
    public Cart remove(String cartId, Long stickerId) {
        return setQuantity(cartId, stickerId, 0);
    }

    /**
     * Takes the paid-for quantities out of the cart, rather than emptying it. A buyer can
     * add more while the Stripe page is open, and wiping the cart would silently delete
     * something they never bought.
     */
    @Transactional
    public void removePurchased(String cartId, Map<Long, Integer> purchased) {
        carts.findByCartId(cartId).ifPresent(cart -> {
            var remaining = cart;
            for (var bought : purchased.entrySet()) {
                remaining = remaining.minus(bought.getKey(), bought.getValue());
            }
            carts.save(remaining);
        });
    }
}
