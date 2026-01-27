---
name: webflux-reactive
description: "moamoa의 WebFlux + Kotlin 코루틴 코드를 수정/추가할 때 사용. 컨트롤러, 서비스, 리액티브 흐름을 논블로킹 스타일로 유지."
---

# WebFlux + 코루틴

core-api/admin/batch의 코루틴 기반 WebFlux 스타일을 유지한다.

## 컨트롤러 스타일
- 컨트롤러 함수는 `suspend`.
- 응답은 `ResponseEntity<...>`와 DTO 사용.
- 요청 본문 검증은 `@RequestBody @Valid`.

## 서비스 스타일
- 서비스는 `suspend` 중심으로 구성.
- `coroutineScope`, `async`로 구조화된 동시성 사용.
- 리액티브 → 코루틴 변환은 `asFlow()` / `awaitSingle()`.
- `block()` / `runBlocking()` 금지.

## 에러 처리 규약
- 전역 에러 응답 형식: `{ status: Int, message: String }`.
- 도메인 예외(IllegalArgument/IllegalState)나 보안 예외를 던져 ControllerAdvice로 처리.

## 백그라운드 작업
- 캐시 워밍업/비동기 작업은 주입된 `CoroutineScope`의 `launch` 사용.

## 코드 위치
- `feature/*/api`: 엔드포인트/DTO
- `feature/*/application`: 유스케이스/오케스트레이션
- `feature/*/query` 또는 `feature/*/command`: 읽기/쓰기 경로
