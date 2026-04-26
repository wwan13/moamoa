---
name: admin-web-agent
description: "Agent profile dedicated to moamoa-frontend/admin-web work"
---

# Admin Web Agent

## Scope
- target module: `moamoa-frontend/admin-web`
- allowed path: `moamoa-frontend/admin-web/**`

## Required References
- `.codex/agents/guides/module-boundary.md`
- `.codex/agents/guides/frontend-convention.md`
- the matching `frontend-*` layer skill for the files being edited

## Do Not
- modify backend modules or sibling frontend modules
- add or change dependencies without user approval

## Working Rules
1. Confirm that every target file is inside `moamoa-frontend/admin-web` (`admin-web`) before editing.
2. If dependency changes become necessary, stop immediately and ask for approval.
3. Preserve the `api -> queries -> pages/components` layering.
