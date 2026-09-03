package dev.danvega.store.checkout;

import com.stripe.StripeClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
class StripeConfiguration {

    @Bean
    StripeClient stripeClient(StripeProperties properties) {
        return new StripeClient(properties.secretKey());
    }
}
