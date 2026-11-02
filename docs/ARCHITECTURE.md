# Architecture of the STC Platform

This document maps the design artefacts of the Software Architecture project onto the running
implementation: C4 levels → packages, Level-3 components → classes, user stories → endpoints and
database tables.

---

## 1. C4 overview of the implemented system

### Level 1 — System context

```
        Team Member            Senior Manager            Overall Lead
             │                       │                        │
             └───────────┬───────────┴────────────┬───────────┘
                         ▼                        ▼
             ┌────────────────────────────────────────────────┐
             │              STC Platform (this repo)          │
             │  task dashboard · collaboration · toolbox ·    │
             │  identity, roles and access control            │
             └───────┬───────────────┬───────────────┬────────┘
                     ▼               ▼               ▼
              Office API (sim)   Git provider (sim)  Container runtime (sim)
              Object storage (sim)                   Large-model API (sim)
```

### Level 2 — Containers

| Container | Implementation | Responsibility |
| --- | --- | --- |
| User Interface | React SPA (`frontend/`) | Kanban board, chat, meetings, toolbox, identity administration |
| Task Dashboard | `com.sstc.platform.dashboard` | Tasks, cycles, OKR, file tracker, performance indicators |
| Communication Service | `com.sstc.platform.collaboration` | Channels, messages, mentions, meetings, notifications, digest |
| Toolbox | `com.sstc.platform.toolbox` | Documents, presentations, Git, containers, project files |
| Identity & Access | `com.sstc.platform.identity` | Accounts, roles, permission policies, sessions, access audit |
| Database | H2 file database | 35 tables, one prefix per sub-system |
| External services | simulated in process | Office API, Git provider, container runtime, storage, LLM |

### Level 3 — Components

| Sub-system | Component (design) | Implementation |
| --- | --- | --- |
| S01 | Account Controller (US01) | `identity/service/IdentityService` + `AccountEventsRepo` |
| S01 | Role Assignment Controller (US02) | `IdentityService#assignRole/revokeRole` + `UserRolesRepo` |
| S01 | Permission Policy Controller (US03) | `IdentityService#setPolicy/revokePolicy` + `PoliciesRepo` |
| S01 | Authorization Audit Controller (US04) | `common/security/AuthFilter` + `TokenService` + `AccessAuditsRepo` |
| S02 | Modification Scheduler (US03) | `TaskDashboardService#createTask/updateTask/moveTask` |
| S02 | Operation Logger (US04) | `TaskDashboardService#log` + `OperationLogsRepo` + `AuditService` |
| S02 | File Query Controller (US01) | `FileTrackerService#query` |
| S02 | File Bucket (US02) | `FileTrackerService#upload` + `FileRecordsRepo` |
| S02 | Historical Version Controller (US01) | `FileTrackerService#addVersion/rollback` |
| S02 | Display Panel (US05) | `TaskDashboardService#board/summary` + `PerformanceService#performance` |
| S02 | Performance Calculator (US06) | `PerformanceService#scoreOf` |
| S03 | Channel & Message Manager (US02) | `MessagingService#listChannels/publish/history` |
| S03 | Mention Detector (US03) | `MessagingService#detectMentions` |
| S03 | Meeting Orchestrator (US01) | `MeetingService#create/changeStatus` |
| S03 | AI Meeting Assistant (US05) | `MeetingService#generateTranscript/summarize` |
| S03 | Notification Dispatcher (US04) | `NotificationService#dispatch/notifyMention` |
| S03 | Smart Digest Engine (US06) | `NotificationService#digest/scoreOf` |
| S04 | Word Document Builder (US01) | `OfficeToolsService#createDocument/updateDocument` |
| S04 | PPT Presentation Builder (US02) | `OfficeToolsService#createPresentation/updateSlide` |
| S04 | Office API Adapter (US01/US02) | `OfficeToolsService#remainingQuota` (quota contract) |
| S04 | Version Control Service (US03) | `DevelopmentToolsService#history/push` |
| S04 | Git Repository Adapter (US03) | `DevelopmentToolsService#createRepo` + `GitReposRepo` |
| S04 | Container Runtime Adapter (US05) | `DevelopmentToolsService#startEnvironment/stopEnvironment/serviceStatus` |
| S04 | File Upload Handler (US04) | `FileManagementService#upload` |
| S04 | Storage Adapter (US04) | `FileManagementService` storage backend + `FilesRepo` |
| S04 | Share Link Service (US04) | `FileManagementService#createShareLink/revokeLink/accessLink` |
| S04 | File Management Controller (US04) | `FileManagementService#fileStatus` + `ToolboxController` |

