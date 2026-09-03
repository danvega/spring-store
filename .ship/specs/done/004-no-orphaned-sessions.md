# 004 - No orphaned Stripe sessions

## What changes and why

Today `CheckoutService.start()` calls Stripe inside its transaction and writes the order
afterwards, so a failed insert leaves a payable Stripe session that this app has no record
of. Two systems cannot share a transaction, so this does not make the gap disappear. It
makes the failure recoverable instead of silent, by writing the local record first and
giving Stripe a correlation id it hands back on the webhook.

Three changes: take the network call out of the transaction, write the order before
creating the session, and put the order reference in Stripe's `client_reference_id` so the
webhook can find the order even when the session id was never attached.

## Acceptance, and the proof for each

The sequence that matters is a Stripe call that succeeds while a local write does not, so
the proofs make the local side fail rather than the remote one.

1. The order row exists before Stripe is called. A Stripe failure leaves a pending order
   with no session id, not nothing at all.
   Proof: `./verify checkout`

2. The session id is attached once Stripe returns, so the ordinary path is unchanged.
   Proof: `./verify checkout`

3. A webhook for an order whose session id was never attached still records the payment,
   matched on `client_reference_id`.
   Proof: `./verify recovery`

4. Matching by reference attaches the session id, so the confirming page and any
   redelivery can find it directly afterwards.
   Proof: `./verify recovery`

5. An order matched by reference is recorded exactly once, the same as one matched by
   session id.
   Proof: `./verify recovery`

6. Everything from goals 1 through 4 still holds.
   Proof: `./verify all`

## Surface

- `src/main/resources/db/migration/V5__session_id_optional.sql` - new, drops NOT NULL
- `dev.danvega.store.order.PurchaseOrder` - `pending` loses the session id, gains
  `attachSession`
- `dev.danvega.store.order.PurchaseOrderRepository` - `findByReference`
- `dev.danvega.store.checkout.StripeGateway` and `StripeCheckoutGateway` - carry the
  order reference into `client_reference_id`
- `dev.danvega.store.checkout.CheckoutService` - order first, Stripe second, attach third,
  and no transaction spanning the network call
- `dev.danvega.store.webhook.OrderRecorder` - fall back to matching by reference
- `StubStripeConfiguration` - can be told to fail, so the proof can make Stripe blow up
- `src/test/java/dev/danvega/store/RecoveryTest.java` - new

## Out of scope for this feature

- **Reconciliation against Stripe.** If the process dies before any local write at all,
  only a sweep comparing Stripe's sessions to local orders would find it. That needs a
  scheduler, which is a stack decision this project has never made. It is a goal, not a
  fix, and it is not this.
- Goal 5, loud startup failure with no Stripe keys. This feature displaces it by one.
- An idempotency key on the Stripe create call, which prevents duplicate sessions on a
  retry rather than orphans.
