---
name: commit-style
description: "Use this skill when proposing commit messages or commit splits for the moamoa repository. It follows the recent repository pattern and suggests messages in the `type(scope) : summary` format while checking for missing verification points."
---

Use this skill when:
- the user wants a commit message written for them
- the user wants staged changes split into commit units
- the correct type such as `feat`, `fix`, `style`, or `chore` is unclear
- the user wants a quick pre-commit check for missing tests, routes, or API wiring

## Default Format
- Use `type(scope) : summary`
- Common types in recent history: `feat`, `fix`, `style`, `chore`
- Common scopes in recent history: `front`, `server`
- Keep the summary short and concrete
- Prefer one problem or one feature per commit

## Scope Guidance
- Changes under `moamoa-frontend/core-web` or `moamoa-frontend/admin-web` usually map to `front`
- Changes under `moamoa-backend/**` usually map to `server`
- Changes to `AGENTS.md` or `.codex/**` usually map to `agent-config`
- `.gitignore`, local tooling, deployment, or environment configuration often map to `chore`

## Split Guidance
- If frontend and backend both changed for one goal, first check whether they can still be split safely
- If one indivisible task spans both sides, choose the single scope that best represents the main impact
- Split CSS-only work from behavior changes whenever practical
- Split server steps when they represent different rollout or verification points

## Type Guidance
- `feat`: new page, new API, new batch job, new template, or new behavior
- `fix`: bug fix, routing/query/condition/HTTP method correction, or compile error fix
- `style`: CSS, layout, text copy, or publishing changes with little or no behavior change
- `chore`: settings, ignore rules, development tools, runtime environment, or dependency cleanup

## Workflow
1. Classify changed files first: `front`, `server`, or `agent-config`
2. Choose the type from the actual diff, not from the user’s wording alone
3. Compress the summary so it exposes one result or one problem
4. If unrelated work is mixed together, propose a split before proposing a message
5. Provide one primary message, and only one alternative if the choice is ambiguous

## Verification Prompts
- Is there a backend behavior change with no visible test or verification path?
- Was a route added without wiring `AppRoutes` or the actual entry path?
- Did an API method change without aligning both frontend and backend callers?
- Did a batch or template change happen without corresponding scheduler, config, or test updates?
- Does a change that looks like `style` actually change behavior?

## Output Style
- Default output: one commit message and one short reason
- If a split is needed, explain the split boundary first and then propose multiple messages
- If the user wants it, reprint just the final one-line message for direct `git commit` use

## Examples
- `feat(front) : publish notice detail page`
- `fix(server) : fill full size in post list query`
- `style(front) : adjust active tab color`
- `chore(agent-config) : reorganize agent operating docs`
