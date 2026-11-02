# STC Platform REST API

Base URL: `http://localhost:8080` · interactive documentation: `/swagger-ui.html` ·
machine readable spec: `/v3/api-docs`.

## Authentication

Every `/api/**` call except the public endpoints below requires a bearer token obtained from
`POST /api/auth/login`:

```bash
TOKEN=$(curl -s -X POST http://localhost:8080/api/auth/login \
  -H 'Content-Type: application/json' \
  -d '{"username":"lead","password":"Lead@123"}' | jq -r .token)

curl -s http://localhost:8080/api/tasks/board -H "Authorization: Bearer $TOKEN"
```

| Header | Value |
| --- | --- |
| `Authorization` | `Bearer <token>` (also accepted: `X-STC-Token: <token>`) |

Public endpoints: `/api/health`, `/api/meta`, `/api/auth/login`, `/api/auth/register`,
`/api/toolbox/links/{token}/download`, `/swagger-ui/**`, `/v3/api-docs/**`, `/h2-console/**`.

A missing, expired or revoked token returns `401` with the platform error contract.
Signing out (`POST /api/auth/logout`) revokes the session; deactivating an account revokes all of
its sessions.

## Error contract

```json
{
  "status": 404,
  "code": "NOT_FOUND",
  "message": "Task 999 not found",
  "timestamp": "2026-11-02T23:40:12.884"
}
```

| Status | `code` | Typical cause |
| --- | --- | --- |
| 400 | `BAD_REQUEST` / `VALIDATION_ERROR` | Failed business rule or bean validation |
| 401 | `UNAUTHORIZED` | Missing or invalid session token |
| 403 | `FORBIDDEN` | Account not active, or the resource refuses the operation |
| 404 | `NOT_FOUND` | Unknown identifier |
| 409 | `CONFLICT` | Duplicate username/email, container runtime limit reached |
| 500 | `INTERNAL_ERROR` | Unexpected failure (logged with the stack trace) |

---

## S01 · Identity, Role & Access Management

| Method | Path | Story | Description |
| --- | --- | --- | --- |
| POST | `/api/auth/login` | US04 | Verify credentials, open a session, return the token and the effective permissions |
| POST | `/api/auth/logout` | US04 | Revoke the presented session |
| GET | `/api/identity/me` | US04 | Current account with its effective permission set |
| GET | `/api/identity/permissions` | US04 | Role plus permission codes of the caller |
| GET | `/api/identity/check?permission=` | US04 | Authorization decision for one permission |
| GET | `/api/users` | US01 | List accounts |
| GET | `/api/users/{id}` | US01 | One account |
| POST | `/api/users` | US01 | Create an account (`username`, `displayName`, `email`, `password`, `jobTitle`, `role`) |
| PUT | `/api/users/{id}` | US01 | Update profile, role or status |
| DELETE | `/api/users/{id}` | US01 | Deactivate an account and revoke its sessions |
| POST | `/api/users/{id}/password` | US01 | Change a password (requires the current one) |
| GET | `/api/roles` | US02 | Role matrix with bound permissions and member counts |
| GET | `/api/permissions` | US02 | Permission catalogue |
| POST | `/api/users/{id}/role` | US02 | Assign a role (`roleCode`, `adminPrivilege`) |
| DELETE | `/api/users/{id}/role` | US02 | Revoke all role assignments |
| GET | `/api/users/{id}/policies` | US03 | Per-account permission overrides |
| POST | `/api/users/{id}/policies` | US03 | Apply an override (`permissionCode`, `effect` = GRANT/REVOKE, `reason`) |
| DELETE | `/api/policies/{policyId}` | US03 | Remove an override |
| GET | `/api/account-events?limit=` | US01 | Account lifecycle events |
| GET | `/api/access-audits?limit=` | US04 | Authorization decisions (permission, resource, decision, latency) |

**Effective permission resolution**: `permissions(role) + GRANT policies − REVOKE policies`.
`OVERALL_LEAD` implicitly passes every check.

## S02 · Task Dashboard

