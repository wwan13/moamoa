---
name: backend-logging-strategy
description: "Use this skill for backend logging work in moamoa. It covers traceId propagation, request boundary filters, structured logging fields, outbound dependency logs, and duplicate error logging prevention."
---

# Backend Logging Strategy

## Summary Rules
- Use `kotlin-logging` `KLogger`.
- traceId is MDC-based. Prefer `X-Trace-Id`, generate one if missing, and fall back to `SYSTEM` for non-request work.
- Keep log messages short and pair them with structured key-value fields.
- Avoid duplicate error logging. Final response errors should be normalized in controller advice.

## Output Format
- Both `local` and `prod` use JSON logs in `logback-spring.xml`.
- Structured fields such as `call`, `latencyMs`, and `errorType` are emitted as JSON fields.

## Log Categories
1. Request boundary (`[REQ]`): method, path, status, latency, userId, clientIp
2. Business events (`[BIZ]`): state changes and event publication
3. External dependencies (`[DB]`, `[REDIS]`, `[API]`): per-call success and failure
4. Errors (`[REQ_ERROR]`, `[WORKER]`): business warnings and system errors

## Request Boundary Notes
- Request boundary logging is implemented with servlet filters.
- `core-api` uses `RequestBoundaryLogFilter` and skips `/api/admin` paths.
- `admin-api` uses `AdminRequestBoundaryLogFilter` for `/admin` paths.
- Do not rely on coroutine-continuation style trace propagation rules here.

## Preferred Usage
- Prefer the typed log extensions such as `request`, `event`, `db`, `redis`, `api`, and `errorType`.
- Include core fields such as `call`, `target`, `latencyMs`, `success`, and `errorType` when they apply.
