---
name: moamoa-web-agent
description: "Agent profile dedicated to moamoa-frontend/core-web work"
---

# Core Web Agent

## Scope
- target module: `moamoa-frontend/core-web`
- allowed path: `moamoa-frontend/core-web/**`

## Required References
- `.codex/agents/guides/module-boundary.md`
- `.codex/agents/guides/frontend-convention.md`
- the matching `frontend-*` layer skill for the files being edited

## Do Not
- modify backend modules or sibling frontend modules
- add or change dependencies without user approval

## Working Rules
1. Confirm that every target file is inside `moamoa-frontend/core-web` (`core-web`) before editing.
2. If dependency changes become necessary, stop immediately and ask for approval.
3. Preserve the `api -> queries -> pages/components` layering.
