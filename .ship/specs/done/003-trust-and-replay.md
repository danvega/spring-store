# 003 - Trusted events, and surviving a replay

## What changes and why

Goals 3 and 4 together, because they are the same handler. The signature check already
happens, since Stripe's SDK cannot parse an event without verifying it, so goal 3 is
mostly a proof that it does. Goal 4 is real work: a `stripe_event` table whose unique
constraint on the Stripe event id is what makes a redelivery a no-op rather than a second
write.

## Acceptance, and the proof for each

Goal 4's proofs send the same event id twice with a fresh signature each time, which is
how Stripe actually redelivers. A test that invents two different events would prove
nothing.

1. A webhook whose signature does not match is rejected and records nothing.
   Proof: `./verify signature`

2. A webhook with no `Stripe-Signature` header at all is rejected.
   Proof: `./verify signature`

3. A payload tampered with after signing is rejected, even though the signature is
   otherwise well formed.
   Proof: `./verify signature`

4. Stripe redelivering the identical event leaves exactly one recorded event and does not
   move the order's `recorded_at`. Moving it would mean the second delivery was processed.
   Proof: `./verify replay`

5. Two genuinely different events are both processed. The replay guard must key on the
   event id, not on the session.
   Proof: `./verify replay`

6. A rejected event leaves no `stripe_event` row, so Stripe's retry of a request that
   failed for another reason still works.
   Proof: `./verify replay`

7. Everything from goals 1 and 2 still holds.
   Proof: `./verify all`

## Surface

- `src/main/resources/db/migration/V4__create_stripe_event.sql` - new
- `dev.danvega.store.webhook.StripeEvent` and `StripeEventRepository` - new
- `dev.danvega.store.webhook.OrderRecorder` - claim the event id and record in one
  transaction, so a failure rolls back the claim too and Stripe's retry still works
- `dev.danvega.store.webhook.StripeWebhookController` - treat a duplicate as success
- `src/test/java/dev/danvega/store/SignatureTest.java` and `ReplayTest.java` - new

## Out of scope for this feature

- Goal 5, loud startup failure with no Stripe keys.
- Retrying or reconciling an event that was rejected. Stripe already retries.
- `CheckoutService.start()` creating the Stripe session inside its transaction, which is
  a known finding carried in the review batch since goal 1.

## A trap to avoid

`recordPayment` sets `paidAtStripe` and `recordedAt` together, and `confirmation.jte`
dereferences `paidAtStripe` guarded only by `isRecorded()`, which reads `recordedAt`. If
the replay work ever sets one without the other, the NullPointerException from goal 1
comes back.
