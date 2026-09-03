package dev.danvega.store.checkout;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * No defaults for the keys on purpose. A clone with no keys should fail at startup,
 * loudly, rather than at the first click.
 */
@ConfigurationProperties(prefix = "stripe")
public record StripeProperties(String secretKey, String webhookSecret, String baseUrl) {
}
