---
name: backend-testing-style
description: "Use this skill for backend tests in moamoa. It applies to core-api, admin-api, and core-batch test additions and updates across MVC controllers, application services, and JPA/JDBC query verification."
---

# Backend Testing Style

This is the baseline for `core-api`, `admin-api`, and `core-batch` tests.

## Tooling
- JUnit 5 (`spring-boot-starter-test`)
- MockK
- Use Kotest assertions
- Use `runTest` only where the code path is actually coroutine-based

## Test Types
- `UnitTest`: pure unit tests without Spring context
- `IntegrationTest`: integration tests using `@SpringBootTest`
- Persistence tests follow the current JPA/JDBC stack

## Scope And Priority
- MVC controllers: prioritize HTTP behavior, validation, security, and error mapping
- Services: verify state changes, collaborator calls, and outbox registration where relevant
- Query and SQL paths: verify aliases, nullable handling, paging, and condition composition

## Structure
- Mirror the production package structure under `src/test/kotlin`
- Keep one scenario per test using a clear given/when/then shape
- Preserve the current Korean backtick-style test naming convention

## Current Stack Notes
- Avoid WebFlux-specific test tools such as `WebTestClient` unless the target path is truly reactive, which is uncommon in this codebase.
- Keep mocking style aligned with the current `findByIdOrNull` usage.
- Use suspend-style tests only for code paths that are actually suspend-based, such as tech-blog or outbound integration flows.
