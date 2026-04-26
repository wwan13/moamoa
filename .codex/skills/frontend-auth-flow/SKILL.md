---
name: frontend-auth-flow
description: "Use this skill when changing `src/auth/*` and auth state transitions in moamoa frontend apps. It applies to AuthContext, useAuth, login modals, session synchronization, logout cleanup, and login-required behavior."
---

# Frontend Auth Flow

## Scope
- `moamoa-frontend/core-web/src/auth/*`
- `moamoa-frontend/admin-web/src/auth/*`

## Rules
- Auth state changes must live in the `AuthContext` layer only.
- In `core-web`, preserve the login-modal-first flow.
- In `admin-web`, assume protected routes and a separate login page.
- Do not split auth behavior away from the API client hooks such as `setOnLoginRequired` and `setOnLogout`.

## Verify
1. Does login-required behavior still match the app’s rule, redirect or modal open?
2. Does logout still clean up both local state and server-side cookie/session state correctly?
3. Are pages or components avoiding direct ownership of auth state?
