package dev.danvega.store;

import java.time.Instant;

import dev.danvega.store.catalog.StickerRepository;
import dev.danvega.store.order.OrderLine;
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
 * The state under test is an order whose session id was never attached, which is what
 * checkout leaves behind if it dies between creating the Stripe session and writing it
 * back. The buyer has already paid by then, so the webhook has to find the order anyway.
 */
@SpringBootTest(properties = {
        "stripe.secret-key=sk_test_not_used",
        "stripe.webhook-secret=" + IntegrationTest.WEBHOOK_SECRET,
        "stripe.base-url=http://localhost:8080" })
@Import(StubStripeConfiguration.class)
class RecoveryTest extends IntegrationTest {

    @Autowired
    PurchaseOrderRepository orders;

    @Autowired
    StickerRepository stickers;

    @Autowired
    StripeEventRepository events;

    /** An order that got as far as being written, but never got its session id back. */
    private PurchaseOrder orderWithNoSession(String reference) {
        var sticker = stickers.findBySlug("ship-it").orElseThrow();
        return orders.save(PurchaseOrder.pending(reference,
                java.util.Set.of(new OrderLine(sticker.id(), 1, 99)), null));
    }

    private void deliver(String payload) {
        client.post().uri("/stripe/webhook")
                .contentType(MediaType.APPLICATION_JSON)
                .header("Stripe-Signature", stripeSignature(payload, Instant.now()))
                .body(payload)
                .exchange()
                .expectStatus().isOk();
    }

    @Test
    void an_order_with_no_session_id_is_still_recorded_via_its_reference() {
        var reference = "SPR-RECV-0001";
        var order = orderWithNoSession(reference);
        assertThat(order.stripeSessionId()).isNull();

        var sessionId = "cs_test_recovered_0001";
        deliver(completedEvent(sessionId, reference, 99, Instant.now()));

        var recorded = orders.findByReference(reference).orElseThrow();
        assertThat(recorded.isRecorded()).as("matched on client_reference_id").isTrue();
        assertThat(recorded.paidAtStripe()).isNotNull();
    }

    @Test
    void recovering_by_reference_attaches_the_session_id() {
        var reference = "SPR-RECV-0002";
        orderWithNoSession(reference);

        var sessionId = "cs_test_recovered_0002";
        deliver(completedEvent(sessionId, reference, 99, Instant.now()));

        assertThat(orders.findByStripeSessionId(sessionId))
                .as("findable the direct way afterwards")
                .isPresent();
    }

    @Test
    void a_recovered_order_is_recorded_exactly_once() {
        var reference = "SPR-RECV-0003";
        orderWithNoSession(reference);

        var sessionId = "cs_test_recovered_0003";
        var payload = completedEvent(sessionId, reference, 99, Instant.now());

        deliver(payload);
        var afterFirst = orders.findByReference(reference).orElseThrow();

        deliver(payload);
        var afterSecond = orders.findByReference(reference).orElseThrow();

        assertThat(events.countByStripeEventId("evt_test_" + sessionId)).isEqualTo(1);
        assertThat(afterSecond.recordedAt())
                .as("recorded_at must not move")
                .isEqualTo(afterFirst.recordedAt());
    }
}
