package dev.danvega.store;

import dev.danvega.store.catalog.StickerRepository;
import dev.danvega.store.order.PurchaseOrderRepository;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Goal 7. No price has ever come from the browser, because a cart line stores a sticker
 * id and a quantity and nothing else. These prove that rather than build it, which is the
 * honest shape: the value is showing that a tampered request changes nothing.
 */
@SpringBootTest(properties = {
        "stripe.secret-key=sk_test_not_used",
        "stripe.webhook-secret=" + IntegrationTest.WEBHOOK_SECRET,
        "stripe.base-url=http://localhost:8080" })
@Import(StubStripeConfiguration.class)
class PricingTest extends IntegrationTest {

    @Autowired
    PurchaseOrderRepository orders;

    @Autowired
    StickerRepository stickers;

    @Test
    void a_tampered_quantity_changes_the_quantity_and_nothing_else() {
        addToCart("ship-it");
        var stickerId = stickers.findBySlug("ship-it").orElseThrow().id();
        setQuantity(stickerId, 3);

        var order = orders.findByStripeSessionId(checkout()).orElseThrow();

        assertThat(order.lines()).singleElement()
                .satisfies(line -> {
                    assertThat(line.quantity()).isEqualTo(3);
                    assertThat(line.unitPriceCents()).as("from the sticker table").isEqualTo(99);
                });
        assertThat(order.totalCents()).isEqualTo(297);
    }

    @Test
    void a_price_shaped_parameter_is_ignored_completely() {
        addToCart("ship-it");
        var stickerId = stickers.findBySlug("ship-it").orElseThrow().id();

        // Everything a hopeful attacker would try, in one request.
        client.post().uri("/cart/quantity/{id}?quantity=2&price=1&unitPriceCents=1"
                + "&amount=1&total=1&totalCents=1&priceCents=1", stickerId)
                .cookie("cart_id", cartCookie)
                .exchange()
                .expectStatus().is3xxRedirection();

        var order = orders.findByStripeSessionId(checkout()).orElseThrow();

        assertThat(order.totalCents()).as("two stickers at the real price").isEqualTo(198);
        assertThat(stripe.lastLines()).allSatisfy(line ->
                assertThat(line.unitPriceCents()).isEqualTo(99));
    }

    @Test
    void a_zero_or_negative_quantity_removes_the_line_rather_than_going_negative() {
        addToCart("ship-it");
        addToCart("autowired");
        var shipIt = stickers.findBySlug("ship-it").orElseThrow().id();

        setQuantity(shipIt, -5);

        var order = orders.findByStripeSessionId(checkout()).orElseThrow();

        assertThat(order.lines()).hasSize(1);
        assertThat(order.totalCents()).as("never negative").isEqualTo(99);
    }

    @Test
    void what_stripe_is_asked_to_charge_equals_the_servers_own_sum() {
        addToCart("ship-it");
        addToCart("spring-boot-leaf");
        addToCart("spring-boot-leaf");
        addToCart("autowired");

        var order = orders.findByStripeSessionId(checkout()).orElseThrow();
        var askedOfStripe = stripe.lastLines().stream()
                .mapToInt(line -> line.unitPriceCents() * line.quantity()).sum();

        assertThat(askedOfStripe).isEqualTo(order.totalCents()).isEqualTo(396);
    }

    @Test
    void a_line_over_stripes_limit_shows_the_cart_back_rather_than_a_500() {
        addToCart("ship-it");
        var stickerId = stickers.findBySlug("ship-it").orElseThrow().id();
        setQuantity(stickerId, 1_000_000);

        client.post().uri("/cart/checkout").cookie("cart_id", cartCookie).exchange()
                .expectStatus().isOk()
                .expectBody(String.class).value(page -> assertThat(page)
                        .contains("Stripe will not take")
                        .contains("999999")
                        .contains("Your cart"));

        assertThat(stripe.lastLines())
                .as("Stripe was never called")
                .noneSatisfy(line -> assertThat(line.quantity()).isEqualTo(1_000_000));
    }

    @Test
    void a_huge_quantity_shows_the_true_total_rather_than_wrapping_negative() {
        addToCart("ship-it");
        addToCart("autowired");
        setQuantity(stickers.findBySlug("ship-it").orElseThrow().id(), 2_000_000_000);
        setQuantity(stickers.findBySlug("autowired").orElseThrow().id(), 2_000_000_000);

        // No cap is a recorded decision, so the cart holds any quantity an int can carry.
        // Two lines at two billion overflow an int line total, cart total and item count.
        var cartPage = client.get().uri("/cart").cookie("cart_id", cartCookie).exchange()
                .expectStatus().isOk()
                .expectBody(String.class).returnResult().getResponseBody();
        assertThat(cartPage)
                .as("198,000,000,000 cents a line, and twice that in total")
                .contains("$1980000000.00")
                .contains("$3960000000.00")
                .contains("4000000000 stickers")
                .doesNotContain("$-");

        var storefront = client.get().uri("/").cookie("cart_id", cartCookie).exchange()
                .expectStatus().isOk()
                .expectBody(String.class).returnResult().getResponseBody();
        assertThat(storefront).as("the cart count in the header").contains(">4000000000<");
    }
}
