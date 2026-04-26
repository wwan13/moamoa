---
name: frontend-routing-layout
description: "Use this skill when changing `src/routes/*`, layouts, or guarded paths in moamoa frontend apps. It applies to AppRoutes, 404 flow, route metadata, admin login separation, and user layout branching."
---

# Frontend Routing Layout

## Scope
- `moamoa-frontend/core-web/src/routes/*`
- `moamoa-frontend/admin-web/src/routes/*`
- related `src/layouts/*` and `src/components/layout/*`

## App-Specific Rules
- `admin-web`: keep `/login` outside the layout and keep protected routes under `AppLayout`
- `core-web`: preserve user layout branching and any SEO or route-metadata behavior

## Rules
- When adding a route, wire both the route entry and the real page file.
- Keep 404 navigation consistent with `setOnNotFound`.
- If you change menu structure, review sidebar and header navigation together.

## Verify
1. Is every new page actually reachable through the route tree?
2. Do pre-login and post-login access rules still behave as intended?
3. Did any existing menu entry path or breadcrumb-like UI regress?
