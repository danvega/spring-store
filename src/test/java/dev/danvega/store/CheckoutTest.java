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

    @Autowired
    StubStripeConfiguration.StubStripeGateway stripe;

    @Test
    void buying_redirects_to_stripe_and_leaves_a_pending_order() {
        client.post().uri("/buy/ship-it").exchange().expectStatus().is3xxRedirection();

        var sessionId = stripe.lastSessionId();
        var order = orders.findByStripeSessionId(sessionId).orElseThrow();

        assertThat(order.amountCents()).isEqualTo(99);
        assertThat(order.reference()).matches("SPR-[2-9A-HJ-NP-Z]{4}-[2-9A-HJ-NP-Z]{4}");
        assertThat(order.isRecorded()).as("the app has not heard from Stripe yet").isFalse();
        assertThat(order.paidAtStripe()).isNull();
    }

    @Test
    void buying_the_same_sticker_twice_creates_two_orders() {
        client.post().uri("/buy/ship-it").exchange().expectStatus().is3xxRedirection();
        var first = stripe.lastSessionId();
        client.post().uri("/buy/ship-it").exchange().expectStatus().is3xxRedirection();
        var second = stripe.lastSessionId();

        assertThat(first).isNotEqualTo(second);
        assertThat(orders.findByStripeSessionId(first)).isPresent();
        assertThat(orders.findByStripeSessionId(second)).isPresent();
    }

    @Test
    void an_unknown_sticker_is_a_404() {
        client.post().uri("/buy/not-a-real-sticker").exchange().expectStatus().isNotFound();
    }
}
