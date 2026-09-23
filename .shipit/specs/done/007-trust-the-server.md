# 007 - Trust the server's arithmetic

## What changes and why

Goals 7 and 8, and the MVP.

Goal 7 is already true by construction. A cart line stores a sticker id and a quantity,
and `CheckoutService` reads every price from the sticker table, so no price has ever come
from the browser. This spec proves that rather than building it, which is the honest
shape: the value is in showing a tampered request changes nothing.

Goal 8 is not built. The webhook records a payment without ever looking at
`amount_total`. An order for $3.96 would be recorded as paid on an event claiming one
cent, and nothing would notice.

## What "refused" means, decided here

A mismatch is refused with a 400, nothing is recorded, and the event is **not claimed**.
Reasoning:

- 400 rather than 200 because Stripe's dashboard then shows a failed delivery. A
  mismatch is the one thing in this project that should be loud in two places, not one.
- Not claimed, because spec 003's rule is that an event the app could not act on keeps
  its retry. A mismatch will not resolve on retry, but claiming it would foreclose a
  redelivery after the underlying problem is fixed.
- The order stays unrecorded, so the buyer sees goal 2's honest "Stripe has the payment,
  the store does not" screen. That machinery already exists and says exactly the right
  thing.

## Acceptance, and the proof for each

1. Changing a quantity through the real endpoint changes only the quantity. The total
   remains the server's arithmetic over the sticker table's prices.
   Proof: `./verify pricing`

2. A zero or negative quantity removes the line rather than producing a negative total.
   Proof: `./verify pricing`

3. Extra request parameters that look like prices or totals are ignored completely.
   Proof: `./verify pricing`

4. What Stripe is asked to charge equals the server's own sum, for every line.
   Proof: `./verify pricing`

5. A line quantity above Stripe's limit of 999,999 is refused before Stripe is called,
   and the visitor gets the cart back with a message rather than a 500.
   Proof: `./verify pricing`

6. An event whose `amount_total` disagrees with the order total is refused: 400, nothing
   recorded, no claim written, and the disagreement logged with both numbers.
   Proof: `./verify amount`

7. An event whose `amount_total` agrees is recorded exactly as before, including the
   multi-line case.
   Proof: `./verify amount`

8. A refused mismatch does not block a later correct delivery for the same event.
   Proof: `./verify amount`

9. Everything from goals 1 through 6 still holds.
   Proof: `./verify all`

## Surface

- `dev.danvega.store.webhook.RecordOutcome` - an AmountMismatch case
- `dev.danvega.store.webhook.OrderRecorder` - compare before recording
- `dev.danvega.store.webhook.StripeWebhookController` - pass amount_total, map the
  mismatch to 400
- `dev.danvega.store.checkout.CheckoutService` - refuse a line over Stripe's limit
- `dev.danvega.store.web.CartController` - show the cart again with the message
- `src/main/jte/cart.jte` - render that message
- `IntegrationTest.completedEvent` - parameterise `amount_total`, which several existing
  tests depend on
- `src/test/java/dev/danvega/store/PricingTest.java` and `AmountTest.java` - new

## Out of scope for this feature

- Reconciliation against Stripe, still recorded in `.shipit/open.md` as a goal rather than
  a fix.
- Currency. Everything is USD and nothing checks the event's currency field. Worth
  noting because a currency mismatch is the same class of bug as an amount mismatch.
- Any cap on quantity. The recorded decision is that there is none, and Stripe's own
  limit is a boundary to report, not a policy to adopt.
