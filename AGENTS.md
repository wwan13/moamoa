# moamoa — Agent Operating Guide

## Overview

`moamoa` is a multi-module Gradle repository with Spring Boot + Kotlin backend services and two Vite + React frontend applications.

- Backend runtime style: MVC servlet stack, JPA for writes, JDBC query layers for complex reads
- Frontend apps: `core-web` and `admin-web`
- Main editable target modules:
  - `moamoa-backend/core/core-api`
  - `moamoa-backend/admin`
  - `moamoa-frontend/core-web`
  - `moamoa-frontend/admin-web`
- Agent/docs-only target:
  - `AGENTS.md`
  - `.codex/**`

Use this file as the top-level operating hub.
Detailed rules live in:
- `.codex/agents/guides/*.md`
- `.codex/agents/profiles/*.md`
- `.codex/skills/*/SKILL.md`

## Structure

```text
moamoa/
├── AGENTS.md
├── settings.gradle.kts
├── moamoa-backend/
│   ├── admin/                    # admin-api executable module
│   ├── core/
│   │   ├── core-api/             # main public API
│   │   ├── core-batch/           # batch module
│   │   └── core-enum/
│   ├── infra/                    # cache/queue/set/lock/messaging/token/password/mail/tech-blog modules
│   └── support/                  # logging, api docs, templates, monitoring, test helpers, webhook
└── moamoa-frontend/
    ├── core-web/                 # user-facing web app
    └── admin-web/                # admin web app
```

## Where To Look

| Task | Primary Location | Notes |
|------|------------------|-------|
| Public API feature | `moamoa-backend/core/core-api/src/main/kotlin/server/core/feature/` | Default layout is `api/application/domain`, with optional `query` for complex reads |
| Admin API feature | `moamoa-backend/admin/src/main/kotlin/server/admin/feature/` | Admin-side controllers and application logic |
| Public API entry | `moamoa-backend/core/core-api/src/main/kotlin/server/core/CoreApiApplication.kt` | Main executable entry |
| Admin API entry | `moamoa-backend/admin/src/main/kotlin/server/admin/AdminApiApplication.kt` | Admin executable entry |
| Batch entry | `moamoa-backend/core/core-batch/src/main/kotlin/server/batch/CoreBatchApplication.kt` | Batch module entry |
| Outbox/event persistence | `moamoa-backend/core/core-api/src/main/kotlin/server/core/infra/outbox/` | Outbox save and dispatch flow |
| Shared logging | `moamoa-backend/support/support-logging/` | JSON logback config, typed log helpers, traceId context |
| Public API request logging | `moamoa-backend/core/core-api/src/main/kotlin/server/core/global/logging/` | Request boundary filters and repository logging |
| Admin request logging | `moamoa-backend/admin/src/main/kotlin/server/admin/global/logging/` | Admin request boundary filter |
| Frontend app shell | `moamoa-frontend/*/src/app/App.*` | Global modal/toast/confirm/notFound wiring |
| Frontend HTTP client | `moamoa-frontend/*/src/api/client.*` | Shared API wrapper, token refresh, error routing |
| Frontend auth flow | `moamoa-frontend/*/src/auth/*` | `AuthContext`, login-required flow, session handling |
| Frontend routing | `moamoa-frontend/*/src/routes/*` | Route tree, guards, route metadata |
| Frontend page logic | `moamoa-frontend/*/src/pages/*` | Page-level state and event composition |
| Frontend UI components | `moamoa-frontend/*/src/components/*` | Presentation components and CSS Modules |
| Agent/docs rules | `.codex/` | Guides, profiles, and skills only |

## Guides

- Architecture: `.codex/agents/guides/architecture.md`
- Backend rules: `.codex/agents/guides/backend-convention.md`
- Module boundary rules: `.codex/agents/guides/module-boundary.md`
- Frontend rules: `.codex/agents/guides/frontend-convention.md`

## Agent Profiles

- core-api: `.codex/agents/profiles/core-api-agent.md`
- admin-api: `.codex/agents/profiles/admin-api-agent.md`
- core-web: `.codex/agents/profiles/moamoa-web-agent.md`
- admin-web: `.codex/agents/profiles/admin-web-agent.md`
- agent-config: `.codex/agents/profiles/agent-config-agent.md`

## Skill Map

### Backend

- Shared backend structure, module placement, and dependency direction: `$backend-architecture`
- Backend API contract changes: `$backend-api-contract`
- Backend JPA/JDBC and transaction boundaries: `$backend-persistence-jpa`
- Backend logging, traceId, and request boundary logging: `$backend-logging-strategy`
- Backend test additions and updates: `$backend-testing-style`
- Backend image build, push, and remote compose deployment: `$backend-deployment`

### Frontend

- Frontend API functions and DTOs: `$frontend-api-layer`
- Frontend HTTP client, error handling, and token refresh: `$frontend-http-client`
- Frontend auth state and `AuthContext`: `$frontend-auth-flow`
- Frontend routing, layout, and route guards: `$frontend-routing-layout`
- Frontend React Query and cache invalidation: `$frontend-query-layer`
- Frontend app shell and global modal/toast/confirm wiring: `$frontend-app-shell`
- Frontend page-level state composition: `$frontend-page-patterns`
- Frontend components, CSS Modules, and UI composition: `$frontend-component-patterns`

### Repo Operations

- Commit message and commit-splitting guidance: `$commit-style`

## Constraints

- Lock exactly one target module before editing.
- Do not edit outside the locked target module.
- `core-api` and `admin-api` must not modify each other.
- `core-web` and `admin-web` must not modify each other.
- If a task requires dependency changes, stop and get approval first.
- Documentation operations are the only exception:
  - lock `agent-config`
  - only edit `AGENTS.md` and `.codex/**`

## Conventions

- Frontend:
  - Preserve the `api -> queries -> pages/components` layering
  - Route all API calls through `src/api/client.*`
  - Keep auth state changes inside `AuthContext`
  - Use global feedback utilities instead of ad hoc alert flows
- Repository:
  - Prefer the smallest change that solves the requested problem
  - Do not perform unrelated refactors
  - Do not widen module boundaries without approval

## Commands

### Backend

```bash
./gradlew :moamoa-backend:core:core-api:test
./gradlew :moamoa-backend:admin:test
./gradlew :moamoa-backend:core:core-batch:test
./gradlew :moamoa-backend:core:core-api:bootJar
./gradlew :moamoa-backend:admin:bootJar
```

### Frontend

```bash
cd moamoa-frontend/core-web && npm run dev
cd moamoa-frontend/core-web && npm run build
cd moamoa-frontend/admin-web && npm run dev
cd moamoa-frontend/admin-web && npm run build
```

## Anti-Patterns

- Do not change multiple target modules in one task unless the user explicitly approves task splitting.
- Do not add dependencies silently.
- Do not push cross-layer logic into the wrong layer just to “make it work”.
- Do not bypass token refresh or login-required flows in frontend code.
- Do not introduce reactive server patterns into the MVC backend stack.
- Do not treat `.codex` docs as application source-code work; keep them under `agent-config`.
