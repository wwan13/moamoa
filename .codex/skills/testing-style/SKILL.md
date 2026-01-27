---
name: testing-style
description: "moamoa 테스트 스타일. core-api/admin/batch에서 테스트 추가/수정 시 사용."
---

# Testing Style

현재 테스트 파일이 없으므로 아래 기본 규칙을 따른다.

## 도구
- `spring-boot-starter-test` + JUnit 5
- 코루틴 테스트는 `kotlinx-coroutines-test` 사용

## 범위
- WebFlux 컨트롤러는 slice 테스트 중심
- 서비스는 유스케이스 단위 테스트
- 퍼시스턴스는 R2DBC 친화 테스트 (blocking JDBC 금지)

## 구조
- `src/test/kotlin`에 패키지 구조 그대로 배치
- 모듈별(core-api, admin, core-batch)로 테스트 분리

## 기본 커버리지
- 요청 검증
- 인증/인가
- 에러 매핑
- 쿼리 서비스 SQL 매핑/페이징
