# 002 - The confirming states

## What changes and why

The confirmation page currently prints "Payment received" whether or not the app has
recorded anything. Stripe redirects the browser back before the webhook necessarily
arrives, so that claim is regularly false. This splits the page in two: a confirming
state while the app has no record, and the existing paid confirmation once it does.

## Acceptance, and the proof for each

Every one of these runs the sequence reality produces: buy, land on the confirmation
URL, and only then let the webhook arrive.

1. Straight back from Stripe with no webhook yet, the page does not claim the store has
   recorded anything. It says the payment reached Stripe and the app is still waiting.
   Proof: `./verify confirming`

2. The same URL, after the webhook lands, shows the paid confirmation with reference,
   amount and timestamp.
   Proof: `./verify confirming`

3. Unrecorded and 15 seconds in, the page acknowledges the delay and says the payment is
   safe.
   Proof: `./verify confirming`

4. Unrecorded and 60 seconds in, the page stops claiming it will resolve and says
   plainly that Stripe has the payment and the store does not.
   Proof: `./verify confirming`

5. A recorded order never shows a confirming state, however long ago it was created.
   Proof: `./verify confirming`

6. The existing goal 1 behaviour still holds.
   Proof: `./verify all`

## Surface

- `dev.danvega.store.order.ConfirmingStage` - new, the waiting / slow / gave up decision
- `dev.danvega.store.web.OrderController` - branch on whether the order is recorded
- `src/main/jte/confirming.jte` - new
- `src/main/jte/confirmation.jte` - drop the null guard, it only renders recorded orders now
- `src/test/java/dev/danvega/store/ConfirmingTest.java` - new

## Decisions this forces

- **The threshold clock runs from arrival, not from `created_at`.** `created_at` is when
  Buy was clicked, which includes however long the buyer spent typing their card. Someone
  taking two minutes would arrive already past 60 seconds and be told the store has no
  record. The confirming URL carries a `since` timestamp instead, defaulted on first
  render and preserved across refreshes.
- **Re-checking is a meta refresh, not JavaScript.** Two seconds while waiting. This
  project has no client-side JavaScript anywhere and a plain HTML refresh keeps it that
  way. The refresh stops once the page has given up, because a page that has stopped
  claiming it will resolve should stop acting like it will.

## Out of scope for this feature

- Bad signature rejection (goal 3) and replay idempotency (goal 4).
- Missing-keys startup failure (goal 5).
- Any change to how the webhook records payment. This feature only changes what is shown
  while it has not.
