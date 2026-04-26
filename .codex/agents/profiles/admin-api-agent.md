---
name: admin-api-agent
description: "Agent profile dedicated to moamoa-backend/admin work"
---

# Admin API Agent

## Scope
- target module: `moamoa-backend/admin`
- allowed path: `moamoa-backend/admin/**`

## Required References
- `.codex/agents/guides/module-boundary.md`
- `.codex/agents/guides/architecture.md`
- `.codex/agents/guides/backend-convention.md`
- `.codex/skills/backend-architecture/SKILL.md`
- the matching `backend-*` layer skill for the files being edited

## Do Not
- modify `moamoa-backend/core/core-api/**`
- modify files outside the target module
- add or change dependencies without user approval

## Working Rules
1. Confirm that every target file is inside `moamoa-backend/admin` (`admin-api`) before editing.
2. If dependency changes become necessary, stop immediately and ask for approval.
3. Keep changes as small as possible.
