package dev.danvega.store;

import java.time.Instant;

import dev.danvega.store.order.PurchaseOrderRepository;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(properties = {
        "stripe.secret-key=sk_test_not_used",
        "stripe.webhook-secret=" + IntegrationTest.WEBHOOK_SECRET,
        "stripe.base-url=http://localhost:8080" })
@Import(StubStripeConfiguration.class)
class WebhookTest extends IntegrationTest {

    @Autowired
    PurchaseOrderRepository orders;


    @Test
    void a_signed_completed_event_records_the_payment() {
        var sessionId = buy("autowired");
        assertThat(orders.findByStripeSessionId(sessionId).orElseThrow().isRecorded()).isFalse();

        var paidAt = Instant.now();
        var payload = completedEvent(sessionId, paidAt);

        client.post().uri("/stripe/webhook")
                .contentType(MediaType.APPLICATION_JSON)
                .header("Stripe-Signature", stripeSignature(payload, paidAt))
                .body(payload)
                .exchange()
                .expectStatus().isOk();

        var order = orders.findByStripeSessionId(sessionId).orElseThrow();

        assertThat(order.isRecorded()).as("the app now has its own record").isTrue();
        assertThat(order.paidAtStripe()).as("what Stripe said").isNotNull();
        assertThat(order.recordedAt()).as("what this app knows").isNotNull();
    }

    @Test
    void an_event_for_an_unknown_session_is_acknowledged_not_recorded() {
        var now = Instant.now();
        var payload = completedEvent("cs_test_never_created_here", now);

        client.post().uri("/stripe/webhook")
                .contentType(MediaType.APPLICATION_JSON)
                .header("Stripe-Signature", stripeSignature(payload, now))
                .body(payload)
                .exchange()
                .expectStatus().isOk();

        assertThat(orders.findByStripeSessionId("cs_test_never_created_here")).isEmpty();
    }
}