| Method | Path | Story | Description |
| --- | --- | --- | --- |
| GET | `/api/tasks/board` | US05 | Kanban columns with decorated task cards |
| GET | `/api/tasks/summary` | US05 | Counters (total, done, in progress, review, overdue, completion rate, logged hours) |
| GET | `/api/tasks?status=&assigneeId=&cycleId=&keyword=` | US03 | Filtered task list |
| GET | `/api/tasks/{id}` | US03 | One task with its attachments count |
| POST | `/api/tasks` | US03 | Create a task (`title`, `status`, `priority`, `assigneeId`, `cycleId`, `okrId`, `dueDate`, `estimateHours`, `tags`) |
| PUT | `/api/tasks/{id}` | US03 | Update any mutable field (including `loggedHours`, `blocked`) |
| PATCH | `/api/tasks/{id}/move?status=&position=` | US03 | Move a card between columns |
| DELETE | `/api/tasks/{id}` | US03 | Delete a task |
| GET | `/api/tasks/{id}/history` | US04 | Operation log of one task |
| GET | `/api/cycles` | US05 | Delivery cycles with progress |
| POST | `/api/cycles` | US05 | Create a cycle |
| GET | `/api/okrs` | US05 | Objectives with their key results |
| POST | `/api/okrs` | US05 | Create an objective |
| POST | `/api/okrs/{id}/key-results` | US05 | Add a key result and recompute the objective |
| PATCH | `/api/key-results/{id}?currentValue=` | US05 | Update a key result |
| GET | `/api/performance` | US06 | Trend, member scores, bottlenecks, team indicators |
| POST | `/api/performance/snapshot` | US05 | Capture a snapshot immediately |
| GET | `/api/files?bucket=&taskId=&ownerId=&archived=&keyword=` | US01 | Multi-condition file query |
| GET | `/api/files/{id}` | US01 | One index entry |
| GET | `/api/files/{id}/versions` | US01 | Version history |
| POST | `/api/files` | US02 | Register an attachment (`name`, `taskId`, `bucket`, `contentType`, `sizeBytes`, `changeNote`) |
| POST | `/api/files/{id}/versions` | US01 | Add a version |
| POST | `/api/files/{id}/rollback?version=` | US01 | Roll back to a version |
| PATCH | `/api/files/{id}/archive?archived=` | US01 | Archive or restore |
| GET | `/api/files/statistics` | US01 | Count and size per bucket |
| GET | `/api/operations?limit=&action=&actorId=` | US04 | Operation log |

**Performance model**: `totalScore = 0.4 × onTimeRate + 0.3 × qualityScore + 0.3 × okrScore`,
graded `A ≥ 85`, `B ≥ 70`, `C ≥ 55`, otherwise `D`.

## S03 · Team Collaboration

| Method | Path | Story | Description |
| --- | --- | --- | --- |
| GET | `/api/channels` | US02 | Channels with member/message counts and last activity |
| POST | `/api/channels` | US02 | Create a channel |
| POST | `/api/channels/{id}/join` | US02 | Join a channel |
| GET | `/api/channels/{id}/messages?limit=` | US02 | Message history |
| POST | `/api/channels/{id}/messages` | US02/US03 | Publish a message; `@username` tokens raise targeted notifications |
| GET | `/api/messages/recent` | US02 | Latest messages across all channels |
| GET | `/api/mentions` | US03 | Mentions targeting the caller |
| GET | `/api/notifications` | US04 | Notification inbox of the caller |
| GET | `/api/notifications/unread-count` | US04 | Badge counter |
| PATCH | `/api/notifications/{id}/read` | US04 | Mark one notification read |
| POST | `/api/notifications/read-all` | US04 | Mark everything read |
| GET | `/api/digest` | US06 | Ranked digest of unread items |
| GET | `/api/meetings` | US01 | Meetings with host, status, join code and transcript size |
| GET | `/api/meetings/{id}` | US01 | One meeting |
| POST | `/api/meetings` | US01 | Schedule a meeting (`title`, `agenda`, `channelId`, `startAt`, `participantIds`) |
| PATCH | `/api/meetings/{id}/status?status=` | US01/US05 | `SCHEDULED`, `IN_PROGRESS`, `ENDED`, `CANCELLED`; `ENDED` triggers transcription and summarisation |
| GET | `/api/meetings/{id}/transcript` | US05 | Transcribed segments with speaker and offset |
| GET | `/api/meetings/{id}/summary` | US05 | Existing AI summary |
| POST | `/api/meetings/{id}/summary` | US05 | Generate or regenerate the AI summary |

