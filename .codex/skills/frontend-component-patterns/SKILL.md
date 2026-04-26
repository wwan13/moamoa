---
name: frontend-component-patterns
description: "Use this skill when changing `src/components/*`, related CSS Modules, or shared layout fragments in moamoa frontend apps. It applies to presentation components, shared UI, layout pieces, CSS Module structure, and props design."
---

# Frontend Component Patterns

## Scope
- `moamoa-frontend/core-web/src/components/*`
- `moamoa-frontend/admin-web/src/components/*`
- related `src/layouts/*`

## Rules
- Keep components presentation-first.
- Keep data fetching and mutation calls in pages or query hooks whenever possible.
- Manage CSS together with the co-located `.module.css` file.
- Split shared UI based on repeated in-app usage, not on guesswork.

## Verify
1. Have props grown so large that the component is taking over page responsibility?
2. Did a style change unintentionally alter shared UI behavior?
3. If a layout component changed, was the route structure reviewed together with it?
