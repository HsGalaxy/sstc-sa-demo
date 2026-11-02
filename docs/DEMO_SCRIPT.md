# Demo script — ten minute walkthrough

Preparation (2 minutes before the presentation):

```bash
# make sure the platform is running and the data is fresh
cd sstc-app
mvn -DskipTests package          # or: mvn-local.cmd -DskipTests package
java -jar backend/target/sstc-platform.jar
```

Open <http://localhost:8080> in a browser window and keep a second window on
<http://localhost:8080/swagger-ui.html> for the API view. Sign in as `lead` / `Lead@123`.

---

## 0. Framing (30 s)

> "This is the STC Platform: the architecture of our report, running. Four sub-systems, one
> dashboard, a Java backend and a relational database. Every screen names the user story and the
> Level-3 component it exercises, so you can check the design against the behaviour."

## 1. S01 Identity, role and access (90 s)

1. **Identity & access → Accounts & roles**: show the six seeded accounts, the three roles and
   the permission count per account.
2. Click an account: the *Effective access* panel lists its permissions. Point at the
   **policy overrides** (`GRANT S02.PERFORMANCE.VIEW`, `REVOKE S01.AUDIT.VIEW`) and explain that
   the effective set is `role permissions + grants − revokes`.
3. **Permission policies**: pick an account, type `S02.TASK.MANAGE`, choose `REVOKE`, apply.
   Then go back to the account list: the permission disappeared.
4. **Access audit**: the table records every protected call with the checked permission, the
   decision (`ALLOW`/`DENY`) and the latency. Refresh the page and show the new rows.

*Talking point:* the authorization filter writes the audit row even when it denies the call —
that is US04, the auditable access decision.

## 2. S02 Task dashboard (2 min)

1. **Kanban board**: four columns with the real tasks of cycle 4. Drag *API smoke test task* from
   **To Do** to **In Progress** — the card moves and the counters update.
2. Open a card: change the logged hours, tick **blocked**, save. Explain that the
   `ModificationScheduler` validates and persists, and the `OperationLogger` records the change.
3. **Operation log**: the last events show `TASK_MOVE`, `TASK_UPDATE` with actor, target and
   outcome.
4. **File tracker**: filter by bucket `DESIGN`, open a file and show the **version history**
   (`v3`, `v2`, `v1` with authors and change notes). Roll back to `v1` — the current version
   marker moves and the index row is updated.
5. **OKR & performance**: objectives with key results and progress bars, the team indicators, the
   bottleneck list, and the per-member score table (`0.4 × on-time + 0.3 × quality + 0.3 × OKR`,
   graded A–D).

## 3. S03 Team collaboration (2 min)

1. **Channels & chat**: pick `#toolbox-s04` and show the history. Type a message containing
   `@manager` and send it — the mention is detected, highlighted, and the "My mentions" panel on
   the right gets a new entry.
2. Open a second browser window as `manager` / `Manager@123`: the **Notifications & digest** page
   shows the mention notification, and the **Smart digest** ranks it first with its score and the
   reason "Direct @mention, needs an answer".
3. **Meetings & AI summary**: open *Cycle 4 review*. Show the transcript timeline, then the
   *AI summary* tab with overview, decisions, action items and the confidence value.
4. Create a meeting, press **Start**, then **End** — the platform transcribes and summarises it
   automatically and the new summary appears.

*Talking point:* ending the meeting is the trigger that the `MeetingOrchestrator` uses to invoke
the `AIMeetingAssistant`, exactly as in the sequence diagram.

## 4. S04 Toolbox (2 min)

1. **Documents**: open *STC Platform - architecture overview*, append a sentence. The revision
   increases and the edit history shows the operation, the editor, `resultCode = 0` and the
   remaining Office API quota. Explain the `GetQuota(apiName)` → `RunOfficeOperation(edit)`
   ordering; when the quota is exhausted the call returns the symbolic code `-1` and the document
   is left untouched.
2. **Git repositories**: open `sstc-platform`, show the commit history (short SHA, author,
   additions/deletions) and publish a new change. This is `Commit(changes)` followed by
   `Push(repoID, changes)`.
3. **Dev environments**: show two running containers with uptime and heartbeat, start a new one
   (`serviceId`, image, CPU, memory, ports) and look up its status. Emphasise that *request
   accepted* (`int 0`) is distinct from the runtime state (`RUNNING`/`STOPPED`/`FAILED`).
4. **Project files**: upload a file, then create a share link. Show the token, the expiry and the
   access counter. Try to upload `virus.exe` — the validation refuses it with the symbolic code
   `-2`. Show the *All share links* panel and revoke a link, then explain that the public download
   path checks revocation and expiry.

## 5. Architecture and API (1 min)

1. **Architecture map**: the C4 → code map lists every Level-3 component, the database tables and
   the REST endpoints per sub-system, next to the live health check.
2. Switch to the Swagger window: every endpoint of the report is documented and callable.
3. Optionally show `/h2-console` and the `td_task`, `tc_mention`, `usr_access_audit` tables to
   prove the data is persisted.

## 6. Closing (30 s)

> "The architecture is not only drawn, it is executed: requirements map to components, components
> to classes, classes to endpoints and tables, and every decision is auditable. The platform runs
> from a single jar with one database file."

---

## Fallback plan

| Risk | Mitigation |
| --- | --- |
| The port is taken | `java -jar backend/target/sstc-platform.jar --server.port=8081` |
| The database is dirty from earlier experiments | delete `data/sstc.mv.db` and restart, or set `sstc.seed.reset=true` |
| No network | everything is local: H2, the frontend bundle and the simulated integrations |
| Live demo fails | show `docs/screenshots` and the `scripts/smoke-test.ps1` output |
| A question about a specific endpoint | open `/swagger-ui.html` and call it live |

## Useful commands

```bash
# reset the demo data
rm -rf data && java -jar backend/target/sstc-platform.jar

# 36 API checks
powershell -ExecutionPolicy Bypass -File scripts/smoke-test.ps1

# regenerate the screenshots
node scripts/capture-screenshots.mjs --base http://localhost:8080 --out docs/screenshots
```
