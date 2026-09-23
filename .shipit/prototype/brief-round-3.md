# Round 3: the cart

Done in static HTML rather than handed to a design tool. The visual direction is locked
and recorded, so the aesthetic question is closed. What is open is arrangement, and
`shipit-prototype` says plain HTML is the right answer when the question is density and
interaction rather than aesthetics.

A cart is also a solved layout. This round exists to settle product questions, not to
discover a look.

## Screens

1. Cart with items, one of them at quantity two
2. Empty cart
3. Storefront, whose button changes from Buy to Add

## What this has to settle

- **Does quantity have a cap?** Unbounded lets someone build a 400-dollar order out of
  99-cent stickers, which is fine in test mode and odd in a demo.
- **Is the empty cart the same screen as the empty storefront?** They are different
  emptiness. One means nothing is for sale. The other means you have not picked anything.
- **Does the storefront show what is in the cart?** Without it, adding gives no feedback
  and the cart is unreachable except by a link nobody sees.

## Constraints, unchanged

No accounts, so the cart is anonymous. No search, filters, categories or product pages,
which stayed on the non-goals list. Test mode only.
