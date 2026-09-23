# Spring Store

A fake sticker shop for Java developers who want to see how a Spring Boot app handles a
Stripe payment after the customer pays.

Every Stripe tutorial shows the redirect to the payment page. This project is about what
comes next. Does the payment reach the app? Can the app trust what it receives? Does a
replayed event create a second order? Did Stripe charge what the store meant to charge?

It runs in Stripe test mode only. No real money moves and nothing ships.

## What it proves

- The app records a payment from Stripe's webhook, not from the redirect back.
- The page you land on after paying never claims the store has recorded the payment
  before it has.
- A webhook without a valid Stripe signature is rejected.
- A replayed event is handled once, backed by a unique constraint in the database.
- The server computes every total from its own prices. A tampered request can only
  change quantities.
- The webhook checks Stripe's `amount_total` against the order. A mismatch is refused,
  not recorded.
- The app refuses to start without its Stripe keys, and says which one is missing.
- A cart of several stickers becomes one Stripe session and one order.

## What you need

- Java 26
- Docker, for Postgres and for the tests
- A Stripe account in test mode
- The [Stripe CLI](https://docs.stripe.com/stripe-cli), to forward webhooks to your
  machine

Maven comes with the repo as `./mvnw`, so you don't need to install it.

## Run it

1. Clone the repo.

   ```bash
   git clone https://github.com/danvega/spring-store.git
   cd spring-store
   ```

2. Log in to the Stripe CLI and start forwarding webhooks. Leave this running in its own
   terminal.

   ```bash
   stripe login
   stripe listen --forward-to localhost:8080/stripe/webhook
   ```

   It prints a webhook signing secret that starts with `whsec_`. You need it next.

3. Create `secrets.properties` in the project root. Git ignores it.

   ```properties
   stripe.secret-key=sk_test_your_key
   stripe.webhook-secret=whsec_from_stripe_listen
   ```

   Get the secret key from the Stripe dashboard under Developers, API keys. Use the test
   mode key that starts with `sk_test_`. The webhook secret is the one `stripe listen`
   printed. It does not come from the dashboard.

   You can set `STRIPE_SECRET_KEY` and `STRIPE_WEBHOOK_SECRET` as environment variables
   instead.

4. Start the app.

   ```bash
   ./mvnw spring-boot:run
   ```

   Spring Boot starts Postgres for you with Docker Compose, on port 5433. Flyway creates
   the tables and adds six stickers. The container keeps running after the app stops, so
   run `docker compose down` when you're done.

5. Open [http://localhost:8080](http://localhost:8080).

## Try it

- **Buy something.** Add a few stickers, check out, and pay with the test card
  `4242 4242 4242 4242`. Use any future date and any CVC. The confirmation page shows an
  order reference. The same reference goes to Stripe as the checkout session's
  `client_reference_id`. More test cards are in
  [Stripe's testing docs](https://docs.stripe.com/testing).
- **Cancel at Stripe.** You come back to a cart that still has everything in it.
- **Pay with the webhook turned off.** Stop `stripe listen`, then check out and pay. The
  page says it's waiting for Stripe instead of claiming the order is done. After 15
  seconds it says this is slower than usual. After 60 seconds it says Stripe has the
  payment and the store does not. That payment never reaches the app. See
  [Known limits](#known-limits).

## Run the tests

```bash
./verify
```

The tests need Docker, because Testcontainers starts a throwaway Postgres. They don't need
Stripe keys, because Stripe is stubbed.

Each goal has its own target, so you can run one piece at a time:

```bash
./verify signature   # only genuinely signed events are processed
./verify replay      # a redelivered event is not processed twice
./verify amount      # a webhook that disagrees with the order total is refused
./verify help        # list every target
```

## Where to look

| What | Where |
|---|---|
| Checkout writes the order first, then creates the Stripe session | [CheckoutService.java](src/main/java/dev/danvega/store/checkout/CheckoutService.java) |
| Webhook signature check | [StripeWebhookController.java](src/main/java/dev/danvega/store/webhook/StripeWebhookController.java) |
| Replay protection and the amount check | [OrderRecorder.java](src/main/java/dev/danvega/store/webhook/OrderRecorder.java) |
| The unique constraint behind replay protection | [V4__create_stripe_event.sql](src/main/resources/db/migration/V4__create_stripe_event.sql) |
| The confirming page and its 15 and 60 second stages | [ConfirmingStage.java](src/main/java/dev/danvega/store/order/ConfirmingStage.java) |
| Failing loudly without keys | [StripeProperties.java](src/main/java/dev/danvega/store/checkout/StripeProperties.java) |

## How it was built

[PRODUCT.md](PRODUCT.md) is the plan. It has the goals, the non-goals, and every decision
with its reason.

[.shipit/](.shipit/) is the history:

- `specs/done/` has a spec for each feature, with the command that proves each part.
- `prototype/` has the design rounds the screens came from.
- `verify/evidence/` has a run against live Stripe test mode.
- `open.md` lists known limits and risks.
- `retro/` has notes on the build process itself.

## Known limits

- **No reconciliation against Stripe.** If the app never hears about a payment, nothing
  goes looking for it.
- **Pending orders pile up.** An abandoned checkout leaves a pending order, and nothing
  cleans it up.
- **Local only.** There is no deployment setup.

More detail is in [.shipit/open.md](.shipit/open.md).

## Stack

Java 26, Spring Boot 4.0.8, Spring Data JDBC, Flyway, Postgres 18, JTE templates, Tailwind
(loaded from a CDN, with no build step), and stripe-java 29.2.0.
