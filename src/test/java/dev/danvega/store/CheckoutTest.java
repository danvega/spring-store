package dev.danvega.store;

import dev.danvega.store.order.PurchaseOrderRepository;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(properties = {
        "stripe.secret-key=sk_test_not_used",
        "stripe.webhook-secret=" + IntegrationTest.WEBHOOK_SECRET,
        "stripe.base-url=http://localhost:8080" })
@Import(StubStripeConfiguration.class)
class CheckoutTest extends IntegrationTest {

    @Autowired
    PurchaseOrderRepository orders;


    @Test
    void buying_redirects_to_stripe_and_leaves_a_pending_order() {
        var sessionId = buy("ship-it");
        var order = orders.findByStripeSessionId(sessionId).orElseThrow();

        assertThat(order.totalCents()).isEqualTo(99);
        assertThat(order.reference()).matches("SPR-[2-9A-HJ-NP-Z]{4}-[2-9A-HJ-NP-Z]{4}");
        assertThat(order.isRecorded()).as("the app has not heard from Stripe yet").isFalse();
        assertThat(order.paidAtStripe()).isNull();
    }

    @Test
    void buying_the_same_sticker_twice_creates_two_orders() {
        var first = buy("ship-it");
        var second = buy("ship-it");

        assertThat(first).isNotEqualTo(second);
        assertThat(orders.findByStripeSessionId(first)).isPresent();
        assertThat(orders.findByStripeSessionId(second)).isPresent();
    }

    @Test
    void a_stripe_failure_leaves_a_pending_order_rather_than_nothing() {
        var nullSessionOrdersBefore = orders.findAll().stream()
                .filter(order -> order.stripeSessionId() == null).count();

        addToCart("ship-it");
        stripe.failNext();
        client.post().uri("/cart/checkout").cookie("cart_id", cartCookie)
                .exchange().expectStatus().is5xxServerError();

        var nullSessionOrdersAfter = orders.findAll().stream()
                .filter(order -> order.stripeSessionId() == null).count();

        assertThat(nullSessionOrdersAfter)
                .as("the order is written before Stripe is called, so the failure leaves a record")
                .isEqualTo(nullSessionOrdersBefore + 1);
    }

    @Test
    void an_unknown_sticker_is_a_404() {
        client.post().uri("/cart/add/not-a-real-sticker").cookie("cart_id", cartCookie)
                .exchange().expectStatus().isNotFound();
    }
}
