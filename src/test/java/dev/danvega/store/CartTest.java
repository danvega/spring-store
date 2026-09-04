package dev.danvega.store;

import dev.danvega.store.cart.CartRepository;
import dev.danvega.store.catalog.StickerRepository;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;

import static org.assertj.core.api.Assertions.assertThat;

/** Goal 6, the cart itself. Everything goes through the real endpoints. */
@SpringBootTest(properties = {
        "stripe.secret-key=sk_test_not_used",
        "stripe.webhook-secret=" + IntegrationTest.WEBHOOK_SECRET,
        "stripe.base-url=http://localhost:8080" })
@Import(StubStripeConfiguration.class)
class CartTest extends IntegrationTest {

    @Autowired
    CartRepository carts;

    @Autowired
    StickerRepository stickers;

    private String cartPage() {
        return client.get().uri("/cart").cookie("cart_id", cartCookie).exchange()
                .expectStatus().isOk()
                .expectBody(String.class).returnResult().getResponseBody();
    }

    @Test
    void adding_to_an_empty_cart_creates_it_with_one_line() {
        addToCart("ship-it");

        var cart = carts.findByCartId(cartCookie).orElseThrow();
        assertThat(cart.lines()).hasSize(1);
        assertThat(cart.itemCount()).isEqualTo(1);
        assertThat(cartPage()).contains("Ship It").contains("$0.99");
    }

    @Test
    void adding_the_same_sticker_again_raises_the_quantity() {
        addToCart("ship-it");
        addToCart("ship-it");
        addToCart("ship-it");

        var cart = carts.findByCartId(cartCookie).orElseThrow();
        assertThat(cart.lines()).as("one line, not three").hasSize(1);
        assertThat(cart.itemCount()).isEqualTo(3);
        assertThat(cartPage()).contains("$2.97");
    }

    @Test
    void quantity_can_be_changed_and_a_line_removed() {
        addToCart("autowired");
        var stickerId = stickers.findBySlug("autowired").orElseThrow().id();

        setQuantity(stickerId, 4);
        assertThat(carts.findByCartId(cartCookie).orElseThrow().itemCount()).isEqualTo(4);

        setQuantity(stickerId, 2);
        assertThat(carts.findByCartId(cartCookie).orElseThrow().itemCount()).isEqualTo(2);

        client.post().uri("/cart/remove/{id}", stickerId).cookie("cart_id", cartCookie)
                .exchange().expectStatus().is3xxRedirection();
        assertThat(carts.findByCartId(cartCookie).orElseThrow().lines()).isEmpty();
    }

    @Test
    void removing_the_last_line_leaves_the_empty_cart_screen() {
        addToCart("ship-it");
        var stickerId = stickers.findBySlug("ship-it").orElseThrow().id();
        setQuantity(stickerId, 0);

        assertThat(cartPage())
                .contains("Your cart is empty")
                .doesNotContain("Checkout");
    }

    @Test
    void the_cart_is_in_the_database_so_the_same_cookie_finds_it_again() {
        addToCart("stack-overflow-driven");

        // A different visitor with a different cookie sees nothing.
        var otherPage = client.get().uri("/cart").cookie("cart_id", "someone-else").exchange()
                .expectStatus().isOk()
                .expectBody(String.class).returnResult().getResponseBody();
        assertThat(otherPage).contains("Your cart is empty");

        // The original cookie still finds the cart.
        assertThat(cartPage()).contains("Stack Overflow Driven Development");
    }

    @Test
    void the_storefront_shows_what_is_already_in_the_cart() {
        addToCart("ship-it");
        addToCart("ship-it");

        var page = client.get().uri("/").cookie("cart_id", cartCookie).exchange()
                .expectStatus().isOk()
                .expectBody(String.class).returnResult().getResponseBody();

        assertThat(page).contains("In cart").contains("Cart &middot;").contains(">2<");
    }
}
