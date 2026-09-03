-- Every Stripe event this app has already handled. The unique constraint on
-- stripe_event_id is the whole mechanism: a redelivered event cannot be inserted twice,
-- so it cannot be processed twice.
create table stripe_event (
    id              bigint generated always as identity primary key,
    stripe_event_id text        not null unique,
    type            text        not null,
    received_at     timestamptz not null default now()
);
