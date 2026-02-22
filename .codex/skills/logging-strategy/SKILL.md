---
name: logging-strategy
description: "moamoa 공통 로깅 규약. traceId 자동 주입, 로그 유형 태그, 중복 로깅 방지 기준 적용."
---

# Logging Strategy

## 규칙 요약
- 로거는 `kotlin-logging`의 `KLogger` 사용
- 포맷: `[traceId] message` (`X-Trace-Id` 우선, 없으면 UUID 8자리, 부재 시 `NONE`)
- 메시지는 평문만 작성, traceId는 MDC + logback 자동 주입
- 바디는 기본 미로깅, 에러 중복 로깅 금지(최종은 `ControllerAdvice`)

## 로그 발생 기준
1. 요청 경계: 시작/종료 (`[REQ_START]`, `[REQ_END]`)  
필드: `method`, `path`, `status`, `latencyMs`, `clientIp`, `userId`
2. 비즈니스 이벤트: 의미 있는 상태 변화만 (`[BIZ]`)  
현재: 회원 생성, 북마크 등록/해제, 제보 생성, 구독/해제/알림토글
3. 외부 의존성 호출: DB/Redis/API (`[DB]`, `[REDIS]`, `[API]`)  
필드: `result`, `call`, `target`, `latencyMs`, `retry`, `timeout`, 실패 시 `errorCode`, `errorSummary`
4. 오류/예외: 비즈니스 예외 `WARN`, 시스템 예외 `ERROR` (`[REQ_ERROR]`, `[WORKER]`)

## 사용
- `logger.infoWithTrace { ... }`
- `logger.info(continuation) { ... }`
- `logger.infoWithTraceId(traceId) { ... }`
