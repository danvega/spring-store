package dev.danvega.store;

import java.time.Instant;

import dev.danvega.store.catalog.StickerRepository;
import dev.danvega.store.order.PurchaseOrder;
import dev.danvega.store.order.PurchaseOrderRepository;
import dev.danvega.store.webhook.StripeEventRepository;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Goal 4. Stripe redelivers on any timeout or non-2xx, and the redelivery carries the
 * same event id and the same body with a fresh signature. These tests send exactly that,
 * rather than two events the test invented, which would prove nothing.
 */
@SpringBootTest(properties = {
        "stripe.secret-key=sk_test_not_used",
        "stripe.webhook-secret=" + IntegrationTest.WEBHOOK_SECRET,
        "stripe.base-url=http://localhost:8080" })
@Import(StubStripeConfiguration.class)
class ReplayTest extends IntegrationTest {

    @Autowired
    PurchaseOrderRepository orders;

    @Autowired
    StripeEventRepository events;

    @Autowired
    StubStripeConfiguration.StubStripeGateway stripe;

    @Autowired
    StickerRepository stickers;

    private String buy(String slug) {
        client.post().uri("/buy/{slug}", slug).exchange().expectStatus().is3xxRedirection();
        return stripe.lastSessionId();
    }

    /** One delivery of an already-built payload, signed now the way a retry would be. */
    private void deliver(String payload) {
        client.post().uri("/stripe/webhook")
                .contentType(MediaType.APPLICATION_JSON)
                .header("Stripe-Signature", stripeSignature(payload, Instant.now()))
                .body(payload)
                .exchange()
                .expectStatus().isOk();
    }

    @Test
    void a_redelivered_event_is_not_processed_twice() {
        var sessionId = buy("stack-overflow-driven");
        var eventId = "evt_test_" + sessionId;
        var payload = completedEvent(sessionId, Instant.now());

        deliver(payload);
        var afterFirst = orders.findByStripeSessionId(sessionId).orElseThrow();
        assertThat(afterFirst.isRecorded()).isTrue();

        // Stripe retries. Identical body, identical event id, fresh signature.
        deliver(payload);
        var afterSecond = orders.findByStripeSessionId(sessionId).orElseThrow();

        assertThat(events.countByStripeEventId(eventId))
                .as("the event is claimed exactly once")
                .isEqualTo(1);
        assertThat(afterSecond.recordedAt())
                .as("recorded_at must not move, or the redelivery was processed")
                .isEqualTo(afterFirst.recordedAt());
        assertThat(afterSecond.paidAtStripe()).isEqualTo(afterFirst.paidAtStripe());
    }

    @Test
    void the_guard_keys_on_the_event_not_the_session() {
        var first = buy("ship-it");
        var second = buy("spring-boot-leaf");

        deliver(completedEvent(first, Instant.now()));
        deliver(completedEvent(second, Instant.now()));

        assertThat(orders.findByStripeSessionId(first).orElseThrow().isRecorded()).isTrue();
        assertThat(orders.findByStripeSessionId(second).orElseThrow().isRecorded()).isTrue();
        assertThat(events.countByStripeEventId("evt_test_" + first)).isEqualTo(1);
        assertThat(events.countByStripeEventId("evt_test_" + second)).isEqualTo(1);
    }

    @Test
    void an_event_with_no_order_is_not_claimed_so_it_can_still_be_recorded_later() {
        // No order was ever created for this session. That happens if the checkout
        // insert failed after Stripe already had a payable session.
        var sessionId = "cs_test_orphaned_session";
        var eventId = "evt_test_" + sessionId;
        var payload = completedEvent(sessionId, Instant.now());

        deliver(payload);
        assertThat(events.countByStripeEventId(eventId))
                .as("an event with nothing to act on must not burn its retry")
                .isZero();

        // The order is put right, and Stripe redelivers.
        var sticker = stickers.findBySlug("ship-it").orElseThrow();
        orders.save(PurchaseOrder.pending("SPR-FIXT-0001", sticker.id(), 99).attachSession(sessionId));
        deliver(payload);

        assertThat(orders.findByStripeSessionId(sessionId).orElseThrow().isRecorded()).isTrue();
        assertThat(events.countByStripeEventId(eventId)).isEqualTo(1);
    }

    @Test
    void a_rejected_event_is_not_claimed_so_stripe_can_retry_it() {
        var sessionId = buy("it-works-on-my-machine");
        var eventId = "evt_test_" + sessionId;
        var payload = completedEvent(sessionId, Instant.now());

        client.post().uri("/stripe/webhook")
                .contentType(MediaType.APPLICATION_JSON)
                .header("Stripe-Signature", "t=1,v1=notarealsignature")
                .body(payload)
                .exchange()
                .expectStatus().isBadRequest();

        assertThat(events.countByStripeEventId(eventId)).isZero();

        // Stripe retries with a good signature and it goes through.
        deliver(payload);

        assertThat(orders.findByStripeSessionId(sessionId).orElseThrow().isRecorded()).isTrue();
        assertThat(events.countByStripeEventId(eventId)).isEqualTo(1);
    }
}
