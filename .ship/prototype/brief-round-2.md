# Round 2: the confirming state

Round 1 locked the visual language. This round adds the one screen it revealed was
missing, and does not reopen the direction.

## Screen

"Confirming": the gap between the buyer returning from Stripe and the webhook landing.
Round 1's confirmation page asserts PAYMENT RECEIVED unconditionally, which can be false
at render time. This screen is what honest looks like in that window.

## Why it is the hardest one left

It is the only screen with no good default. Too reassuring and it lies. Too alarming and
it reads as a failed payment when the money moved fine.

## States

1. Waiting, normal, under a couple of seconds
2. Still waiting after ~15s: acknowledge the delay, confirm the payment is safe
3. Gave up after ~60s: paid at Stripe, never recorded by the app. The failure this whole
   PoC exists to catch, so it gets shown honestly

## Constraints

No order reference exists yet in states 1 and 2, so no reference field. The buyer has no
action that fixes this, so imply none.

## Visual direction

Locked to the vocabulary in PRODUCT.md. Match, do not redesign.
