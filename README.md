<div align="center">

# STC Platform

**Small Team Collaboration — a working implementation of the architecture designed for the
"Smart Working Management App (small teams)" case study**

[![Java](https://img.shields.io/badge/Java-8-007396?logo=openjdk&logoColor=white)](#tech-stack)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-2.7.18-6DB33F?logo=springboot&logoColor=white)](#tech-stack)
[![React](https://img.shields.io/badge/React-18-61DAFB?logo=react&logoColor=black)](#tech-stack)
[![Database](https://img.shields.io/badge/Database-H2%20(file)-004C8C?logo=databricks&logoColor=white)](#tech-stack)
[![License](https://img.shields.io/badge/License-MIT-blue.svg)](LICENSE)

A full stack reference implementation of the four sub-systems specified in the
Software Architecture group project: **S01 identity & access**, **S02 task dashboard**,
**S03 team collaboration** and **S04 toolbox**. The C4 design of the report is mapped onto real
packages, real REST endpoints and a real relational schema, so the architecture can be
demonstrated instead of only described.

</div>

---

## 1. What this is

> ### ▶ Live demo
>
> **<http://113.44.246.242:18080>** — deployed on a 2 vCPU / 2 GB Debian 12 host, running from a
> single jar behind systemd. There is no registered domain, so it answers on a high port over
> plain HTTP.
>
> Sign in with any seeded account, for example **`lead` / `Lead@123`** (Overall Lead) or
> **`yunteng` / `Member@123`** (Team Member, S04 toolbox). The full list is on the sign-in page.
>
> This is a demo instance: treat the credentials as public and do not put anything sensitive in
> it. Sizing rationale and the deployment procedure: [`docs/DEPLOYMENT.md`](docs/DEPLOYMENT.md).

The case study asks for a smart working management app for small teams: one shared dashboard
for tasks, deadlines, conversations, files and the productivity tools a team needs. This
repository implements that app:

| Layer | What runs |
| --- | --- |
| Frontend | React 18 + TypeScript single page application (Vite), 17 screens across the four sub-systems |
| Backend | Java 8 + Spring Boot 2.7 REST API, organised as one package per sub-system |
| Database | H2 file database (`./data/sstc.mv.db`) accessed through Spring Data JPA / Hibernate |
| Extras | OpenAPI UI, seeded demo data, audit trails, headless screenshot tooling |

Everything is served from a **single jar**: the Maven build packages the compiled frontend into
`backend/src/main/resources/static`, so one `java -jar` gives you the API, the dashboard, the
OpenAPI documentation and the H2 console.

## 2. Screens

Seventeen screens across the four sub-systems, captured from the running build with headless
Chrome (`scripts/capture-screenshots.mjs`). Every screen names the user story and the Level-3
component it exercises.

### Overview and architecture map

| Dashboard — aggregated indicators of all four sub-systems | Architecture map — C4 → code, tables and endpoints |
| --- | --- |
| ![Dashboard](docs/screenshots/overview.png) | ![Architecture map](docs/screenshots/architecture.png) |

### S02 · Task Dashboard

| Kanban board — drag and drop between columns | File tracker — multi-condition query and version history |
| --- | --- |
| ![Kanban board](docs/screenshots/tasks.png) | ![File tracker](docs/screenshots/files.png) |

| OKR, indicators and performance score | Operation log — full lifecycle events |
| --- | --- |
| ![OKR and performance](docs/screenshots/okr.png) | ![Operation log](docs/screenshots/operations.png) |

### S03 · Team Collaboration

| Channels, group chat and @mentions | Meetings, transcript and AI summary |
| --- | --- |
| ![Collaboration](docs/screenshots/collaboration.png) | ![Meetings](docs/screenshots/meetings.png) |

| Notifications and ranked smart digest |
| --- |
| ![Inbox and digest](docs/screenshots/inbox.png) |

### S04 · Toolbox

| Documents — editing with the Office API quota check | Presentations — slides of a deck |
| --- | --- |
| ![Documents](docs/screenshots/toolbox-documents.png) | ![Presentations](docs/screenshots/toolbox-presentations.png) |

| Git repositories — commit history and publishing | Dev environments — container runtime |
| --- | --- |
| ![Git repositories](docs/screenshots/toolbox-repos.png) | ![Dev environments](docs/screenshots/toolbox-environments.png) |

| Project files — upload, status and share links |
| --- |
| ![Project files](docs/screenshots/toolbox-files.png) |

### S01 · Identity, Role & Access

| Accounts, roles and effective permissions | Permission policies — GRANT / REVOKE overrides |
| --- | --- |
| ![Accounts](docs/screenshots/identity-users.png) | ![Policies](docs/screenshots/identity-policies.png) |

| Access audit — every authorization decision |
| --- |
| ![Access audit](docs/screenshots/identity-audit.png) |

All originals live in [`docs/screenshots`](docs/screenshots).

## 3. Quick start

### Prerequisites

* **JDK 8 or newer** (the code is compiled for Java 8; JDK 8 is what the demo uses)
* **Maven 3.8+** (or use the helper below with a local Maven distribution)
* **Node.js 18+** and npm — only needed if you want to rebuild the frontend

### Build and run

```bash
# 1. build the frontend into the backend static resources
cd frontend
npm install
npm run build

# 2. package and start the platform
cd ..
mvn -DskipTests package
java -jar backend/target/sstc-platform.jar
```

Then open <http://localhost:8080>.

| URL | Purpose |
| --- | --- |
| <http://localhost:8080> | The dashboard |
| <http://localhost:8080/swagger-ui.html> | OpenAPI / Swagger UI |
| <http://localhost:8080/v3/api-docs> | OpenAPI JSON |
| <http://localhost:8080/h2-console> | Database console (JDBC `jdbc:h2:file:./data/sstc;AUTO_SERVER=TRUE`, user `sstc`, password `sstc`) |

Windows helper that pins `JAVA_HOME` and uses a project local Maven repository:

```bat
mvn-local.cmd -DskipTests package
```

### Development mode (hot reload)

```bash
cd frontend
npm run dev      # http://localhost:5173, /api is proxied to the backend on :8080
```

### Demo accounts

Every account is created by the seeder; the password is shown in the sign-in screen.

| Username | Password | Role | Focus |
| --- | --- | --- | --- |
| `lead` | `Lead@123` | Overall Lead | S01 identities, roles, permission policies, audit |
| `manager` | `Manager@123` | Senior Manager | S02 task review, OKR, project files |
| `yunteng` | `Member@123` | Team Member | S04 toolbox (documents, Git, containers, files) |
| `jinghui` | `Member@123` | Team Member | S02 dashboard and file tracker |
| `changyang` | `Member@123` | Team Member | S03 collaboration and notifications |
| `hao` | `Member@123` | Team Member | S01 identity implementation |

## 4. Architecture

The repository keeps the same vocabulary as the design documents: **sub-system → container →
component**, with a one-to-one mapping from every Level-3 component to a service or adapter
class, and from every user story to a REST endpoint.

```
S01 Identity, Role & Access          S02 Task Dashboard
  UserRoleManager                      TaskDashboard
  ├─ AccountController                 ├─ ModificationScheduler   (US03)
  ├─ RoleAssignmentController          ├─ OperationLogger         (US04)
  ├─ PermissionPolicyController        ├─ FileQueryController     (US01)
  └─ AuthorizationAuditController      ├─ HistoricalVersionController (US01)
                                       ├─ FileBucket              (US02)
S03 Team Collaboration                 ├─ DisplayPanel            (US05)
  TeamCollaboration                    └─ PerformanceCalculator   (US06)
  ├─ ChannelMessageManager (US02)
  ├─ MentionDetector       (US03)     S04 Toolbox
  ├─ MeetingOrchestrator   (US01)       Toolbox
  ├─ AIMeetingAssistant    (US05)       ├─ WordDocumentBuilder / PPTPresentationBuilder
  ├─ NotificationDispatcher(US04)       ├─ OfficeAPIAdapter
  └─ SmartDigestEngine     (US06)       ├─ VersionControlService / GitRepositoryAdapter
                                        ├─ ContainerRuntimeAdapter
                                        └─ FileUploadHandler / StorageAdapter / ShareLinkService
```

* Java packages: `com.sstc.platform.{identity,dashboard,collaboration,toolbox,common}`
* Database: 35 tables prefixed `usr_` (S01), `td_` (S02), `tc_` (S03), `tb_` (S04), `cm_` (audit)
* Full mapping, endpoint inventory and the sequence-diagram scenarios: [`docs/ARCHITECTURE.md`](docs/ARCHITECTURE.md)
* Complete REST reference: [`docs/API.md`](docs/API.md)
* Ten minute walkthrough for the presentation: [`docs/DEMO_SCRIPT.md`](docs/DEMO_SCRIPT.md)
* Deployment on a server (sizing, systemd, Docker, reverse proxy, backup): [`docs/DEPLOYMENT.md`](docs/DEPLOYMENT.md)

## 5. Tech stack

| Concern | Choice | Why |
| --- | --- | --- |
| Language | Java 8 | Widely available JDK; keeps the build runnable on any lab machine |
| Framework | Spring Boot 2.7.18 | Mature LTS line for Java 8, batteries included |
| Persistence | Spring Data JPA + Hibernate 5.6 | Entities document the domain model directly |
| Database | H2 (file mode, `AUTO_SERVER=TRUE`) | Zero install, survives restarts, inspectable in a console |
| API docs | springdoc-openapi 1.6 | Generated from the controllers, no hand written spec |
| Security | Session tokens + BCrypt + authorization filter | Implements the S01 identity API and the access audit trail |
| Frontend | React 18 + TypeScript + Vite | Fast build, typed API client, no UI framework lock-in |
| Styling | Hand written CSS design system (`src/styles.css`) | No dependency, consistent across the four sub-systems |

## 6. Feature checklist

**S01 Identity, Role & Access** — accounts (create / update / deactivate / view), roles,
18 permissions, per-account GRANT / REVOKE policy overrides, session issue and revocation,
effective permission lookup, access audit of every protected call.

**S02 Task Dashboard** — Kanban board with drag and drop, task CRUD with logging, delivery
cycles, file tracker with multi-condition query and classified buckets, file version history
and rollback, OKR and key results, live indicators, bottleneck detection, weighted performance
score with A–D grading, full operation log.

**S03 Team Collaboration** — channels and group chat, `@mention` detection with targeted
notifications, meeting lifecycle, simulated transcription, AI meeting summary (overview,
decisions, action items, confidence), notification inbox and a ranked smart digest.

**S04 Toolbox** — online document editing with an Office API quota check, slide decks, Git
repositories with commit history and publishing, containerised development environments with
start / stop / heartbeat and status lookup, project file upload with validation, share links
with expiry and revocation, storage and integration status panels.

## 7. Project layout

```
sstc-app/
├── backend/                     Spring Boot application (Java 8)
│   ├── src/main/java/com/sstc/platform/
│   │   ├── common/              audit, security, seed data, web plumbing
│   │   ├── identity/            S01 domain, repositories, service, REST
│   │   ├── dashboard/           S02
│   │   ├── collaboration/       S03
│   │   └── toolbox/             S04
│   └── src/main/resources/
│       ├── application.yml      runtime configuration
│       └── static/              compiled frontend (generated by `npm run build`)
├── frontend/                    React + TypeScript dashboard
│   └── src/
│       ├── api/client.ts        typed API client (single source of truth for the contract)
│       ├── app/                 auth and toast providers
│       ├── components/          app shell and UI kit
│       └── pages/               one page per screen
├── docs/                        architecture, API reference, demo script, screenshots
├── scripts/                     smoke test, screenshot capture, build helpers
└── pom.xml                      Maven parent (module: backend)
```

## 8. Verification

```bash
# API level smoke test: 36 checks across S01–S04 plus the negative cases
powershell -ExecutionPolicy Bypass -File scripts/smoke-test.ps1

# capture every screen with headless Chrome (writes docs/screenshots)
node scripts/capture-screenshots.mjs --base http://localhost:8080 --out docs/screenshots
```

The smoke test exercises authentication, the auth guard, task CRUD and moves, the file tracker
and version history, mentions and notifications, the meeting transcript and AI summary, document
editing, repository publishing, environment start and status lookup, file upload validation and
share links.

## 9. Configuration

Everything lives in [`backend/src/main/resources/application.yml`](backend/src/main/resources/application.yml):

| Key | Default | Meaning |
| --- | --- | --- |
| `server.port` | `8080` | HTTP port |
| `spring.datasource.url` | `jdbc:h2:file:./data/sstc;AUTO_SERVER=TRUE` | Database file, relative to the working directory |
| `sstc.seed.enabled` | `true` | Create the demo team on first start |
| `sstc.seed.reset` | `false` | Re-seed even when data exists |
| `sstc.security.token-ttl-hours` | `12` | Session lifetime |
| `sstc.toolbox.office.quota-per-day` | `200` | Simulated Office API quota used by the quota check |
| `sstc.toolbox.container.max-environments` | `12` | Container runtime limit |
| `sstc.toolbox.storage.max-upload-mb` | `25` | Upload validation limit |

## 10. Notes and limitations

* The external systems of the Level-1 context diagram (Microsoft 365, GitHub, Docker, object
  storage, the large-model API) are **simulated in process**. The adapters, their contracts and
  their result codes are real; the network calls are not. Every simulated boundary is labelled in
  the UI and in the Javadoc.
* AI meeting summarisation uses a deterministic extractive summariser so a demo is reproducible;
  the interface is the one a real speech-to-text plus LLM pipeline would implement.
* The transcription, quota and container state are modelled with symbolic result codes
  (`0` success, `-1` quota exhausted, `-2` upload rejected) exactly as documented in the sequence
  diagrams of the SA report.
* This is a teaching artefact: it is not hardened for production traffic (no rate limiting, no
  TLS, no secret management, no multi-tenant isolation).

## 11. License

MIT — see [LICENSE](LICENSE).
