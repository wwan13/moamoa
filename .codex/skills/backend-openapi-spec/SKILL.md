---
name: backend-openapi-spec
description: "Use this skill when you need to inspect or extract a filtered OpenAPI spec from moamoa core-api. It applies to path- and method-specific API contract checks, AI-readable Swagger/OAS extraction, and backend tasks that need the current core-api spec without manually browsing Swagger UI."
---

# Backend OpenAPI Spec

Use this skill when you need the current `core-api` OpenAPI contract for a specific path and method.

## Scope
- Target module is `moamoa-backend/core/core-api`.
- Primary entrypoint is the Gradle task `extractOpenApiSpec`.
- This skill is for reading the current spec, not for changing controller contracts by itself.

## Standard Command

Use quiet mode so Gradle logs do not pollute stdout:

```bash
./gradlew -q :moamoa-backend:core:core-api:extractOpenApiSpec -Ppath=/api/post -Pmethod=GET -Pmode=json
```

## Required Inputs
- `-Ppath`: prefix-matched API path such as `/api/post`
- `-Pmethod`: HTTP method such as `GET`, `POST`, `PUT`, `PATCH`, or `DELETE`

## Output Modes
- Default mode is pretty JSON for human inspection.
- Use `-Pmode=json` when another agent step needs raw machine-friendly JSON on stdout.

## Behavior Notes
- The task briefly boots `core-api`, fetches `/v3/api-docs`, filters by path prefix and method, and prunes unused OpenAPI components.
- `/api/post` with `GET` can include multiple GET endpoints under that prefix, such as `/api/post`, `/api/post/bookmarked`, and `/api/post/tech-blog`.
- Prefer the narrowest path prefix that still covers the contract you need.

## Usage Guidance
- For API contract review, extract the exact path and method before reasoning about request and response DTOs.
- For code changes in `core-api`, pair this skill with `$backend-api-contract`.
- For test additions around the same endpoint, pair this skill with `$backend-testing-style`.

## Failure Handling
- If the task fails before returning JSON, inspect the Gradle error and embedded `core-api` logs first.
- If stdout is mixed with unexpected text, rerun with `-q`.
- If the requested prefix returns multiple endpoints, tighten `-Ppath` rather than post-filtering by hand.
