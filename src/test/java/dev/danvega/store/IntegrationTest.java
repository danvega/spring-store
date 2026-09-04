package dev.danvega.store;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.UUID;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import org.testcontainers.containers.PostgreSQLContainer;

import org.junit.jupiter.api.BeforeEach;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.test.web.servlet.client.RestTestClient;
import org.springframework.web.context.WebApplicationContext;

/**
 * Shared setup. A throwaway Postgres per run, and a stubbed Stripe so the checkout
 * flow can be proved without a live account.
 */
public abstract class IntegrationTest {

    public static final String WEBHOOK_SECRET = "whsec_test_secret_for_signing";

    /**
     * Started once and never stopped. The @Testcontainers/@Container lifecycle stops the
     * container when a test class finishes, which leaves every later class connecting to
     * a dead port.
     */
    @ServiceConnection
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:18");

    static {
        POSTGRES.start();
    }

    @Autowired
    private WebApplicationContext context;

    /**
     * One fluent client for every test here. Bound to the application context, so the
     * whole stack runs but no socket is opened and redirects are not followed.
     */
    protected RestTestClient client;

    @Autowired
    protected StubStripeConfiguration.StubStripeGateway stripe;

    /** A fresh anonymous visitor per test. There are no accounts, so this is the identity. */
    protected String cartCookie;

    @BeforeEach
    void bindClient() {
        this.client = RestTestClient.bindToApplicationContext(this.context).build();
        this.cartCookie = UUID.randomUUID().toString();
    }

    /** Adds through the real endpoint. A cart row built directly would prove nothing. */
    protected void addToCart(String slug) {
        client.post().uri("/cart/add/{slug}", slug)
                .cookie("cart_id", cartCookie)
                .exchange()
                .expectStatus().is3xxRedirection();
    }

    protected void setQuantity(Long stickerId, int quantity) {
        client.post().uri("/cart/quantity/{id}?quantity={q}", stickerId, quantity)
                .cookie("cart_id", cartCookie)
                .exchange()
                .expectStatus().is3xxRedirection();
    }

    /** Checks out the cart and returns the Stripe session id the stub handed back. */
    protected String checkout() {
        client.post().uri("/cart/checkout")
                .cookie("cart_id", cartCookie)
                .exchange()
                .expectStatus().is3xxRedirection();
        return stripe.lastSessionId();
    }

    /** The common case: one sticker, straight through to a Stripe session. */
    protected String buy(String slug) {
        addToCart(slug);
        return checkout();
    }

    /** A checkout.session.completed event carrying the order reference Stripe echoes back. */
    public static String completedEvent(String sessionId, String clientReferenceId, Instant created) {
        return completedEvent(sessionId, created)
                .replace("\"payment_status\": \"paid\"",
                        "\"client_reference_id\": \"%s\",\n      \"payment_status\": \"paid\"".formatted(clientReferenceId));
    }

    /** A checkout.session.completed event, shaped the way Stripe sends it. */
    public static String completedEvent(String sessionId, Instant created) {
        return """
                {
                  "id": "evt_test_%s",
                  "object": "event",
                  "api_version": "2024-06-20",
                  "created": %d,
                  "type": "checkout.session.completed",
                  "data": {
                    "object": {
                      "id": "%s",
                      "object": "checkout.session",
                      "amount_total": 99,
                      "currency": "usd",
                      "payment_status": "paid",
                      "status": "complete"
                    }
                  }
                }""".formatted(sessionId, created.getEpochSecond(), sessionId);
    }

    /** Stripe's signature scheme: t=<unix>,v1=<hmac sha256 of "t.payload">. */
    public static String stripeSignature(String payload, Instant at) {
        long timestamp = at.getEpochSecond();
        String signed = timestamp + "." + payload;
        return "t=" + timestamp + ",v1=" + hmacSha256Hex(signed);
    }

    private static String hmacSha256Hex(String value) {
        try {
            var mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(WEBHOOK_SECRET.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            var raw = mac.doFinal(value.getBytes(StandardCharsets.UTF_8));
            var hex = new StringBuilder(raw.length * 2);
            for (byte b : raw) {
                hex.append("%02x".formatted(b));
            }
            return hex.toString();
        }
        catch (Exception e) {
            throw new IllegalStateException("Could not sign the test payload", e);
        }
    }
}
