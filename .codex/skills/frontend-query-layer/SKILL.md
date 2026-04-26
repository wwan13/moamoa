---
name: frontend-query-layer
description: "Use this skill when changing `src/queries/*` in moamoa frontend apps. It covers React Query hooks, query keys, invalidation policy, list filter keys, and post-mutation refetch strategy."
---

# Frontend Query Layer

## Scope
- `moamoa-frontend/core-web/src/queries/*`
- `moamoa-frontend/admin-web/src/queries/*`

## Rules
- Keep domain-prefixed query keys.
- Include list conditions in query keys.
- Invalidate related keys explicitly after successful mutations.
- Keep query hooks focused on API calls and cache policy, while page state remains in the page layer.

## Verify
1. Are there any query key collisions across pages that share the same data?
2. Is the post-mutation refresh scope neither too broad nor too narrow?
3. Does the hook behavior still match the default `QueryClient` policy in `main.*`?
