package dev.danvega.store.checkout;

import jakarta.validation.constraints.NotBlank;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/**
 * Validated so a clone with no keys refuses to start, and says something useful when it
 * does. The properties carry an empty default rather than none, so binding runs and these
 * messages appear instead of a placeholder resolution error that says what is missing but
 * not what to do about it.
 */
@ConfigurationProperties(prefix = "stripe")
@Validated
public record StripeProperties(

        @NotBlank(message = "stripe.secret-key is not set. Put it in secrets.properties, "
                + "which is git ignored, or set STRIPE_SECRET_KEY. Get a test key from the "
                + "Stripe dashboard under Developers, API keys. It starts with sk_test_.")
        String secretKey,

        @NotBlank(message = "stripe.webhook-secret is not set. Run "
                + "'stripe listen --forward-to localhost:8080/stripe/webhook' and use the "
                + "whsec_ value it prints. It does not come from the Stripe dashboard.")
        String webhookSecret,

        String baseUrl) {
}
