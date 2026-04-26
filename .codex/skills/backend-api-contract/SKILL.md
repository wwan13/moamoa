---
name: backend-api-contract
description: "Use this skill when adding or changing backend HTTP API contracts in moamoa. It applies to controller request and response shapes, validation rules, headers, status codes, and error response rules in core-api and admin-api."
---

# Backend API Contract

This is the API contract baseline for `core-api` and `admin-api`.

## Endpoint Rules
- The default path shape is `/api/<feature>`.
- Use `@RestController` with `@RequestMapping`.
- Return either `ResponseEntity` or a DTO directly.

## Requests And Responses
- Request bodies should use `@RequestBody @Valid` DTOs.
- Responses should use application or query DTOs. Do not expose entities directly.

## Authentication
- Use `@RequestPassport` where authenticated member context is required.
- Refresh token flows use the `X-Refresh-Token` header.

## Error Responses
- The shared error shape is `{ status: Int, message: String }`.
- Follow the mapping rules implemented by `ApiControllerAdvice` and `AdminApiControllerAdvice`.

## Status Codes
- 400: validation or binding error
- 401/403: authentication or authorization error
- 500: server error

## Current Implementation Notes
- `core-api` and `admin-api` run on the MVC servlet stack.
- Controllers are usually regular synchronous functions.
- If an outbound integration path returns a `suspend` result, bridge it explicitly at the correct upper boundary.
