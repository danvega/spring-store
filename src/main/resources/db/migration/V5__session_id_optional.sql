-- The order row is now written before the Stripe session exists, so it has no session id
-- for a moment. Unique still holds: Postgres allows many nulls in a unique index.
alter table purchase_order alter column stripe_session_id drop not null;
