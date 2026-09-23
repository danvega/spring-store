# 005 - Fail loudly without keys

## What changes and why

Goal 5 has two halves. Secrets must not be in the repo, which is a claim about tracked
files and gets a shell proof, not a Java one. And a clone without keys must fail at
startup with a clear message, which today is a raw Spring placeholder error that says
what is missing but not what to do about it.

The keys gain an empty default so binding actually happens, and `StripeProperties` gains
`@Validated` with messages that say where to get each value. Failing later than the
placeholder does is not a loss: both refuse to start, and only one of them is useful.

## Acceptance, and the proof for each

1. No tracked file contains Stripe key material.
   Proof: `./verify secrets`

2. `secrets.properties` is ignored by git, so putting keys there cannot leak them.
   Proof: `./verify secrets`

3. The application refuses to start when the keys are absent. Not at the first click,
   not on the first webhook, at startup.
   Proof: `./verify startup`

4. The failure names the property that is missing and how to obtain it. A message that
   only says a placeholder could not be resolved does not clear this.
   Proof: `./verify startup`

5. A blank key is treated the same as a missing one, since an empty environment variable
   is the likelier mistake than an absent one.
   Proof: `./verify startup`

6. Everything from goals 1 through 4 still holds.
   Proof: `./verify all`

## Surface

- `src/main/resources/application.properties` - empty defaults so binding runs
- `dev.danvega.store.checkout.StripeProperties` - `@Validated`, `@NotBlank`, real messages
- `verify` - new `secrets` and `startup` targets
- `src/test/java/dev/danvega/store/StartupTest.java` - new

## Out of scope for this feature

- Scanning git history for secrets committed in the past. There is one commit and it was
  checked before it was made.
- Any secret manager or vault. `secrets.properties` plus environment variables is the
  recorded approach and deployment is a non-goal.
- Validating that a key is well formed or that Stripe accepts it. Absent is the failure
  worth catching. A wrong key fails at the first call to Stripe with Stripe's own error.
