# Storefront design brief

Disposable. Delete this folder once the decisions are extracted into PRODUCT.md.

## Screen

The storefront: the single page listing the stickers, where someone picks one and buys
it. It is the hardest screen because it is the only one with real content, and because
it has to make a one-click purchase obvious without implying a cart that does not exist.

## What this has to make obvious

What is for sale, that each one is 99 cents, and how to buy one. Buying is one click per
sticker, straight to Stripe's hosted checkout.

## Real content

Six stickers, all $0.99. **Provisional, not confirmed with Dan.**

- Spring Boot Leaf
- It Works On My Machine
- NullPointerException Survivor
- @Autowired
- Stack Overflow Driven Development
- Ship It

Longest name: "Stack Overflow Driven Development". Shortest: "Ship It". Both sit in the
same grid, so the layout has to hold the long one without truncating.

## States to design

- Default: all six listed
- Post-purchase: confirmation after paying, showing sticker name, amount paid, order
  reference. Must read as a real confirmation, not a thanks-for-your-interest page
- Cancelled: buyer backed out of Stripe checkout and returned
- Empty: no stickers available, rare, should not look broken

## Constraints

From the PRODUCT.md non-goals. Do not design anything that implies these exist:

- No accounts, sign in, or order history. The buyer is anonymous
- No cart, wishlist, or quantities
- No shipping address, tax, or delivery estimates
- No search, filters, or categories
- No real money. Stripe test mode only

## Do not design

Navigation, footer links, settings, onboarding, an about page, or any card entry form.
Stripe hosts the payment page, so no card fields exist anywhere in this app.

## Visual direction

Round 1, left open. Ask for three options to compare. Plain but deliberate: this is a
developer's test app and heavy design would misrepresent it. Clarity and long-name
handling matter more than decoration.

## Open questions

- Is the catalog above right, or does Dan have real sticker names?
- One-click buy per sticker assumes no cart. Confirmed by the non-goals, not by Dan.
- PRODUCT.md lists "a polished storefront" as a non-goal, which caps how far this pass
  should go. Either that boundary holds and this stays deliberately plain, or it moves.
