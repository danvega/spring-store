package dev.danvega.store;

import java.util.concurrent.atomic.AtomicInteger;

import dev.danvega.store.catalog.Sticker;
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
     * Session ids are unique per call, not per sticker. Every test class shares one
     * Postgres, and stripe_session_id is unique, so a slug-derived id makes buying the
     * same sticker twice anywhere in the suite fail on a constraint rather than on the
     * thing under test.
     */
    public static class StubStripeGateway implements StripeGateway {

        private final AtomicInteger counter = new AtomicInteger();

        private volatile String lastSessionId;

        @Override
        public CheckoutSession start(Sticker sticker, String successUrl, String cancelUrl) {
            var id = "cs_test_%s_%d".formatted(sticker.slug(), counter.incrementAndGet());
            this.lastSessionId = id;
            return new CheckoutSession(id, "https://checkout.stripe.test/pay/" + id);
        }

        /** The session id from the most recent purchase, for asserting against. */
        public String lastSessionId() {
            return lastSessionId;
        }
    }
}