**Digest ranking**: `score = base(kind) + 10 × recency`, where `base` is 90 for `MENTION`,
70 for `TASK`, 60 for `MEETING`, 50 for `FILE`, 30 for `SYSTEM`, and recency decays linearly over
24 hours. Mentions therefore always rank first.

## S04 · Toolbox

| Method | Path | Story | Description |
| --- | --- | --- | --- |
| GET | `/api/toolbox/documents` | US01 | Documents |
| GET | `/api/toolbox/documents/{id}` | US01 | One document |
| POST | `/api/toolbox/documents` | US01 | Create a document (`title`, `content`, `template`) |
| POST | `/api/toolbox/documents/{id}/edits` | US01 | Apply an edit (`operation` = APPEND/REPLACE/RENAME, `payload`); quota checked first |
| GET | `/api/toolbox/documents/{id}/edits` | US01 | Edit history with result codes and remaining quota |
| GET | `/api/toolbox/presentations` | US02 | Slide decks with their slides |
| GET | `/api/toolbox/presentations/{id}` | US02 | One deck |
| POST | `/api/toolbox/presentations` | US02 | Create a deck |
| POST | `/api/toolbox/presentations/{id}/slides?slideId=` | US02 | Update a slide, or append when `slideId` is omitted |
| GET | `/api/toolbox/repos` | US03 | Repositories with commit counts |
| GET | `/api/toolbox/repos/{id}` | US03 | One repository |
| POST | `/api/toolbox/repos` | US03 | Register a repository |
| GET | `/api/toolbox/repos/{id}/commits` | US03 | Commit history |
| POST | `/api/toolbox/repos/{id}/commits` | US03 | Publish a change (`message`, `branch`) — `Commit` + `Push` |
| GET | `/api/toolbox/commits/recent` | US03 | Latest commits across repositories |
| GET | `/api/toolbox/environments` | US05 | Container environments with state and uptime |
| POST | `/api/toolbox/environments` | US05 | Start an environment (`serviceId`, `image`, `cpuLimit`, `memoryMb`, `ports`) |
| PATCH | `/api/toolbox/environments/{containerId}/stop` | US05 | Stop an environment |
| GET | `/api/toolbox/environments/status?serviceId=&containerId=` | US05 | Status lookup (service → container delegation) |
| POST | `/api/toolbox/environments/heartbeat` | US05 | Refresh the heartbeat of running environments |
| GET | `/api/toolbox/files` | US04 | Stored project files |
| GET | `/api/toolbox/files/{id}` | US04 | One file |
| POST | `/api/toolbox/files` | US04 | Upload (`name`, `folder`, `contentType`, `sizeBytes`); validation is internal |
| GET | `/api/toolbox/files/{id}/status` | US04 | `GetFileStatus(file)` |
| POST | `/api/toolbox/files/{id}/links` | US04 | Create a share link (`expiresInDays` or `expiresAt`) |
| GET | `/api/toolbox/files/{id}/links` | US04 | Links of one file |
| GET | `/api/toolbox/links` | US04 | All share links |
| DELETE | `/api/toolbox/links/{id}` | US04 | Revoke a link |
| GET | `/api/toolbox/links/{token}/download` | US04 | Public download of a valid link (no session required) |
| GET | `/api/toolbox/storage` | US04 | Storage statistics |
| GET | `/api/toolbox/integrations` | US04 | Simulated integration status (Office quota, Git, runtime, storage) |

**Upload validation rules**: file name ≤ 200 characters, size ≤ `sstc.toolbox.storage.max-upload-mb`
(25 MB by default), and no `.exe`, `.bat` or `.cmd` files. A rejected upload returns `400` with the
symbolic result code `-2`.

## Platform

| Method | Path | Description |
| --- | --- | --- |
| GET | `/api/health` | Liveness probe with the server time |
| GET | `/api/meta` | Case study, actor taxonomy, sub-system catalogue, stack |
| GET | `/api/audits?limit=&module=&action=&actorId=` | Unified audit trail across S01–S04 |

---

## Smoke test

`scripts/smoke-test.ps1` exercises 36 checks end to end (happy paths plus the negative cases):
authentication, the auth guard, task CRUD and moves, the file tracker and version history,
mention detection, notifications and digest, the meeting transcript and AI summary, document
editing with the quota check, repository publishing, environment start and status lookup, project
file upload and share links.

```powershell
powershell -ExecutionPolicy Bypass -File scripts/smoke-test.ps1
```
