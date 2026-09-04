-- An anonymous visitor's cart, found by the value of their cart_id cookie. There are no
-- accounts, so the cookie is the only identity there is.
create table cart (
    id         bigint      generated always as identity primary key,
    cart_id    text        not null unique,
    created_at timestamptz not null default now()
);

-- One row per sticker in the cart. Adding the same sticker again raises quantity rather
-- than adding a second line, which the unique constraint enforces rather than trusts.
create table cart_line (
    id         bigint  generated always as identity primary key,
    cart       bigint  not null references cart (id) on delete cascade,
    sticker_id bigint  not null references sticker (id),
    quantity   integer not null check (quantity > 0),
    unique (cart, sticker_id)
);
