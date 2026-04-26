# Architecture

This document defines the architecture and module-boundary baseline for the `moamoa` repository.
Use it as the single reference for architecture-related decisions.

## Overall Structure
- Multi-module Gradle with Kotlin DSL
- Backend: Spring Boot + Kotlin + MVC + JPA
- Frontend: `moamoa-frontend/core-web` (Vite + React), `moamoa-frontend/admin-web` (Vite + React)

## Module Naming
- Use `core-api`, `admin-api`, `core-web`, and `admin-web` as the canonical module names in documents.
- Their actual paths are `moamoa-backend/core/core-api`, `moamoa-backend/admin`, `moamoa-frontend/core-web`, and `moamoa-frontend/admin-web`.
- Older aliases such as `admin`, `moamoa-web`, and `moamoa-admin/admin-web` should be treated only as legacy labels for `admin-api`, `core-web`, and `admin-web`.

## Backend Skill Map
- Shared backend structure, module placement, and dependency direction: `$backend-architecture`
- Controller API contracts, DTOs, and status codes: `$backend-api-contract`
- JPA/JDBC, transaction boundaries, and query layer work: `$backend-persistence-jpa`
- Request boundary logging, traceId, and structured fields: `$backend-logging-strategy`
- Backend test additions and updates: `$backend-testing-style`

## Module Map
- `moamoa-backend:core:core-api`: main public API server
- `moamoa-backend:core:core-batch`: batch processing
- `moamoa-backend:infra:*`: feature-specific `api/impl/starter` modules
- `moamoa-backend:infra:cache-*`: cache contracts, implementations, and starters
- `moamoa-backend:infra:queue-*`: queue contracts, Redis implementations, and starters
- `moamoa-backend:infra:set-*`: set contracts, Redis implementations, and starters
- `moamoa-backend:infra:lock-*`: lock contracts, redisson/local/resilient implementations, and starters
- `moamoa-backend:infra:messaging-*`: messaging contracts, Redis implementations, and starters
- `moamoa-backend:infra:token-*`: token contracts, JWT implementations, and starters
- `moamoa-backend:infra:password-*`: password contracts, crypto implementations, and starters
- `moamoa-backend:infra:mail-*`: mail contracts, Mailgun implementations, and starters
- `moamoa-backend:support:*`: shared libraries such as API docs, logging, templates, tests, and monitoring
- `moamoa-backend:admin`: the `admin-api` executable module

## Package Root Rules
- `moamoa-backend/core/core-api`: `server`
- `moamoa-backend/core/core-batch`: `server.batch`
- `moamoa-backend/admin`: `server.admin`

## Current Backend Baseline
- `core-api` and `admin-api` run on the MVC servlet stack.
- Prefer JPA plus dirty checking for write paths.
- Allow `NamedParameterJdbcTemplate` query layers for complex reads and aggregation.
- Use the blocking Redis stack with `StringRedisTemplate` and `Redisson`.
- Restrict WebClient to outbound integration code and keep server request handling on MVC.

## Dependency Direction Rules
- Put domain and product logic in `core-*`.
- Put external-system integration code in `infra-*`.
- Put shared utilities in `support-*`.
- Do not let infra modules depend back on core modules.
- Only `core-api` and `admin-api` should remain executable JARs. Other modules should stay library JARs.
- Put infra contracts in `moamoa-backend:infra:*‑api`.
- Application modules such as `core-api`, `core-batch`, and `admin-api` should prefer `*-starter` dependencies over direct implementation dependencies.
- When application code uses contract types, prefer starter transitive dependencies declared with `api(...)`. Add direct `*-api` dependencies only when there is a specific reason.
- Keep implementation-selection logic in `*-resilient` modules.
- Keep implementation classes `internal` when possible and compose them from starter auto-configuration.
- Use `AutoConfiguration.imports` for starter auto-configuration wiring.

## core-api Layer Rules
The default feature layout is `feature/<name>/api`, `application`, and `domain`.

- `api`: request and response DTOs, controllers, routing
- `application`: use-case orchestration and transaction boundaries
- `domain`: domain models, rules, and repository interfaces

### command/query Split
- Do not split into `command` and `query` by default.
- Split only when reads become sufficiently complex.
- Treat a query with at least one join as the baseline threshold for that split.

When split:
- Write path: `api -> command`
- Read path: `api -> query`

## Adding New Features
1. Lock the target module first: `core-api`, `admin-api`, `core-web`, or `admin-web`.
2. Inside the target module, follow either `api/application/domain` or `api -> queries -> pages/components`.
3. If external integration is required, add the adapter in the matching `moamoa-backend:infra:*` module or the matching shared frontend layer.
4. Only add Gradle dependencies at the module-boundary level, and do not widen the structure without approval.

## Backend Entry Points
- `moamoa-backend:core:core-api` -> `CoreApiApplication.kt`
- `moamoa-backend:core:core-batch` -> `CoreBatchApplication.kt`
- `moamoa-backend:admin` -> `AdminApiApplication.kt`
