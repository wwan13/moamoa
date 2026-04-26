---
name: backend-architecture
description: "Use this skill for shared backend structure decisions in moamoa. It applies when work spans core-api, admin-api, core-batch, or infra/support boundaries and you need to decide feature placement, module ownership, dependency direction, or starter composition."
---

# Backend Architecture

Use these documents as the single source of truth for backend architecture work.

- Architecture guide: `.codex/agents/guides/architecture.md`
- Backend convention guide: `.codex/agents/guides/backend-convention.md`
- Module boundary guide: `.codex/agents/guides/module-boundary.md`

## Companion Skills
- API contracts, DTOs, and status codes: `$backend-api-contract`
- JPA/JDBC, transactions, and query layer work: `$backend-persistence-jpa`
- Logging, traceId, and boundary filters: `$backend-logging-strategy`
- Test coverage and verification: `$backend-testing-style`

## Current Backend Baseline
- `core-api` and `admin-api` use the MVC servlet stack with JPA.
- Do not introduce WebFlux server patterns such as reactive repositories or reactive security/filter chains.
- Redis integrations use the blocking stack (`StringRedisTemplate`, `Redisson`).
- WebClient is acceptable in outbound integration code, but the server layer remains MVC.
- Prefer entity mutation plus dirty checking over JPQL bulk update paths.
- In query services, default to sequential DB access unless there is a strong reason to do otherwise.

## Coroutines And Function Style
- `suspend` is acceptable in tech-blog and outbound integration paths where the codebase already uses it.
- Most application and controller code remains regular synchronous functions.
- Only use `runBlocking` as an explicit bridge at a well-defined boundary.

## Reactive Restrictions
- Outside outbound WebClient integration code, do not introduce reactive types.
- Do not apply WebFlux server patterns to MVC controllers, filters, or security configuration.
- Do not introduce `Mono`, `Flux`, or reactive repositories in JPA or Redis areas.
- Avoid casual `block()` usage.

## Transactions And Outbox
- The current codebase primarily uses Spring `@Transactional` on application and query services.
- Event publisher components save outbox records inside an existing transaction and commonly enforce `Propagation.MANDATORY`.
- Keep domain changes and outbox persistence in the same transaction boundary.

## Use This Skill When
- Adding or reshaping backend modules
- Changing dependency direction between modules
- Deciding whether code belongs in `core`, `infra`, `support`, or `admin`
- Deciding layer structure such as `api/application/domain` and optional `query`
- Extending the split between `*-api`, implementation modules, and `*-starter`
- A backend task touches multiple layers and needs one coordinating skill

## Workflow
1. Read the architecture and module-boundary guides, then lock the target module.
2. Decide feature placement and check whether the work spills outside the target module.
3. If cross-module edits or dependency changes are required, stop and get approval first.
4. Implement the smallest change that respects the approved boundary.
5. Verify dependency direction, layering, and module boundaries before finishing.

## Infra And Starter Rules
- Put infra contracts such as interfaces, shared types, and annotations in `moamoa-backend:infra:*‑api`.
- Put implementations in the feature-specific implementation modules such as `*-redis`, `*-caffeine`, or `*-resilient`.
- Use `*-starter` modules to bind contracts and implementations through auto-configuration.
- Keep implementation classes `internal` unless there is a strong reason not to.
- Application modules such as `core-api`, `core-batch`, and `admin-api` should prefer starter dependencies over direct implementation dependencies.
