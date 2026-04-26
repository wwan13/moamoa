---
name: logging-strategy
description: "moamoa 공통 로깅 규약. traceId 자동 주입, 로그 유형 태그, 중복 로깅 방지 기준 적용."
---

# Logging Strategy

## 규칙 요약
- 로거는 `kotlin-logging`의 `KLogger` 사용.
- traceId는 MDC 기반(`X-Trace-Id` 우선, 없으면 생성값, 부재 시 `SYSTEM`).
- 메시지는 짧은 평문 + 구조화 필드(key-value) 병행.
- 중복 에러 로깅 금지(최종 사용자 응답 에러는 ControllerAdvice에서 정리).

## 출력 포맷
- local/prod 모두 JSON 로그를 사용한다(`logback-spring.xml`).
- 구조화 필드(`call`, `latencyMs`, `errorType` 등)는 JSON 필드로 적재된다.

## 로그 발생 기준
1. 요청 경계 (`[REQ]`): method/path/status/latencyMs/userId/clientIp
2. 비즈니스 이벤트 (`[BIZ]`): 상태 변화 이벤트 발행 시
3. 외부 의존성 (`[DB]`, `[REDIS]`, `[API]`): 호출 단위 성공/실패
4. 오류 (`[REQ_ERROR]`, `[WORKER]`): 비즈니스 WARN, 시스템 ERROR

## MVC 전환 후 주의
- 요청 경계는 servlet filter 기반으로 기록한다.
- `/admin` 경로는 admin 전용 경계 필터만 기록하고, 공통 경계 필터는 제외한다.
- coroutine continuation 기반 trace 전파 규칙은 사용하지 않는다.

## 사용
- request/event/db/redis/api/errorType 확장 로거를 우선 사용.
- 주요 필드(`call`, `target`, `latencyMs`, `success`, `errorType`)를 항상 함께 남긴다.
