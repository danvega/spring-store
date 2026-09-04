package dev.danvega.store;

import java.time.Instant;

import dev.danvega.store.order.PurchaseOrderRepository;
import dev.danvega.store.webhook.StripeEventRepository;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;

import static org.assertj.core.api.Assertions.assertThat;

/** Goal 3: the app trusts only genuinely signed Stripe events. */
@SpringBootTest(properties = {
        "stripe.secret-key=sk_test_not_used",
        "stripe.webhook-secret=" + IntegrationTest.WEBHOOK_SECRET,
        "stripe.base-url=http://localhost:8080" })
@Import(StubStripeConfiguration.class)
class SignatureTest extends IntegrationTest {

    @Autowired
    PurchaseOrderRepository orders;

    @Autowired
    StripeEventRepository events;



    @Test
    void a_wrong_signature_is_rejected_and_records_nothing() {
        var sessionId = buy("ship-it");
        var payload = completedEvent(sessionId, Instant.now());

        client.post().uri("/stripe/webhook")
                .contentType(MediaType.APPLICATION_JSON)
                .header("Stripe-Signature", "t=1234567890,v1=deadbeefdeadbeefdeadbeefdeadbeef")
                .body(payload)
                .exchange()
                .expectStatus().isBadRequest();

        assertThat(orders.findByStripeSessionId(sessionId).orElseThrow().isRecorded()).isFalse();
        assertThat(events.countByStripeEventId("evt_test_" + sessionId)).isZero();
    }

    @Test
    void a_missing_signature_header_is_rejected() {
        var sessionId = buy("autowired");
        var payload = completedEvent(sessionId, Instant.now());

        client.post().uri("/stripe/webhook")
                .contentType(MediaType.APPLICATION_JSON)
                .body(payload)
                .exchange()
                .expectStatus().isBadRequest();

        assertThat(orders.findByStripeSessionId(sessionId).orElseThrow().isRecorded()).isFalse();
    }

    @Test
    void a_payload_tampered_with_after_signing_is_rejected() {
        var sessionId = buy("ship-it");
        var at = Instant.now();
        var original = completedEvent(sessionId, at);
        var signature = stripeSignature(original, at);

        // Same signature, body edited to change the amount.
        var tampered = original.replace("\"amount_total\": 99", "\"amount_total\": 1");

        client.post().uri("/stripe/webhook")
                .contentType(MediaType.APPLICATION_JSON)
                .header("Stripe-Signature", signature)
                .body(tampered)
                .exchange()
                .expectStatus().isBadRequest();

        assertThat(orders.findByStripeSessionId(sessionId).orElseThrow().isRecorded()).isFalse();
        assertThat(events.countByStripeEventId("evt_test_" + sessionId)).isZero();
    }
}
