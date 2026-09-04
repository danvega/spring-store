-- An order becomes a header with lines. unit_price_cents is copied at checkout rather
-- than joined at read time, so a later price change cannot rewrite what someone paid.
create table order_line (
    id               bigint  generated always as identity primary key,
    purchase_order   bigint  not null references purchase_order (id) on delete cascade,
    sticker_id       bigint  not null references sticker (id),
    quantity         integer not null check (quantity > 0),
    unit_price_cents integer not null
);

-- Existing single-sticker orders become one-line orders. Dropping the column without
-- this would silently empty every order already in the table.
insert into order_line (purchase_order, sticker_id, quantity, unit_price_cents)
select id, sticker_id, 1, amount_cents from purchase_order;

-- The order remembers which cart it came from, so the webhook can empty that cart when
-- the payment is recorded. Nullable: orders that predate the cart have none.
alter table purchase_order add column cart_id text;

alter table purchase_order drop column sticker_id;
alter table purchase_order rename column amount_cents to total_cents;