---

## 2. User story → endpoint → table

### S01 Identity, Role & Access Management

| Story | Endpoint | Table(s) |
| --- | --- | --- |
| US01 accounts | `GET/POST /api/users`, `PUT /api/users/{id}`, `DELETE /api/users/{id}` | `usr_account`, `usr_account_event` |
| US02 roles | `GET /api/roles`, `GET /api/permissions`, `POST/DELETE /api/users/{id}/role` | `usr_role`, `usr_permission`, `usr_role_permission`, `usr_user_role` |
| US03 policies | `GET/POST /api/users/{id}/policies`, `DELETE /api/policies/{id}` | `usr_permission_policy` |
| US04 access | `POST /api/auth/login`, `POST /api/auth/logout`, `GET /api/identity/me`, `GET /api/access-audits` | `usr_session_token`, `usr_access_audit` |

### S02 Task Dashboard

| Story | Endpoint | Table(s) |
| --- | --- | --- |
| US01 file query + versions | `GET /api/files`, `GET /api/files/{id}/versions`, `POST /api/files/{id}/rollback` | `td_file_record`, `td_file_version` |
| US02 upload attachment | `POST /api/files`, `POST /api/files/{id}/versions` | `td_file_record`, `td_file_version` |
| US03 task CRUD | `GET/POST /api/tasks`, `PUT /api/tasks/{id}`, `PATCH /api/tasks/{id}/move`, `DELETE /api/tasks/{id}` | `td_task`, `td_cycle` |
| US04 operation log | `GET /api/operations`, `GET /api/tasks/{id}/history` | `td_operation_log` |
| US05 display panel | `GET /api/tasks/board`, `GET /api/tasks/summary`, `GET /api/okrs` | `td_task`, `td_okr`, `td_key_result` |
| US06 performance | `GET /api/performance`, `POST /api/performance/snapshot` | `td_performance_snapshot` |

### S03 Team Collaboration

| Story | Endpoint | Table(s) |
| --- | --- | --- |
| US01 meetings | `GET/POST /api/meetings`, `PATCH /api/meetings/{id}/status` | `tc_meeting`, `tc_channel_member` |
| US02 group chat | `GET/POST /api/channels`, `GET/POST /api/channels/{id}/messages` | `tc_channel`, `tc_message` |
| US03 mentions | `GET /api/mentions` (produced by the publish path) | `tc_mention` |
| US04 notifications | `GET /api/notifications`, `PATCH /api/notifications/{id}/read`, `POST /api/notifications/read-all` | `tc_notification` |
| US05 AI summary | `GET /api/meetings/{id}/transcript`, `GET/POST /api/meetings/{id}/summary` | `tc_meeting_transcript`, `tc_meeting_summary` |
| US06 smart digest | `GET /api/digest` | `tc_digest_item` |

### S04 Toolbox

| Story | Endpoint | Table(s) |
| --- | --- | --- |
| US01 documents | `GET/POST /api/toolbox/documents`, `POST /api/toolbox/documents/{id}/edits` | `tb_doc_document`, `tb_doc_edit` |
| US02 presentations | `GET/POST /api/toolbox/presentations`, `POST /api/toolbox/presentations/{id}/slides` | `tb_presentation`, `tb_slide` |
| US03 version control | `GET/POST /api/toolbox/repos`, `GET/POST /api/toolbox/repos/{id}/commits` | `tb_git_repo`, `tb_commit_record` |
| US04 project files | `GET/POST /api/toolbox/files`, `POST /api/toolbox/files/{id}/links`, `DELETE /api/toolbox/links/{id}`, `GET /api/toolbox/links/{token}/download` | `tb_toolbox_file`, `tb_share_link` |
| US05 container runtime | `GET/POST /api/toolbox/environments`, `PATCH /api/toolbox/environments/{id}/stop`, `GET /api/toolbox/environments/status` | `tb_dev_environment` |

---

## 3. Architecture execution (sequence diagrams)

