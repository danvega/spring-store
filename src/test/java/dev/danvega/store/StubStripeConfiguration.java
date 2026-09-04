package dev.danvega.store;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import dev.danvega.store.checkout.CheckoutFailedException;
import dev.danvega.store.checkout.StripeGateway;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;

/**
 * Stands in for Stripe's network so the checkout flow can be proved without a live
 * account. Top level on purpose: nested inside the test base it also gets picked up as a
 * default configuration class, which Spring Framework 7.1 stops ignoring.
 */
@TestConfiguration
public class StubStripeConfiguration {

    @Bean
    @Primary
    public StubStripeGateway stubStripeGateway() {
        return new StubStripeGateway();
    }

    /**
     * Session ids are unique per call, not per cart. Every test class shares one Postgres,
     * and stripe_session_id is unique, so a repeatable id makes checking out twice
     * anywhere in the suite fail on a constraint rather than on the thing under test.
     */
    public static class StubStripeGateway implements StripeGateway {

        private final AtomicInteger counter = new AtomicInteger();

        private volatile String lastSessionId;

        private volatile String lastOrderReference;

        private volatile List<Line> lastLines = List.of();

        private volatile boolean failNext;

        /** Make the next call blow up, so a proof can fail the remote side on demand. */
        public void failNext() {
            this.failNext = true;
        }

        @Override
        public CheckoutSession start(List<Line> lines, String orderReference, String successUrl, String cancelUrl) {
            if (failNext) {
                failNext = false;
                throw new CheckoutFailedException("stubbed Stripe failure", new IllegalStateException("stub"));
            }
            var id = "cs_test_" + counter.incrementAndGet();
            this.lastSessionId = id;
            this.lastOrderReference = orderReference;
            this.lastLines = List.copyOf(lines);
            return new CheckoutSession(id, "https://checkout.stripe.test/pay/" + id);
        }

        public String lastSessionId() {
            return lastSessionId;
        }

        public String lastOrderReference() {
            return lastOrderReference;
        }

        /** What Stripe was actually asked to charge for. */
        public List<Line> lastLines() {
            return lastLines;
        }
    }
}
