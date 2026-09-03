# Open

What is owed, carried out of `ship-feature` review batches. A state, not a log: items are
deleted when they are addressed, not marked done.

Read this alongside PRODUCT.md's `Next:` line at the start of a session.

## Review findings not fixed

- **`CheckoutService.start()` creates the Stripe session inside its transaction.**
  Open since goal 1. If `orders.save()` fails after Stripe returns a session, the buyer
  gets a payable session with no order row. They pay, and the app has no record. Goal 4
  stopped this becoming permanently unrecoverable, since an event with no matching order
  is no longer claimed, but the orphaned session is still possible. The real answer is
  reconciliation against Stripe, which no goal currently covers.

## Latent risks worth knowing before touching the code

- **`confirmation.jte` dereferences `paidAtStripe` guarded only by `isRecorded()`**, which
  reads `recordedAt`. The two are only ever set together inside
  `PurchaseOrder.recordPayment`. Anything that sets one without the other brings back the
  NullPointerException from goal 1. The null guard that used to cover this was removed
  when goal 2 made the template render recorded orders only.
