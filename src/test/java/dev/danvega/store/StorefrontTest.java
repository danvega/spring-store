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
                .value(page -> assertThat(page).contains("action=\"/cart/add/ship-it\""));
    }

    @Test
    void shows_the_leaf_logo_and_every_page_links_a_favicon_that_is_served() {
        client.get().uri("/").exchange()
                .expectStatus().isOk()
                .expectBody(String.class).value(page -> assertThat(page)
                        .contains("src=\"/logo.svg\"")
                        .contains("href=\"/favicon.svg\"")
                        .contains("href=\"/favicon-32.png\""));

        // The favicon lives in the shared layout, so a page other than the storefront has it too.
        client.get().uri("/cart").exchange()
                .expectStatus().isOk()
                .expectBody(String.class).value(page -> assertThat(page).contains("href=\"/favicon.svg\""));

        // A browser shows nothing when an image link points at a missing file, so check each one.
        for (var path : new String[] { "/logo.svg", "/favicon.svg", "/favicon-32.png" }) {
            client.get().uri(path).exchange().expectStatus().isOk();
        }
    }
}
