---
name: backend-persistence-jpa
description: "Use this skill for backend persistence work in moamoa. It covers JPA entity writes, JDBC-based query reads, transaction boundaries, outbox persistence, and repository patterns in the DB access layer."
---

# Backend Persistence (JPA/JDBC)

The current persistence baseline is JPA for writes and JDBC for complex reads.

## Current Baseline
- Use Spring Data JPA plus dirty checking for write paths.
- Use `NamedParameterJdbcTemplate` SQL query layers for complex reads and aggregation.
- Do not introduce reactive repository or R2DBC `DatabaseClient` patterns for new work.

## Entities And Repositories
- Prefer regular classes over `data class` for entities.
- Keep mutable fields as `var private set` and update them through domain methods such as `update...`.
- Keep immutable fields as `val`.
- Prefer `findByIdOrNull` where it fits the repository style.

## Transactions
- The current codebase mainly uses Spring `@Transactional`.
- Keep writes and outbox event persistence inside the same transaction.
- Event publisher components often require an existing transaction through `Propagation.MANDATORY`.
- If propagation must be explicit, declare it with `@Transactional(propagation = ...)`.

## SQL And Query Notes
- In query layers, default to sequential multi-DB access.
- Keep column aliases exactly aligned with mapper field names.

## Do Not
- Mix reactive repositories with JPA in the same server path.
- Introduce new `Mono` or `Flux` based DB access in the server layer.
