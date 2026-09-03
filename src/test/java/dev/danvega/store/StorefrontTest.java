package dev.danvega.store;

import org.junit.jupiter.api.Test;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(properties = {
        "stripe.secret-key=sk_test_not_used",
        "stripe.webhook-secret=" + IntegrationTest.WEBHOOK_SECRET,
        "stripe.base-url=http://localhost:8080" })
@Import(StubStripeConfiguration.class)
class StorefrontTest extends IntegrationTest {

    @Test
    void lists_every_seeded_sticker_with_a_price() {
        client.get().uri("/").exchange()
                .expectStatus().isOk()
                .expectBody(String.class).value(page -> assertThat(page)
                        .contains("Spring Boot Leaf")
                        .contains("It Works On My Machine")
                        .contains("NullPointerException Survivor")
                        .contains("@Autowired")
                        .contains("Stack Overflow Driven Development")
                        .contains("Ship It")
                        .contains("$0.99"));
    }

    @Test
    void offers_a_buy_form_per_sticker() {
        client.get().uri("/").exchange()
                .expectStatus().isOk()
                .expectBody(String.class)
                .value(page -> assertThat(page).contains("action=\"/buy/ship-it\""));
    }
}
