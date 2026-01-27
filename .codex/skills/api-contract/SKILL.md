---
name: api-contract
description: "moamoa HTTP API 계약(엔드포인트/DTO/검증/헤더/에러 응답)을 추가/변경할 때 사용."
---

# API Contract

core-api의 기존 API 패턴을 유지한다.

## 엔드포인트 규칙
- 기본 경로는 `/api/<feature>`.
- `@RestController` + `@RequestMapping` 사용.
- 응답은 `ResponseEntity`.

## 요청/응답
- 입력은 `@RequestBody @Valid` DTO.
- 응답은 application 레이어 DTO(엔티티 직접 노출 금지).

## 인증
- 인증 필요 시 `@RequestPassport` 사용.
- 토큰 재발급은 `X-Refresh-Token` 헤더 사용.

## 에러 응답
- 전역 에러 형태: `{ status: Int, message: String }`.
- `ApiControllerAdvice` 매핑 규칙을 따른다.

## 상태 코드
- 400: 검증/바인딩 오류
- 401/403: 인증/인가 오류
- 500: 서버 오류
