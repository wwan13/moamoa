# Module Boundary

This document enforces file edit boundaries for agent work.

## Shared Mandatory Rules
- Each task may modify files only inside the assigned target module path.
- Do not modify files outside the target module.
- If dependency changes are required in `build.gradle.kts`, `settings.gradle.kts`, the version catalog, or module dependency blocks, stop immediately and get user approval.
- Do not create new modules, add cross-module dependencies, or change module structure without approval.

## Allowed Edit Paths Per Target
- `agent-config`
  - allowed: `AGENTS.md`, `.codex/**`
  - forbidden: `moamoa-backend/**`, `moamoa-frontend/**`, except `.codex/**`
- `core-api`
  - allowed: `moamoa-backend/core/core-api/**`
  - forbidden: `moamoa-backend/admin/**`, `moamoa-backend/infra/**`, `moamoa-backend/support/**`, `moamoa-backend/core/core-batch/**`, `moamoa-frontend/**`
- `admin-api`
  - allowed: `moamoa-backend/admin/**`
  - forbidden: `moamoa-backend/core/**`, `moamoa-backend/infra/**`, `moamoa-backend/support/**`, `moamoa-frontend/**`
- `core-web`
  - allowed: `moamoa-frontend/core-web/**`
  - forbidden: `moamoa-backend/**`, `moamoa-frontend/admin-web/**`
- `admin-web`
  - allowed: `moamoa-frontend/admin-web/**`
  - forbidden: `moamoa-backend/**`, `moamoa-frontend/core-web/**`

## core-api / admin-api Mutual Exclusion
- A `core-api` task must not modify `moamoa-backend/admin/**`.
- An `admin-api` task must not modify `moamoa-backend/core/core-api/**`.
- If a request requires both modules, do not treat it as one task. Split it after user approval.

## Documentation Task Rules
- Changes to `AGENTS.md` or `.codex/**` must be handled as `agent-config` work only.
- If a documentation task starts requiring source-code changes, split that source work into a separate module task.
