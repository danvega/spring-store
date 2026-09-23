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
class ConfirmationTest extends IntegrationTest {

    @Autowired
    PurchaseOrderRepository orders;


    @Test
    void shows_the_paid_order_with_its_reference_and_amount() {
        var sessionId = buy("spring-boot-leaf");

        var paidAt = Instant.now();
        var payload = paidEvent(sessionId, paidAt);
        client.post().uri("/stripe/webhook")
                .contentType(MediaType.APPLICATION_JSON)
                .header("Stripe-Signature", stripeSignature(payload, paidAt))
                .body(payload)
                .exchange()
                .expectStatus().isOk();

        var reference = orders.findByStripeSessionId(sessionId).orElseThrow().reference();

        client.get().uri("/order/confirm?session_id={id}", sessionId).exchange()
                .expectStatus().isOk()
                .expectBody(String.class).value(page -> assertThat(page)
                        .contains("Payment received")
                        .contains(reference)
                        .contains("$0.99")
                        .contains("Spring Boot Leaf"));
    }

    @Test
    void an_unknown_session_is_a_404() {
        client.get().uri("/order/confirm?session_id={id}", "cs_test_nope").exchange()
                .expectStatus().isNotFound();
    }
}
