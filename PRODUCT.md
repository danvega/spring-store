# Spring Store

A fictitious 99-cent sticker shop on Spring Boot, wired to Stripe. A proof of concept
for Dan, not a product.

## What it proves

Checkout is not in doubt. Every tutorial shows the redirect. This exists to test what
happens after the customer pays: whether the confirmation actually reaches the app,
whether it can be trusted, whether a replay creates a second order, and whether the amount
charged is the amount the store meant to charge.

## Goals

1. **A visitor buys a sticker and the app knows about it.**
   Done when: a Stripe test card completes checkout and the order reads as paid in the
   app, not just in the Stripe dashboard.
2. **The confirmation page never claims paid before the app has recorded it.**
   Done when: with the webhook artificially delayed, the page shows a confirming state
   and flips to paid only once the app's own record says so.
3. **The app trusts only genuine Stripe events.**
   Done when: a webhook with a bad signature is rejected, not processed.
4. **A duplicate event does not double-count.**
   Done when: replaying the same webhook twice leaves exactly one paid order.
5. **Secrets never live in the repo.**
   Done when: a fresh clone with no keys fails loudly at startup with a clear message.
6. **A visitor buys several stickers in one go.**
   Done when: a cart holding three different stickers, one of them with a quantity above
   one, produces a single Stripe session and one order whose lines match the cart.
7. **The amount is computed by the server, never taken from the browser.**
   Done when: a request that tampers with a quantity or a price is ignored, and the Stripe
   session total matches the server's own arithmetic.
8. **A recorded payment is checked against what Stripe actually charged.**
   Done when: an event whose `amount_total` disagrees with the order total is refused
   rather than recorded.

## Non-goals

- **Real money** (not ever): test mode only. There is no business here and nothing to
  fulfill.
- **User accounts** (not ever): the buyer is anonymous. Auth proves nothing about
  payments.
- **Shipping, tax, inventory** (not ever): real commerce concerns that would triple the
  surface and teach nothing about the integration.
- **Storefront browsing** (not ever): no search, no filters, no categories, no product
  pages. Six stickers on one page. Replaces an earlier "no polished storefront" non-goal,
  which was wrong: the design pass showed polish is cheap and features are what bloat.
  A cart and quantities were also on this list and were promoted on 3 September 2026,
  because pricing a multi-item order is where the real payment lesson lives. With one
  fixed 99-cent item the store never has to ask whether Stripe charged what it expected.
  With a cart it does, and that is goals 7 and 8.
- **Deployment** (not yet): runs locally with the Stripe CLI forwarding webhooks.
  Revisit only if this becomes a talk or a video.

## Decisions

- **Spring Boot and Stripe**: Dan's call, and the entire point of the exercise.
- **Stripe Checkout, hosted**: Stripe hosts the payment page, so there is no card form
  and no Stripe.js in this app. Gives up a branded checkout to reach the webhook
  sooner, which is the part under test. Revisit if this ever needs to look like a real
  store.
- **Postgres via Docker Compose**: a real unique constraint on the Stripe event id is
  what makes the replay test in goal 4 honest. Costs a running Docker daemon. Revisit
  if that daemon becomes more friction than the test is worth.
- **JTE, server-rendered**: Dan's preferred template engine. Templates are typed and
  compile at build time, so a broken page fails the build instead of the browser. A
  product list and a buy button need no client-side JavaScript.
- **Tailwind for styling**: Dan's default. Separate decision from JTE, which only
  answers how pages are rendered.
- **Tailwind via the play CDN**: one script tag, no build step and no second watch
  process running beside the app. Tailwind says never ship the CDN, which does not bite
  here because deployment is a non-goal. Revisit the moment this gets deployed or filmed.
