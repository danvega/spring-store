package dev.danvega.store;

import dev.danvega.store.checkout.StripeProperties;
import org.junit.jupiter.api.Test;

import org.springframework.boot.WebApplicationType;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.boot.validation.autoconfigure.ValidationAutoConfiguration;
import org.springframework.context.annotation.Configuration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Goal 5's second half. This deliberately does not extend {@link IntegrationTest}, because
 * the point is a context that never starts and it must not inherit one that does. The last
 * test borrows that class's Postgres container only as a database to point at.
 */
class StartupTest {

    @Configuration(proxyBeanMethods = false)
    @EnableConfigurationProperties(StripeProperties.class)
    static class OnlyStripeProperties {
    }

    private ApplicationContextRunner runner() {
        return new ApplicationContextRunner()
                .withConfiguration(AutoConfigurations.of(ValidationAutoConfiguration.class))
                .withUserConfiguration(OnlyStripeProperties.class);
    }

    @Test
    void a_missing_secret_key_stops_startup_and_says_where_to_get_one() {
        runner().withPropertyValues("stripe.webhook-secret=whsec_present")
                .run(context -> assertThat(context)
                        .hasFailed()
                        .getFailure()
                        .hasStackTraceContaining("stripe.secret-key is not set")
                        .hasStackTraceContaining("Developers, API keys"));
    }

    @Test
    void a_missing_webhook_secret_stops_startup_and_points_at_the_cli() {
        runner().withPropertyValues("stripe.secret-key=sk_test_present")
                .run(context -> assertThat(context)
                        .hasFailed()
                        .getFailure()
                        .hasStackTraceContaining("stripe.webhook-secret is not set")
                        .hasStackTraceContaining("stripe listen"));
    }

    @Test
    void a_blank_key_is_treated_the_same_as_a_missing_one() {
        runner().withPropertyValues("stripe.secret-key=", "stripe.webhook-secret=  ")
                .run(context -> assertThat(context).hasFailed());
    }

    @Test
    void both_keys_present_starts_cleanly() {
        runner().withPropertyValues("stripe.secret-key=sk_test_x", "stripe.webhook-secret=whsec_x")
                .run(context -> assertThat(context).hasNotFailed()
                        .getBean(StripeProperties.class)
                        .extracting(StripeProperties::secretKey).isEqualTo("sk_test_x"));
    }

    /**
     * The runner above proves the validation. This proves the real application refuses to
     * boot, which is what the goal actually says.
     *
     * The database is real and working on purpose. A fresh clone has the compose file, so
     * Postgres is not what it is missing. Pointing this at nothing made the datasource
     * fail first and proved only that a broken database breaks the app.
     */
    @Test
    void the_whole_application_refuses_to_boot_without_keys() {
        assertThatThrownBy(() -> new SpringApplicationBuilder(SpringStoreApplication.class)
                .web(WebApplicationType.NONE)
                .properties("spring.docker.compose.enabled=false",
                        "spring.datasource.url=" + IntegrationTest.POSTGRES.getJdbcUrl(),
                        "spring.datasource.username=" + IntegrationTest.POSTGRES.getUsername(),
                        "spring.datasource.password=" + IntegrationTest.POSTGRES.getPassword())
                // Command line arguments, not .properties(). That method sets default
                // properties, the lowest precedence there is, so a developer's real
                // secrets.properties silently won and the app started fine.
                .run("--stripe.secret-key=", "--stripe.webhook-secret="))
                .hasStackTraceContaining("stripe.secret-key is not set");
    }
}
