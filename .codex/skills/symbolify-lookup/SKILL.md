---
name: symbolify-lookup
description: "Use this skill when you need fast code-definition lookup in moamoa without broad grep/file scans. It applies to Kotlin symbols in core-api via the Gradle `symbolify*` tasks and React/TypeScript symbols in core-web via the npm `symbolify:*` scripts."
---

# Symbolify Lookup

Use `symbolify` when the task is primarily "find the definition" rather than "search arbitrary text".

Why this exists:
- `rg` finds every text hit, so imports, usages, comments, and nearby helper code get mixed together.
- for high-usage symbols, that means many candidate files and many irrelevant lines before you reach the definition
- `symbolify` narrows that first step to definition records, so you read fewer files before opening source

Prefer this skill for:
- locating a Kotlin class, function, property, or typealias definition in `core-api`
- locating a React component, hook, context, type, or interface definition in `core-web`
- running one repo-root command before falling back to broader file scans
- narrowing the exact file and line range before opening source files

Good fit examples:
- "AuthService 어디 정의됐지?"
- "useAuth 선언 파일이 뭐지?"
- "AuthContext 정의만 바로 보고 싶다"
- "grep 결과가 너무 많아서 정의 파일만 먼저 좁히고 싶다"

Do not use this skill for:
- arbitrary text/regex search
- reference search
- call graph analysis
- searching other modules that do not have `symbolify` wired yet

Bad fit examples:
- "\"refresh token\" 문자열이 어디 있지?"
- "useAuth를 어디서 쓰지?"
- "이 API가 어떤 흐름으로 연결되지?"
- "admin-web 쪽에서 이 심볼이 있나?"

## Current Basis

Indexing basis:
- Kotlin `core-api`: symbol-level index from Kotlin PSI declarations
- React `core-web`: symbol-level index from TypeScript/TSX AST declarations
- it is not a file-level full-text index

Current indexed unit:
- one record per definition symbol
- examples: class, function, property, typealias, hook, context, component

Stored metadata:
- symbol name
- symbol kind
- file path
- line range and column range
- signature-like preview
- container metadata when available

## Search Model

Search basis:
- exact symbol-name lookup against indexed definition records
- root `symbolifyFind` is an orchestrator over currently wired targets
- this is structure-based definition search, not keyword ranking or fuzzy full-text retrieval

What the result means:
- `symbolifyFind` answers "where is this symbol defined?"
- it does not answer "where is this used?"
- it does not search arbitrary strings inside implementation bodies

Why it helps:
- `rg` is text-first, so definition, import, usage, and comment hits are mixed together
- `symbolify` is definition-first, so the first result set is smaller when the symbol is widely used
- this is most useful when the cost is choosing which file to open, not when the cost is understanding the implementation after opening it

## Commands

### Root

Use the root commands first unless you explicitly need a module-local command.

Build all currently wired indexes:

```bash
rtk ./gradlew symbolifyIndex
```

Find a definition across all wired targets:

```bash
rtk ./gradlew symbolifyFind -PsymbolName=useAuth
```

Find a definition in one target only:

```bash
rtk ./gradlew symbolifyFind -PsymbolName=AuthService -PsymbolifyTarget=core-api
rtk ./gradlew symbolifyFind -PsymbolName=useAuth -PsymbolifyTarget=core-web
```

Show a symbol snippet from one target:

```bash
rtk ./gradlew symbolifyShow -PsymbolId=<id> -PsymbolifyTarget=core-api
rtk ./gradlew symbolifyShow -PsymbolId=<id> -PsymbolifyTarget=core-web
```

This currently runs:
- `core-api` Kotlin symbol indexing
- `core-web` React/TypeScript symbol indexing

Root command rules:
- `symbolifyFind` defaults to `-PsymbolifyTarget=all`
- `symbolifyShow` requires `-PsymbolifyTarget=core-api|core-web`

### Kotlin / core-api

Build the symbol index:

```bash
rtk ./gradlew :moamoa-backend:core:core-api:symbolifyIndex
```

Find a definition by name:

```bash
rtk ./gradlew :moamoa-backend:core:core-api:symbolifyFind -PsymbolName=AuthService
```

Show a symbol snippet by id:

```bash
rtk ./gradlew :moamoa-backend:core:core-api:symbolifyShow -PsymbolId=<id>
```

Notes:
- index output path: `moamoa-backend/core/core-api/build/symbolify/core-api`
- Kotlin implementation lives in `moamoa-backend/support/support-symbolify`
- the current Kotlin indexer is `kotlin-compiler-embeddable` based and emits a K1 deprecation warning during compile

### React / core-web

Build the symbol index:

```bash
cd moamoa-frontend/core-web && rtk npm run symbolify:index
```

Find a definition by name:

```bash
cd moamoa-frontend/core-web && rtk npm run symbolify:find -- --name useAuth
```

Show a symbol snippet by id:

```bash
cd moamoa-frontend/core-web && rtk npm run symbolify:show -- --id <id>
```

Notes:
- index output path: `moamoa-frontend/core-web/build/symbolify/core-web`
- React implementation lives in `moamoa-frontend/core-web/scripts/symbolify.mjs`

## Workflow

1. If the index might be stale, run `rtk ./gradlew symbolifyIndex`.
2. Run `rtk ./gradlew symbolifyFind -PsymbolName=<name>` first.
3. If the result set is noisy, rerun with `-PsymbolifyTarget=core-api` or `-PsymbolifyTarget=core-web`.
4. Use `symbolifyShow` with the returned id only for the target you want to inspect.
5. Only open the source file if the symbol metadata and snippet are not enough.

## Output Expectations

`find` prints one line per symbol:
- `id`
- `kind`
- `moduleHint`
- `filePath:start-end`
- `signature`

`show` prints:
- symbol name and kind
- exact file path and line range
- a small snippet around the definition

## Limitations

- Kotlin support is wired only for `core-api`.
- React support is wired only for `core-web`.
- `symbolify` is definition-oriented, so it will not replace `rg` for free-text investigation.
- Root lookup is an orchestrator over the currently wired targets, not a generic whole-repo symbol engine.
- If a symbol is missing, fall back to `rtk rg` or direct file inspection.
