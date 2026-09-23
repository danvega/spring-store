# Open

What is owed, carried out of `shipit-feature` review batches. A state, not a log: items are
deleted when they are addressed, not marked done.

Read this alongside PRODUCT.md's `Next:` line at the start of a session.

## Known limitations, deliberate

- **No reconciliation against Stripe.** Spec 004 made an orphaned session recoverable by
  writing the order first and correlating on `client_reference_id`. What it cannot cover
  is the process dying before any local write at all. Only a sweep comparing Stripe's
  sessions against local orders finds that, which needs a scheduler, which is a stack
  decision this project has never made. It is a goal, not a fix.
- **Pending orders accumulate.** A failed Stripe call and an abandoned checkout both leave
  a permanent pending row, and nothing distinguishes them or cleans them up. Harmless at
  this scale and the accepted cost of writing the order first.

## Weak spots in the proofs

- **`./verify secrets` only matches known Stripe key shapes.** It covers sk_, rk_, whsec_
  and pk_live_. Anything encoded, or any other provider's credential, walks straight
  through. A pass means "no Stripe key material", not "no secrets".

## Latent risks worth knowing before touching the code

- **`confirmation.jte` dereferences `paidAtStripe` guarded only by `isRecorded()`**, which
  reads `recordedAt`. The two are only ever set together inside
  `PurchaseOrder.recordPayment`. Anything that sets one without the other brings back the
  NullPointerException from goal 1. The null guard that used to cover this was removed
  when goal 2 made the template render recorded orders only.
- **`OrderRecorder` does not check whether the matched order is already recorded.** Not
  reachable today, because a reference maps to exactly one session, so the event id guard
  catches every real redelivery. It would become reachable if anything ever gave two
  Stripe sessions the same `client_reference_id`.
- **A cart line at int's maximum wraps when it grows.** Only a tampered request can set
  a quantity of 2,147,483,647. From there, Add computes `quantity + 1` in `CartLine.plus`,
  the result wraps negative, and the request fails with a 500. The + button does the same
  sum in `cart.jte`, and its negative result clamps to 0 and removes the line. Nothing is
  ever charged. If it matters, saturate the add rather than cap the cart.