- **Java 26, Maven, Spring Boot 4.0.8**: Dan wanted the latest of each, and 4.1.1 was
  the intent. Initializr declares the jte dependency compatible with [4.0.0, 4.1.0) and
  refuses to generate it against 4.1.1, so one Boot minor was traded to keep JTE. Note
  that gg.jte:jte-spring-boot-starter-4 does exist (3.2.4, what this project uses), so
  4.1.1 is undeclared rather than known-broken. Revisit by bumping the parent and running
  the suite.
- **Spring Data JDBC with Flyway**: repositories without JPA's lazy loading and dirty
  checking, and aggregates that load whole. Flyway keeps the schema explicit, which
  matters because the unique constraint on the Stripe event id is the thing goal 4
  actually tests. Stating it in a migration beats inferring it from an annotation.
- **Screens**: storefront, confirming (waiting / taking longer / not recorded),
  confirmation, cancelled, empty.
- **An order is two facts, not one status**: "paid at Stripe" and "recorded by the app"
  are independent, each with its own timestamp. The design shows them as paired badges,
  green for Stripe and yellow for the app. Conflating them is what made the original
  goal 1 weak enough to pass by accident.
- **The order row is written before the Stripe session is created**, and flipped to paid
  by the webhook. Gives the confirming page something to poll and makes goal 4 natural,
  since the webhook updates a row that is already there. The ordering also decides which
  orphan a failure leaves: Stripe first would leave a payable session this app never heard
  of, while order first leaves a pending order that can never be paid. `CheckoutService`
  is deliberately not transactional, because holding a database connection across a
  network call to Stripe buys nothing.
- **The order reference travels to Stripe as `client_reference_id`**, and comes back on
  the webhook. It is the fallback match for an order whose session id was never attached,
  which is what checkout leaves behind if it dies between creating the session and writing
  it back. Matching that way attaches the session id, so everything afterwards finds the
  order directly. Two systems cannot share a transaction, so this does not remove the gap,
  it makes the gap recoverable instead of silent.
- **The success URL carries the Stripe checkout session id**: the confirming page needs
  an identity to poll and the order reference does not exist yet at that point.
- **Confirming thresholds are 15s and 60s**: a plain wait below 15s, an acknowledged
  delay at 15s, and at 60s it stops claiming it will resolve. These come from the design,
  so they are product behavior rather than an implementation detail.
- **Catalog seeded into Postgres**: stickers are rows, not constants, which makes the
  empty state from the design reachable and worth building. Costs a table and a seed
  script that prove nothing about payments, accepted to keep the storefront honest.
- **Visual direction**: neo-brutalist. Cream ground, thick black borders, hard offset
  shadows, chunky rounded corners, one flat color per sticker. Locked by design round 1,
  so later rounds match it instead of reopening it.
- **Machine values in monospace**: amounts, order references, timestamps, the test card
  number. Prose stays sans. Came out of the design and earns its place, because it makes
  reconcilable values visually distinct from copy.
- **Orders carry a human-readable reference** (shape: SPR-4Q8T-2M19), shown on the
  confirmation so it can be matched against the Stripe dashboard. Distinct from the
  Stripe session id.
- **Testcontainers for tests**: each run gets a clean isolated Postgres, so the replay
  test in goal 4 cannot be polluted by rows left over from clicking around by hand.
  Docker is already required. Costs a few seconds of container startup per run.
- **The confirming clock runs from arrival, not from creation**: the 15s and 60s
  thresholds measure how long the buyer has been on the confirming page. `created_at`
  would also count however long they spent on Stripe's card form, so an unhurried buyer
  would land already past 60 seconds and be told the store has no record before it had
  waited at all. The confirming URL carries a `since` timestamp instead, set on first
  render and preserved across refreshes.
- **Re-checking is a meta refresh, not JavaScript**: two seconds while waiting, and it
  stops once the page has given up, because a page that has stopped claiming it will
  resolve should stop behaving like it will. Keeps this project free of client-side
  JavaScript. Costs a full page load per check, which is free at this scale.
