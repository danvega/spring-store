package dev.danvega.store;

import java.time.Instant;

import dev.danvega.store.order.ConfirmingStage;
import dev.danvega.store.order.PurchaseOrderRepository;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Every test here runs the order Stripe actually produces: buy, land on the confirmation
 * URL, and only then let the webhook arrive. Goal 1's tests posted the webhook first and
 * shipped a guaranteed NullPointerException green because of it.
 */
@SpringBootTest(properties = {
        "stripe.secret-key=sk_test_not_used",
        "stripe.webhook-secret=" + IntegrationTest.WEBHOOK_SECRET,
        "stripe.base-url=http://localhost:8080" })
@Import(StubStripeConfiguration.class)
class ConfirmingTest extends IntegrationTest {

    @Autowired
    PurchaseOrderRepository orders;



    private void webhookArrives(String sessionId) {
        var at = Instant.now();
        var payload = completedEvent(sessionId, at);
        client.post().uri("/stripe/webhook")
                .contentType(MediaType.APPLICATION_JSON)
                .header("Stripe-Signature", stripeSignature(payload, at))
                .body(payload)
                .exchange()
                .expectStatus().isOk();
    }

    @Test
    void straight_back_from_stripe_it_does_not_claim_the_store_recorded_anything() {
        var sessionId = buy("ship-it");

        client.get().uri("/order/confirm?session_id={id}", sessionId).exchange()
                .expectStatus().isOk()
                .expectBody(String.class).value(page -> assertThat(page)
                        .contains("Paid at Stripe")
                        .contains("Confirming your payment")
                        .doesNotContain("Payment received")
                        .doesNotContain("Order reference"));
    }

    @Test
    void it_flips_to_paid_once_the_webhook_lands() {
        var sessionId = buy("it-works-on-my-machine");

        client.get().uri("/order/confirm?session_id={id}", sessionId).exchange()
                .expectStatus().isOk()
                .expectBody(String.class).value(page -> assertThat(page).doesNotContain("Payment received"));

        webhookArrives(sessionId);
        var reference = orders.findByStripeSessionId(sessionId).orElseThrow().reference();

        client.get().uri("/order/confirm?session_id={id}", sessionId).exchange()
                .expectStatus().isOk()
                .expectBody(String.class).value(page -> assertThat(page)
                        .contains("Payment received")
                        .contains(reference)
                        .doesNotContain("Confirming your payment"));
    }

    @Test
    void after_fifteen_seconds_it_acknowledges_the_delay() {
        var sessionId = buy("spring-boot-leaf");
        var arrived = Instant.now().minus(ConfirmingStage.SLOW_AFTER).getEpochSecond();

        client.get().uri("/order/confirm?session_id={id}&since={since}", sessionId, arrived).exchange()
                .expectStatus().isOk()
                .expectBody(String.class).value(page -> assertThat(page)
                        .contains("Taking longer")
                        .contains("Still confirming")
                        .contains("Your payment is safe at Stripe")
                        .contains("Check again")
                        .doesNotContain("Payment received"));
    }

    @Test
    void after_sixty_seconds_it_stops_claiming_it_will_resolve() {
        var sessionId = buy("autowired");
        var arrived = Instant.now().minus(ConfirmingStage.GIVE_UP_AFTER).getEpochSecond();

        client.get().uri("/order/confirm?session_id={id}&since={since}", sessionId, arrived).exchange()
                .expectStatus().isOk()
                .expectBody(String.class).value(page -> assertThat(page)
                        .contains("Not recorded")
                        .contains("Stripe has the payment. The store does not.")
                        .doesNotContain("Payment received")
                        // a page that has given up must stop auto refreshing
                        .doesNotContain("http-equiv=\"refresh\""));
    }

    @Test
    void a_waiting_page_refreshes_itself_and_keeps_the_arrival_time() {
        var sessionId = buy("stack-overflow-driven");
        var arrived = Instant.now().minusSeconds(3).getEpochSecond();

        client.get().uri("/order/confirm?session_id={id}&since={since}", sessionId, arrived).exchange()
                .expectStatus().isOk()
                .expectBody(String.class).value(page -> assertThat(page)
                        .contains("http-equiv=\"refresh\"")
                        .contains("since=" + arrived));
    }

    @Test
    void an_unusable_since_is_treated_as_arriving_now() {
        var sessionId = buy("ship-it");

        // non-numeric, past the maximum Instant, past the minimum, blank, and in the future
        for (var bad : java.util.List.of("abc", "99999999999999999", "-99999999999999999", "",
                String.valueOf(Instant.now().plusSeconds(3600).getEpochSecond()))) {
            client.get().uri("/order/confirm?session_id={id}&since={since}", sessionId, bad).exchange()
                    .expectStatus().isOk()
                    .expectBody(String.class).value(page -> assertThat(page)
                            .contains("Confirming your payment")
                            .doesNotContain("Payment received"));
        }
    }

    @Test
    void a_recorded_order_never_shows_a_confirming_state_however_old_it_is() {
        var sessionId = buy("nullpointerexception-survivor");
        webhookArrives(sessionId);
        var ancient = Instant.now().minusSeconds(3600).getEpochSecond();

        client.get().uri("/order/confirm?session_id={id}&since={since}", sessionId, ancient).exchange()
                .expectStatus().isOk()
                .expectBody(String.class).value(page -> assertThat(page)
                        .contains("Payment received")
                        .doesNotContain("Stripe has the payment. The store does not."));
    }
}
