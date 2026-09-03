# 001 - Scaffold and first purchase

## What changes and why

Stands up the Spring Boot project and delivers goal 1: a visitor picks a sticker, pays
with a Stripe test card, and the app's own record says paid. Everything here exists to
make that round trip real, including the catalog to buy from and the webhook that
records the result.

## Acceptance, and the proof for each

1. The storefront lists the six seeded stickers, each with a buy button.
   Proof: `./verify storefront`

2. Buying creates a pending order row and redirects to Stripe Checkout.
   Proof: `./verify checkout`

3. A validly signed `checkout.session.completed` webhook flips that order to paid,
   setting both timestamps: paid at Stripe and recorded by the app.
   Proof: `./verify webhook`

4. The confirmation page, looked up by Stripe session id, shows the order as paid with
   its reference and amount.
   Proof: `./verify confirmation`

5. The full browser round trip against real Stripe test mode.
   Proof: none possible. Needs the Stripe CLI and real test keys, both outside the repo,
   so this hits the Stops list. Parked for the batch with instructions for Dan.

## Surface

- `compose.yaml`, `pom.xml`, `application.properties`
- `src/main/resources/db/migration/V1..V3`
- `dev.danvega.store.catalog` - Sticker, StickerRepository
- `dev.danvega.store.order` - PurchaseOrder, PurchaseOrderRepository, OrderReference
- `dev.danvega.store.checkout` - StripeProperties, CheckoutService, CheckoutController
- `dev.danvega.store.webhook` - StripeWebhookController, OrderRecorder
- `dev.danvega.store.web` - StorefrontController, OrderController
- `src/main/jte` - layout, storefront, confirmation, cancelled
- `./verify` and the tests behind it

## Out of scope for this feature

- **The confirming states (goal 2).** The confirmation page here assumes the webhook
  already landed, which is exactly the weakness goal 2 exists to fix.
- **Proving bad signatures are rejected (goal 3).** The handler verifies signatures
  because Stripe's SDK needs that to parse the event at all, but the test that proves it
  is its own delta.
- **Replay idempotency (goal 4).** No stripe_event table yet.
- **Missing-keys startup failure (goal 5).**
- **The empty state screen.**
