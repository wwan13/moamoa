# Backend Convention

This document defines the shared implementation baseline for backend work in `moamoa-backend`.

## Scope
- Applies to `moamoa-backend/core/core-api`
- Applies to `moamoa-backend/admin`
- Applies to `moamoa-backend/core/core-batch` when the same stack conventions matter
- Priority: this document > backend skill documents for shared implementation rules

## Core Runtime Rules
- Keep request handling on the MVC servlet stack.
- Do not introduce WebFlux server patterns into controllers, filters, or security configuration.
- Use regular synchronous functions by default in controller and application layers.
- Allow `suspend` only where the codebase already uses it for outbound integration or tech-blog collection flows.

## Service And Layering Rules
- In `core-api`, prefer `feature/<name>/api`, `application`, and `domain` as the default feature layout.
- Add a `query` layer only when reads are complex enough to justify it.
- Keep controllers thin. Request parsing, orchestration, and transaction boundaries belong in application or query services.
- Keep repository interfaces in the domain-facing layer and concrete external integrations in infra-facing layers.

## Persistence Rules
- Prefer Spring Data JPA plus dirty checking for write paths.
- Use `NamedParameterJdbcTemplate` for complex reads, reporting, or aggregation where JPA is awkward.
- Prefer entity mutation methods over JPQL bulk updates unless there is a clear reason not to.
- Prefer `findByIdOrNull` where it matches the existing repository style.

## Transaction Rules
- Use Spring `@Transactional` as the default transaction boundary mechanism.
- Use `readOnly = true` for read services where appropriate.
- Keep domain writes and outbox persistence in the same transaction boundary.
- Event publisher components that persist outbox rows may require an existing transaction with `Propagation.MANDATORY`.

## Logging Rules
- Use the shared typed logging helpers from `support-logging`.
- Preserve traceId propagation through `RequestLogContextHolder`.
- Use request boundary filters for request-level logs instead of ad hoc controller logging.
- Avoid duplicate error logging when the same error is already normalized in controller advice.

## Redis And External I/O Rules
- Use the blocking Redis stack already present in the repository, such as `StringRedisTemplate` and `Redisson`.
- Restrict WebClient usage to outbound integration paths.
- Do not introduce reactive repositories or reactive Redis access into the main backend stack.

## Testing Rules
- Prefer unit tests for application and domain logic.
- Use integration tests when Spring wiring, persistence behavior, or web behavior must be verified together.
- Use `runTest` only for code paths that are actually coroutine-based.

## Do Not
- Do not widen module boundaries without approval.
- Do not add new dependencies without approval.
- Do not mix unrelated structural cleanup into feature work.
- Do not add reactive server patterns into this backend stack.
