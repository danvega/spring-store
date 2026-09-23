# Held retro findings

Findings from `shipit-retro` that did not clear the bar for a skill edit. Each carries a
running count. The next retro adds to this rather than starting from zero.

Counts are per project. A finding that feels familiar from another project should be
promoted on the structural test instead of waiting for a count that will not arrive.

## Open

- **No testing slot in the shipit-stack spine.** (1: 3 Sep 2026) The spine covers language,
  deployment, persistence, auth, jobs and frontend. Test framework and test API are never
  asked, so MockMvc got picked silently and Dan asked for RestTestClient afterwards.
  `shipit-verify` builds the harness but does not ask either.
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

- **Running `./verify` from whatever directory the last command left behind.** (3: 3 Sep
  2026) Exit 127 three times, because a `cd` into the test package persisted and `./verify`
  lives at the root. Cleared the recurring bar at retro 2 and was not chosen, because the
  cause is shell discipline rather than anything the skill text says. Still owed an edit if
  it keeps happening.
- **PRODUCT.md grows past the limit shipit-mvp sets, and nothing measures it.** (1: 3 Sep
  2026, structural) 200 lines against a stated ~150, with 39 decision bullets making up
  about half. shipit-mvp names this as "the only failure mode that actually kills this file"
  and predicts it by feature eight. We are at feature six. Not chosen at retro 2 because
  the edit would encode behaviour already happening: it gets raised unprompted every time.
  Choose it if raising it ever stops working.

## Promoted

- **Proofs nobody has seen fail.** (retro 2, 3 Sep 2026) Became "make each new proof fail
  once before trusting it" in `shipit-feature` Step 3. Motivated by StartupTest passing while
  proving the opposite of its name: SpringApplicationBuilder.properties() writes to the
  lowest precedence tier, so a real secrets.properties outranked the blank keys and the app
  booted fine. The secrets check written the same day was made to fail on purpose and was
  sound. The difference between the two was whether anyone watched it go red.
- **Proofs that arrange their own sequence.** (3 Sep 2026) Became the "the proof has to run
  the sequence reality produces" paragraph in `shipit-feature` Step 3. Motivated by the
  confirmation page NPE that shipped green because every test posted the webhook before
  loading the page.
