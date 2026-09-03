create table sticker (
    id          bigint generated always as identity primary key,
    slug        text    not null unique,
    name        text    not null,
    price_cents integer not null check (price_cents > 0),
    color       text    not null,
    sort_order  integer not null
);
