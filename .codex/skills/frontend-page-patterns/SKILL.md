---
name: frontend-page-patterns
description: "Use this skill when changing `src/pages/*` in moamoa frontend apps. It applies to page-level state, event handlers, query hook composition, form submission flow, and list filter/search behavior."
---

# Frontend Page Patterns

## Scope
- `moamoa-frontend/core-web/src/pages/*`
- `moamoa-frontend/admin-web/src/pages/*`

## Rules
- Pages own screen state and event composition.
- Use the API and query layers instead of raw fetch calls.
- Prefer composing existing hooks and components over recreating domain logic inside pages.
- Keep page-only UI local, and lift it to `components` only when reuse becomes real.

## Verify
1. Did data-fetching logic leak into the page layer?
2. Does form submit behavior still follow the app’s alert, toast, and redirect conventions?
3. Do search, filter, and paging state still align with query-key composition?
