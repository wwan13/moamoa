---
name: frontend-http-client
description: "Use this skill when changing `src/api/client.*` and shared HTTP behavior in moamoa frontend apps. It covers token refresh, login-required handling, global alert/confirm/toast/notFound wiring, and ApiError behavior."
---

# Frontend HTTP Client

## Scope
- `moamoa-frontend/core-web/src/api/client.ts`
- `moamoa-frontend/admin-web/src/api/client.ts`

## This Skill Owns
- the shared request wrapper such as `apiRequest`
- `ApiError` and safe JSON parsing
- token refresh and retry behavior
- `setOnLoginRequired`, `setOnLogout`, `setOnServerError`, `setOnNotFound`
- `showGlobalAlert`, `showGlobalConfirm`, `showToast`

## Rules
- Do not spread raw `fetch` usage into pages and components.
- Do not bypass auth failure, token refresh failure, or forced logout flow.
- Route user feedback through the global handlers exposed by the client layer.
- Preserve app-specific endpoint prefix differences between `core-web` and `admin-web`.

## Verify
1. Do status-code-specific UI reactions still behave the same?
2. Is duplicate refresh request protection still intact?
3. Does login-required and logout cleanup still match the existing flow?