- **A redelivery is stopped by a unique constraint, not by a catch**: the recorder checks
  whether the event id is already claimed, then inserts it. An ordinary redelivery hits
  the check. Two identical deliveries arriving at once hit the constraint instead, that
  transaction rolls back, and Stripe's own retry runs into the check. No exception
  control flow, and the constraint is load-bearing rather than decorative.
- **Claiming the event and recording the payment share one transaction**: if the
  recording fails, the claim rolls back with it, so Stripe's retry can still do the work.
  A rejected event leaves no claim behind, and neither does an event that matched no
  order. Claiming something the app could not act on would burn the only retry that could
  ever record that payment, which is the exact failure this project exists to catch.
- **Missing keys fail validation, not placeholder resolution**: the Stripe properties
  carry an empty default so binding actually runs, and `@Validated` with `@NotBlank`
  produces a message naming the property and where to get its value. Without the default
  the app still refused to start, but only said a placeholder could not be resolved. Both
  are loud. One of them is useful.
- **The cart is a table keyed by a cookie**: there are no accounts, so a `cart_id` cookie
  identifies an anonymous visitor and `cart` plus `cart_line` rows hold what they picked.
  Needs no new dependency, unlike Spring Session. Survives restarts, which an in-memory
  HTTP session does not, and it is inspectable in psql like every other fact this project
  cares about.
- **No quantity cap**: a visitor can add as many of one sticker as they like. Capping
  would be arbitrary and it is not what protects the total, since goal 7 is about pricing
  server-side rather than limiting what the browser asks for. Stripe imposes its own limit
  of 999,999 per line, so that boundary gets handled rather than ignored.
- **The empty cart is its own screen**: someone can open the cart with nothing in it.
  "Your cart is empty" and "no stickers are listed" are different statements and reusing
  one screen for both would say the wrong thing.
- **The cart empties when the payment is recorded, not when checkout starts**, so
  cancelling at Stripe does not cost you your cart. A refresh of checkout only creates a
  second unpaid order, which is harmless. `purchase_order` carries the `cart_id` so the
  webhook knows which cart to empty. This replaces an earlier decision that the cancel URL
  carried a sticker id: its reason was returning you to the same purchase, and the cart
  now does that better.
- **Line prices are copied onto the order at checkout**, never joined from the sticker at
  read time. A later price change must not rewrite what somebody already paid.
- **Recording a payment subtracts the purchased quantities from the cart**, it does not
  empty it. The cart stays live while the Stripe page is open, so a buyer can add to it,
  and clearing it wholesale would delete something they never bought.

## Stops

Halt the branch and wait for Dan when:

- **A non-goal is in play.** Promoting one is a conversation, never a line edit.
- **A decision is missing.** A dependency, service or library this file does not record.
  Ask, do not pick.
- **A criterion has no command.** Never invent a proof to clear the gate. Park it for
  the batch.
- **The step needs real Stripe keys or the Stripe CLI.** Those live outside the repo and
  cannot run unattended. Park it and write down what evidence Dan should capture.

Anything else: record the decision with its reason, keep building, carry it into the
end-of-run batch.

## Current state

**Works today:** goals 1 through 6. Stickers are added to a cart kept in the database and
found by a cookie, quantities can be changed, and checking out turns the whole cart into
one Stripe session and one order with lines. Landing back before the webhook shows a
confirming state that never claims the store has recorded anything. Forged, unsigned and
tampered webhooks are rejected. A redelivered event is claimed once. A clone with no keys
refuses to start. 46 tests behind `./verify`, all green.
**In progress:** nothing. Goal 1's browser round trip was proven against live Stripe
test mode on 3 September 2026, evidence in `.ship/verify/evidence/001-goal-1-live-stripe.md`.
The measured gap between Stripe's event time and the app recording it was about 1 second.
**Next:** goals 7 and 8, spec 007. Pricing is already server-side, so this is the proofs:
a tampered quantity or price is ignored, and a webhook whose `amount_total` disagrees with
the order total is refused rather than recorded. Stripe's limit of 999,999 per line needs
a response better than a 500 and belongs there too.
