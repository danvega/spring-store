# Held retro findings

Findings from `ship-retro` that did not clear the bar for a skill edit. Each carries a
running count. The next retro adds to this rather than starting from zero.

Counts are per project. A finding that feels familiar from another project should be
promoted on the structural test instead of waiting for a count that will not arrive.

## Open

- **No testing slot in the ship-stack spine.** (1: 3 Sep 2026) The spine covers language,
  deployment, persistence, auth, jobs and frontend. Test framework and test API are never
  asked, so MockMvc got picked silently and Dan asked for RestTestClient afterwards.
  `ship-verify` builds the harness but does not ask either.
- **Nothing scans tracked files for secrets.** (1: 3 Sep 2026) Real Stripe test keys were
  pasted into `application.properties` as placeholder defaults, straight against goal 5.
  Caught by accident while reading the file for another reason, not by any step.
- **Acting on an authoritative but wrong source without a one-command check.**
  (2: 3 Sep 2026) A stale search.maven.org index said `jte-spring-boot-starter-4` did not
  exist; `maven-metadata.xml` settled it in one call. The `rest-test-client` skill said
  `bodyValue`; `javap` settled it in one call. One of the two produced wrong advice to Dan.
- **Misreading a piped exit code as success.** (1: 3 Sep 2026) `./verify all 2>&1 | tail`
  reports tail's status. Reported green while 7 of 9 tests had failed.
- **Changing a generated scaffold default without a reason.** (1: 3 Sep 2026) Initializr's
  ephemeral compose port was pinned to 5432, which collided with an existing Postgres and
  cost a debugging cycle.

- **Writing a derived number into PRODUCT.md from memory instead of reading it.**
  (2: 3 Sep 2026) The test count in `## Current state` was wrong twice in one session,
  16 against 17 and then 25 against 24. Harmless both times, same reflex both times. One
  more and it clears the bar.

## Promoted

- **Proofs that arrange their own sequence.** (3 Sep 2026) Became the "the proof has to run
  the sequence reality produces" paragraph in `ship-feature` Step 3. Motivated by the
  confirmation page NPE that shipped green because every test posted the webhook before
  loading the page.