The SA module tests an architecture by "executing" it. The runtime behaviour of the platform is
aligned with the six scenarios of the S04 sequence diagrams.

| # | Story | Contract | Execution in the code |
| --- | --- | --- | --- |
| 1 | US01 | `UpdateDocument(documentID, edit)` → `GetQuota(apiName)` → `RunOfficeOperation(edit)` | `OfficeToolsService#updateDocument`: `remainingQuota()` is evaluated **before** the content is mutated; `alt [quota > 0]` applies the edit and returns `0`, otherwise the result code is `-1` and the document is untouched. Every attempt is recorded in `tb_doc_edit`. |
| 2 | US02 | `UpdateSlide(presentationID, slideID, edit)` | same quota guard in `OfficeToolsService#updateSlide`; a `null` slide id appends a new slide. |
| 3 | US03 | `GetHistory` → `Pull` (opt) → `Commit(changes)` → `Push` (opt when commit = 0) | `DevelopmentToolsService#history` then `#push`; `push` returns the created `CommitView` only after the repository sync timestamp is updated. |
| 4 | US04 A | `UploadFile(file, content)` → `WriteFile(file, content)` → `GetFileStatus` (opt) | `FileManagementService#upload` validates internally (name length, size limit, executables), stores through the storage adapter and returns `VALIDATED`; a rejected upload throws a `400` with the symbolic code `-2`. `#fileStatus` serves the confirmation step. |
| 5 | US04 B | `GetFileStatus` → `CreateShareLink(file, expiry)` (opt) → `GetFileStatus` | `FileManagementService#createShareLink` refuses any file whose status is not `VALIDATED`, then `#accessLink` enforces revocation and expiry on the public download path. |
| 6 | US05 | `RouteRequest(request)` → `StartEnvironment(request)` → `GetServiceStatus` → `GetEnvironmentStatus` | `DevelopmentToolsService#startEnvironment` accepts the request (HTTP 200, `int 0` semantics) and returns the container id; `#serviceStatus` resolves the retained service → container association and delegates to `#environmentStatus`. |

Symbolic result codes used consistently across the code and the report: `0` success,
`-1` Office API quota exhausted, `-2` upload validation rejected.

---

## 4. Cross-cutting concerns

| Concern | Implementation | Where |
| --- | --- | --- |
| Authentication | Random session token, `Authorization: Bearer`, 12 h TTL, revocation on sign-out and deactivation | `common/security/TokenService` |
| Authorization | Permission lookup (role permissions ± policy overrides) then a per-request decision, recorded in `usr_access_audit` | `common/security/AuthFilter` |
| Credential storage | BCrypt hashes, never returned by the API | `common/config/WebConfig#passwordEncoder` |
| Audit | `cm_audit_log` written by every sub-system through `AuditService` in its own transaction | `common/audit/AuditService` |
| Error contract | `{status, code, message, timestamp}` for every failure | `common/web/GlobalExceptionHandler` |
| API documentation | Generated from the controllers | springdoc at `/swagger-ui.html` |
| Demo data | Idempotent seeder: 6 accounts, 3 roles, 18 permissions, 14 tasks, 3 OKRs, 4 channels, 2 meetings, 3 repositories, 3 environments | `common/seed/DemoDataSeeder` |

---

## 5. Data model

```
usr_account ──┬─< usr_user_role >── usr_role ──< usr_role_permission >── usr_permission
              ├─< usr_permission_policy
              ├─< usr_account_event
              └─< usr_session_token
usr_access_audit  (one row per protected API call)

td_cycle ──< td_task >── td_okr ──< td_key_result
                │
                ├─< td_file_record ──< td_file_version
                └─< td_operation_log
td_performance_snapshot  (daily indicators)

tc_channel ──< tc_channel_member
           └─< tc_message ──< tc_mention
tc_notification ──< tc_digest_item
tc_meeting ──< tc_meeting_transcript
           └── tc_meeting_summary

tb_git_repo ──< tb_commit_record
tb_doc_document ──< tb_doc_edit
tb_presentation ──< tb_slide
tb_dev_environment
tb_toolbox_file ──< tb_share_link

cm_audit_log  (platform wide trail)
```

The dashboard renders this model live: see the **Architecture map** screen
(`/architecture`), which lists the components, tables and endpoints of every sub-system next to
the running health check.
