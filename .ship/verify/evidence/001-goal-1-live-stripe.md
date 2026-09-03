# Goal 1, proven against live Stripe test mode

3 September 2026. The one criterion in spec 001 that had no proof command, because it
needs the Stripe CLI and real test keys.

Setup: Spring Store sandbox, `stripe listen --forward-to localhost:8080/stripe/webhook`,
app on 8080, Postgres on host 5433. Paid with 4242 4242 4242 4242.

```
   reference   | amount_cents |     paid_at_stripe     |          recorded_at
---------------+--------------+------------------------+-------------------------------
 SPR-32A6-U92K |           99 |                        |
 SPR-WDL7-BPGR |           99 | 2026-09-03 21:18:48+00 | 2026-09-03 21:18:49.123293+00
```

- SPR-WDL7-BPGR: paid and recorded by the app's own webhook handler, not read back from
  the Stripe dashboard. Goal 1 satisfied.
- SPR-32A6-U92K: a checkout started and abandoned. Both timestamps null, which is the
  pending state behaving correctly.
- The gap between the two timestamps was about 1 second. That is the measured size of
  the window goal 2 has to be honest about.
