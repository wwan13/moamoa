# Frontend Convention

This document defines the shared frontend baseline for `moamoa-frontend`.

## Scope
- Applies to `moamoa-frontend/core-web` and `moamoa-frontend/admin-web`
- Priority: this document > detailed frontend skill documents

## Required Rules
1. Maintain type safety and make important values, functions, and API inputs/outputs explicit.
2. Write function declarations in the form `const fnName = (...) => { ... }`.

## Type System Rules
- Do not use `any`. Prefer `unknown` with type guards when needed.
- Declare API request and response types in `src/api/*.api.*`.
- Make component props and hook return types explicit.
- Give async functions explicit return types such as `Promise<T>`.
- Minimize `as` assertions and prefer runtime validation when practical.

## Architecture And Layering
- Keep the layer split as `api -> queries -> pages/components`
- `api`: network calls, DTOs, serialization
- `queries`: React Query hooks, cache keys, invalidation policy
- `pages`: screen state, event handling, query composition
- `components`: presentation-first, minimal domain calls
- Auth state changes must go through the `AuthContext` layer only.

## Network And Error Handling
- All API calls must go through the `http` wrapper in `src/api/client.*`.
- Do not bypass token expiry, refresh, or login-required flows.
- Prefer the global feedback layer: `showGlobalAlert`, `showGlobalConfirm`, `showToast`.

## React Query Rules
- Use domain-prefixed query keys.
- Include list conditions such as page, search, and filters in query keys.
- Invalidate related query keys after successful mutations.
- Keep `QueryClient` default options consistent across the project.

## Naming And Style
- Keep domain-based file naming such as `post.api.ts` and `post.queries.ts`.
- Prefer `useXxx` for hooks, `fetchXxx` for reads, and `update/create/deleteXxx` for writes.
- Prefer named exports over default exports.
- Do not introduce new `function foo(){}` declarations in new code.

## Skill Selection Rules
- If the work touches `src/api/client.*` or global error/token flow, use `$frontend-http-client`.
- If the work mainly touches `src/api/*.api.*`, use `$frontend-api-layer`.
- If the work touches `src/auth/*`, use `$frontend-auth-flow`.
- If the work touches `src/routes/*`, layouts, or guarded routes, use `$frontend-routing-layout`.
- If the work touches `src/queries/*`, use `$frontend-query-layer`.
- If the work touches `src/app/App.*` or global UI event wiring, use `$frontend-app-shell`.
- If the work mainly touches `src/pages/*`, use `$frontend-page-patterns`.
- If the work mainly touches `src/components/*` or `src/layouts/*`, use `$frontend-component-patterns`.
- If multiple frontend layers change together, combine the matching `frontend-*` skills instead of routing through an extra orchestration skill.

## Do Not
- Leave `// @ts-ignore` in merge-ready code
- Bind untyped external responses directly into the UI
- Bypass layers with direct calls such as `pages -> raw fetch`
