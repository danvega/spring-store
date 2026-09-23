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

/**
 * Goal 8. What Stripe says it charged and what the store computed are two independent
 * facts, the same shape as the two timestamps in goal 2. Recording a payment without
 * comparing them is trusting a number nobody checked.
 */
@SpringBootTest(properties = {
        "stripe.secret-key=sk_test_not_used",
        "stripe.webhook-secret=" + IntegrationTest.WEBHOOK_SECRET,
        "stripe.base-url=http://localhost:8080" })
@Import(StubStripeConfiguration.class)
class AmountTest extends IntegrationTest {

    @Autowired
    PurchaseOrderRepository orders;

    @Autowired
    StripeEventRepository events;

    /** Four stickers, $3.96. */
    private String aCartWorthFourDollars() {
        addToCart("ship-it");
        addToCart("stack-overflow-driven");
        addToCart("stack-overflow-driven");
        addToCart("autowired");
        return checkout();
    }

    private void deliver(String payload, int expectedStatus) {
        var spec = client.post().uri("/stripe/webhook")
                .contentType(MediaType.APPLICATION_JSON)
                .header("Stripe-Signature", stripeSignature(payload, Instant.now()))
                .body(payload)
                .exchange();
        if (expectedStatus == 200) {
            spec.expectStatus().isOk();
        }
        else {
            spec.expectStatus().isBadRequest();
        }
    }

    @Test
    void an_event_charging_less_than_the_order_is_refused() {
        var sessionId = aCartWorthFourDollars();

        deliver(completedEvent(sessionId, 1, Instant.now()), 400);

        var order = orders.findByStripeSessionId(sessionId).orElseThrow();
        assertThat(order.isRecorded()).as("nothing recorded").isFalse();
        assertThat(events.countByStripeEventId("evt_test_" + sessionId))
                .as("not claimed, so a corrected redelivery can still work")
                .isZero();
    }

    @Test
    void an_event_charging_more_than_the_order_is_also_refused() {
        var sessionId = aCartWorthFourDollars();

        deliver(completedEvent(sessionId, 100_000, Instant.now()), 400);

        assertThat(orders.findByStripeSessionId(sessionId).orElseThrow().isRecorded()).isFalse();
    }

    @Test
    void the_refusal_names_both_numbers() {
        var sessionId = aCartWorthFourDollars();
        var payload = completedEvent(sessionId, 1, Instant.now());

        client.post().uri("/stripe/webhook")
                .contentType(MediaType.APPLICATION_JSON)
                .header("Stripe-Signature", stripeSignature(payload, Instant.now()))
                .body(payload)
                .exchange()
                .expectStatus().isBadRequest()
                .expectBody(String.class).value(body -> assertThat(body)
                        .contains("expected 396")
                        .contains("Stripe charged 1"));
    }

    @Test
    void a_matching_amount_is_recorded_including_the_multi_line_case() {
        var sessionId = aCartWorthFourDollars();

        deliver(completedEvent(sessionId, 396, Instant.now()), 200);

        var order = orders.findByStripeSessionId(sessionId).orElseThrow();
        assertThat(order.isRecorded()).isTrue();
        assertThat(order.totalCents()).isEqualTo(396);
        assertThat(events.countByStripeEventId("evt_test_" + sessionId)).isEqualTo(1);
    }

    @Test
    void a_refused_mismatch_does_not_block_a_later_correct_delivery() {
        var sessionId = aCartWorthFourDollars();

        deliver(completedEvent(sessionId, 1, Instant.now()), 400);
        assertThat(orders.findByStripeSessionId(sessionId).orElseThrow().isRecorded()).isFalse();

        // Stripe redelivers the same event id with the right amount.
        deliver(completedEvent(sessionId, 396, Instant.now()), 200);

        assertThat(orders.findByStripeSessionId(sessionId).orElseThrow().isRecorded())
                .as("the earlier refusal must not have burned the retry")
                .isTrue();
    }

    @Test
    void an_event_with_no_amount_at_all_is_refused_rather_than_trusted() {
        var sessionId = aCartWorthFourDollars();

        // What deserializeUnsafe leaves behind when Stripe's API version and the SDK's
        // disagree: a readable event whose amount did not map.
        var payload = completedEvent(sessionId, 396, Instant.now())
                .replace("\"amount_total\": 396,", "");

        deliver(payload, 400);

        assertThat(orders.findByStripeSessionId(sessionId).orElseThrow().isRecorded())
                .as("unable to check is not the same as checked and fine")
                .isFalse();
        assertThat(events.countByStripeEventId("evt_test_" + sessionId)).isZero();
    }

    @Test
    void a_single_sticker_order_is_still_checked() {
        var sessionId = buy("ship-it");

        deliver(completedEvent(sessionId, 98, Instant.now()), 400);
        assertThat(orders.findByStripeSessionId(sessionId).orElseThrow().isRecorded())
                .as("one cent out is still out")
                .isFalse();
    }
}
