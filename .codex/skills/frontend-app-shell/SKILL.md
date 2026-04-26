---
name: frontend-app-shell
description: "Use this skill when changing `src/app/App.*` and top-level UI event wiring in moamoa frontend apps. It covers global alert/confirm/toast rendering, notFound handling, login modal wiring, and search modal wiring."
---

# Frontend App Shell

## Scope
- `moamoa-frontend/core-web/src/app/App.tsx`
- `moamoa-frontend/admin-web/src/app/App.tsx`

## This Skill Owns
- where global modals and toasts are rendered
- bindings for `setOnGlobalAlert`, `setOnGlobalConfirm`, `setOnToast`, and `setOnNotFound`
- app-level UI state composition at startup
- app-shell event wiring such as login modal and search modal flow in `core-web`

## Rules
- Wire global UI once, in `App`.
- Do not pull page-local feedback state into the app shell.
- Keep the client’s global handlers separate from the UI render responsibility.

## Verify
1. Is any event being registered more than once?
2. Are close-time callbacks and cleanup still preserved for modals and toasts?
3. Does the global UI remain stable across route transitions?
