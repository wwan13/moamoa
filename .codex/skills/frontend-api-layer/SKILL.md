---
name: frontend-api-layer
description: "Use this skill when adding or updating `src/api/*.api.*` files in moamoa frontend apps. It covers domain request functions, request and response DTOs, serialization, and parameter shapes while keeping network details inside the API layer."
---

# Frontend API Layer

## Scope
- `moamoa-frontend/core-web/src/api/*.api.*`
- `moamoa-frontend/admin-web/src/api/*.api.*`

## Rules
- Route all network calls through `src/api/client.*`.
- Keep domain request functions in the API layer and keep UI state logic out of it.
- Keep request and response shapes, query-string composition, and serialization inside the API layer.
- Favor response shapes used by the screen over entity-like naming.

## File Placement
1. Keep domain files named as `<domain>.api.*`
2. Group read, create, update, and delete functions by domain
3. If a helper is needed, first check whether it can stay inside the app’s `src/api` layer

## Do Not
- put React hooks inside API files
- control alerts, toasts, or modal state directly from API files
