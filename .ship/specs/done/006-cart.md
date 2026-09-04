# 006 - The cart

## What changes and why

Goal 6. Today one click buys one sticker and the order holds a single `sticker_id`. This
makes the order a header with lines, adds a cart keyed by a cookie, and turns the
storefront's Buy into Add.

Most of the work is not the cart. It is the ripple: every screen that names a single
sticker has to name several, and every existing test buys through `/buy/{slug}`, which
stops existing.

## Acceptance, and the proof for each

The cart is built through the real endpoints in every proof. An order row assembled
directly in a test would prove nothing about whether adding and checking out work.

1. Adding a sticker to an empty cart creates the cart, sets the cookie, and shows one
   line at quantity one.
   Proof: `./verify cart`

2. Adding the same sticker again raises the quantity instead of adding a second line.
   Proof: `./verify cart`

3. Quantity can be changed and a line removed. Removing the last line leaves the empty
   cart screen, not a broken one.
   Proof: `./verify cart`

4. A visitor returning with the same cookie sees the same cart. The cart is in the
   database, not in memory.
   Proof: `./verify cart`

5. Checking out a cart of three stickers, one of them at quantity two, produces a single
   Stripe session and one order whose lines match the cart.
   Proof: `./verify cart-checkout`

6. The order total is the server's own arithmetic over its own prices, and the cart
   survives checkout, emptying only when the payment is recorded. Emptying at checkout
   would cost the cart of anyone who cancels at Stripe, and a refresh only creates a
   second unpaid order, which is harmless.
   Proof: `./verify cart-checkout`

7. The confirmation page lists every line with its quantity, not just the first.
   Proof: `./verify cart-checkout`

8. Everything from goals 1 through 5 still holds, including the webhook, replay and
   recovery behaviour, against the new order shape.
   Proof: `./verify all`

## Surface

- `V6__create_cart.sql` - `cart` and `cart_line`, cart keyed by the cookie value
- `V7__order_lines.sql` - `order_line`, existing orders migrated into it, then
  `purchase_order` drops `sticker_id` and renames `amount_cents` to `total_cents`
- `dev.danvega.store.cart` - Cart, CartLine, CartRepository, CartService, CartCookie,
  CartController
- `dev.danvega.store.order` - PurchaseOrder gains lines and a total, OrderLine is new
- `dev.danvega.store.checkout` - StripeGateway takes an order rather than a sticker,
  CheckoutService builds from the cart
- `src/main/jte` - cart, cart-empty, storefront, confirmation, confirming, cancelled
- Existing tests: every `/buy/{slug}` becomes add-then-checkout

## Out of scope for this feature

- **Goals 7 and 8.** Pricing is computed server-side here because that is the only
  sensible way to build it, but the proofs that a tampered request is ignored and that a
  webhook with a disagreeing `amount_total` is refused are spec 007.
- **Stripe's 999,999 per line limit.** Recorded as needing a response better than a 500,
  and it belongs with the other input handling in 007.
- Cart expiry, merging carts, or cleaning up abandoned ones.
