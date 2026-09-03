package dev.danvega.store;

import org.junit.jupiter.api.Test;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;

@SpringBootTest(properties = {
        "stripe.secret-key=sk_test_not_used",
        "stripe.webhook-secret=" + IntegrationTest.WEBHOOK_SECRET,
        "stripe.base-url=http://localhost:8080" })
@Import(StubStripeConfiguration.class)
class SpringStoreApplicationTests extends IntegrationTest {

    @Test
    void contextLoads() {
    }
}
