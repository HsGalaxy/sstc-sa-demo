# STC Platform · frontend

React 18 + TypeScript single page application for the STC Platform. It talks to the Spring Boot
backend through a single typed client (`src/api/client.ts`), which is the source of truth for the
API contract.

## Commands

```bash
npm install
npm run dev        # http://localhost:5173, /api proxied to http://localhost:8080
npm run build      # type check + production build into ../backend/src/main/resources/static
npm run preview    # serve the production build on http://localhost:4173
```

The production build is emitted directly into the backend static resources, so the Spring Boot jar
serves the dashboard from the same origin as the API (no CORS configuration needed in production).

## Structure

```
src/
├── api/client.ts        typed fetch client: every endpoint, every view type, ApiError, token storage
├── app/
│   ├── AuthContext.tsx  session state, sign-in/out, permission checks (can(permission))
│   └── ToastContext.tsx transient success/error messages
├── components/
│   ├── AppShell.tsx     sidebar navigation (grouped by sub-system), top bar, unread badge
│   └── ui.tsx           Card, Stat, Badge, Button, Field, Avatar, Modal, Bar, Sparkline, formatters
├── pages/
│   ├── LoginPage.tsx            sign-in with the seeded demo accounts
│   ├── OverviewPage.tsx         aggregated landing page
│   ├── ArchitecturePage.tsx     C4 → code map, endpoints, tables, sequence scenarios
│   ├── BoardPage.tsx            S02 Kanban board (drag and drop)
│   ├── FileTrackerPage.tsx      S02 file query, versions, rollback
│   ├── OkrPage.tsx              S02 OKR, indicators, performance scores, bottlenecks
│   ├── OperationLogPage.tsx     S02 operation log
│   ├── CollaborationPage.tsx    S03 channels, chat, mentions
│   ├── MeetingsPage.tsx         S03 meeting lifecycle, transcript, AI summary
│   ├── InboxPage.tsx            S03 notifications and smart digest
│   ├── identity/                S01 accounts, policies, access audit
│   └── toolbox/                 S04 documents, presentations, repos, environments, project files
└── styles.css           design system: tokens, layout, cards, tables, kanban, chat, login
```

## Conventions

* **No UI framework.** Layout, tables, cards, charts and the Kanban board are plain CSS plus a
  small component kit, so nothing obscures the architecture being demonstrated.
* **Typed contract.** Every response shape is declared in `api/client.ts`; pages never build URLs
  by hand.
* **Permission aware.** Write actions are gated with `can('S0x....')` from the auth context, which
  mirrors the permission codes enforced by the backend filter.
* **Explicit provenance.** Each page header names the user story and the Level-3 component it
  exercises, so a reviewer can trace behaviour back to the design.
* **Failure handling.** Every API call is wrapped in try/catch and reports through the toast
  provider; a `401` clears the stored session and returns the user to the sign-in screen.

## Session storage

| Key | Content |
| --- | --- |
| `stc.token` | Bearer token returned by `POST /api/auth/login` |
| `stc.user` | Cached account projection (id, name, role, permissions) for instant rendering |

Signing out calls `POST /api/auth/logout` (revoking the token server side) and clears both keys.
