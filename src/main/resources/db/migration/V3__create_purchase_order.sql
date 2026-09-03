-- "order" is reserved in SQL, so the table is purchase_order.
-- stripe_session_id is unique: one checkout session can only ever be one order.
create table purchase_order (
    id                bigint generated always as identity primary key,
    reference         text        not null unique,
    sticker_id        bigint      not null references sticker (id),
    amount_cents      integer     not null,
    stripe_session_id text        not null unique,
    created_at        timestamptz not null default now(),
    paid_at_stripe    timestamptz,
    recorded_at       timestamptz
);

create index purchase_order_session_idx on purchase_order (stripe_session_id);
