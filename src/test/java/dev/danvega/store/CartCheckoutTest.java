package dev.danvega.store;

import java.time.Instant;

import dev.danvega.store.cart.CartRepository;
import dev.danvega.store.catalog.StickerRepository;
import dev.danvega.store.order.OrderLine;
import dev.danvega.store.order.PurchaseOrderRepository;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Goal 6's acceptance: a real cart becomes one Stripe session and one order. Built by
 * clicking add the way a person would, never by assembling an order row.
 */
@SpringBootTest(properties = {
        "stripe.secret-key=sk_test_not_used",
        "stripe.webhook-secret=" + IntegrationTest.WEBHOOK_SECRET,
        "stripe.base-url=http://localhost:8080" })
@Import(StubStripeConfiguration.class)
class CartCheckoutTest extends IntegrationTest {

    @Autowired
    PurchaseOrderRepository orders;

    @Autowired
    CartRepository carts;

    @Autowired
    StickerRepository stickers;

    /** Three different stickers, one of them at quantity two. Four items, $3.96. */
    private String aCartOfThree() {
        addToCart("ship-it");
        addToCart("stack-overflow-driven");
        addToCart("stack-overflow-driven");
        addToCart("autowired");
        return checkout();
    }

    @Test
    void a_cart_of_three_becomes_one_session_and_one_order() {
        var sessionId = aCartOfThree();
        var order = orders.findByStripeSessionId(sessionId).orElseThrow();

        assertThat(order.lines()).as("three lines, not four rows").hasSize(3);
        assertThat(order.itemCount()).isEqualTo(4);
        assertThat(order.totalCents()).isEqualTo(396);
        assertThat(order.totalDisplay()).isEqualTo("$3.96");

        var doubled = order.lines().stream()
                .filter(line -> line.quantity() == 2).findFirst().orElseThrow();
        assertThat(doubled.unitPriceCents()).isEqualTo(99);
        assertThat(doubled.lineTotalCents()).isEqualTo(198);
    }

    @Test
    void stripe_is_asked_to_charge_exactly_what_the_cart_held() {
        aCartOfThree();

        assertThat(stripe.lastLines()).hasSize(3);
        assertThat(stripe.lastLines().stream().mapToInt(l -> l.unitPriceCents() * l.quantity()).sum())
                .as("what Stripe was asked for equals the order total")
                .isEqualTo(396);
        assertThat(stripe.lastLines()).allSatisfy(line ->
                assertThat(line.unitPriceCents()).as("prices come from the sticker table").isEqualTo(99));
    }

    @Test
    void the_total_is_the_servers_own_arithmetic_over_its_own_prices() {
        addToCart("ship-it");
        var stickerId = stickers.findBySlug("ship-it").orElseThrow().id();
        setQuantity(stickerId, 7);

        var order = orders.findByStripeSessionId(checkout()).orElseThrow();

        assertThat(order.totalCents()).isEqualTo(7 * 99);
        assertThat(order.lines()).singleElement()
                .extracting(OrderLine::quantity, OrderLine::unitPriceCents)
                .containsExactly(7, 99);
    }

    @Test
    void the_cart_survives_checkout_and_empties_only_when_the_payment_is_recorded() {
        var sessionId = aCartOfThree();

        assertThat(carts.findByCartId(cartCookie).orElseThrow().isEmpty())
                .as("cancelling at Stripe must not cost you your cart")
                .isFalse();

        var at = Instant.now();
        var payload = completedEvent(sessionId, at);
        client.post().uri("/stripe/webhook")
                .contentType(MediaType.APPLICATION_JSON)
                .header("Stripe-Signature", stripeSignature(payload, at))
                .body(payload)
                .exchange()
                .expectStatus().isOk();

        assertThat(carts.findByCartId(cartCookie).orElseThrow().isEmpty())
                .as("emptied once the payment is recorded")
                .isTrue();
    }

    @Test
    void adding_more_while_stripe_is_open_survives_the_payment() {
        var sessionId = aCartOfThree();

        // The Stripe page is open in another tab and they keep shopping.
        addToCart("spring-boot-leaf");
        addToCart("ship-it");

        var at = Instant.now();
        var payload = completedEvent(sessionId, at);
        client.post().uri("/stripe/webhook")
                .contentType(MediaType.APPLICATION_JSON)
                .header("Stripe-Signature", stripeSignature(payload, at))
                .body(payload)
                .exchange()
                .expectStatus().isOk();

        var cart = carts.findByCartId(cartCookie).orElseThrow();
        var leafId = stickers.findBySlug("spring-boot-leaf").orElseThrow().id();
        var shipItId = stickers.findBySlug("ship-it").orElseThrow().id();

        assertThat(cart.quantityOf(leafId))
                .as("added after checkout, never paid for, must not be deleted")
                .isEqualTo(1);
        assertThat(cart.quantityOf(shipItId))
                .as("one was bought and one was added since, so one remains")
                .isEqualTo(1);
        assertThat(cart.itemCount()).isEqualTo(2);
    }

    @Test
    void lines_render_in_catalog_order_not_hash_order() {
        addToCart("ship-it");
        addToCart("spring-boot-leaf");
        addToCart("autowired");

        var page = client.get().uri("/cart").cookie("cart_id", cartCookie).exchange()
                .expectStatus().isOk()
                .expectBody(String.class).returnResult().getResponseBody();

        // sort_order is 1 for Spring Boot Leaf, 4 for @Autowired, 6 for Ship It.
        assertThat(page.indexOf("Spring Boot Leaf")).isLessThan(page.indexOf("@Autowired"));
        assertThat(page.indexOf("@Autowired")).isLessThan(page.indexOf("Ship It"));
    }

    @Test
    void the_confirmation_lists_every_line_not_just_the_first() {
        var sessionId = aCartOfThree();

        var at = Instant.now();
        var payload = completedEvent(sessionId, at);
        client.post().uri("/stripe/webhook")
                .contentType(MediaType.APPLICATION_JSON)
                .header("Stripe-Signature", stripeSignature(payload, at))
                .body(payload)
                .exchange()
                .expectStatus().isOk();

        client.get().uri("/order/confirm?session_id={id}", sessionId).exchange()
                .expectStatus().isOk()
                .expectBody(String.class).value(page -> assertThat(page)
                        .contains("Payment received")
                        .contains("Ship It")
                        .contains("Stack Overflow Driven Development")
                        .contains("@Autowired")
                        .contains("$3.96")
                        .contains("4 stickers paid for"));
    }
}
