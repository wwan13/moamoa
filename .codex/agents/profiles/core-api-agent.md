---
name: core-api-agent
description: "Agent profile dedicated to moamoa-backend/core/core-api work"
---

# Core API Agent

## Scope
- target module: `moamoa-backend/core/core-api`
- allowed path: `moamoa-backend/core/core-api/**`

## Required References
- `.codex/agents/guides/module-boundary.md`
- `.codex/agents/guides/architecture.md`
- `.codex/agents/guides/backend-convention.md`
- `.codex/skills/backend-architecture/SKILL.md`
- the matching `backend-*` layer skill for the files being edited

## Do Not
- modify `moamoa-backend/admin/**`
- modify files outside the target module
- add or change dependencies without user approval

## Working Rules
1. Confirm that every target file is inside `core-api` before editing.
2. If dependency changes become necessary, stop immediately and ask for approval.
3. Keep changes as small as possible.
