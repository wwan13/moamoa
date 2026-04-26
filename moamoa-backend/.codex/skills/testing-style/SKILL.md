---
name: testing-style
description: "moamoa 테스트 스타일. core-api/admin/batch에서 테스트 추가/수정 시 사용."
---

# Testing Style

core-api / admin / core-batch 테스트 기본 규칙.

## 도구
- JUnit 5 (`spring-boot-starter-test`)
- MockK
- Assertion은 Kotest assertions
- 코루틴 테스트는 필요한 지점에서만 `runTest` 사용

## 테스트 타입
- `UnitTest`: 순수 단위 테스트 (Spring Context 없음)
- `IntegrationTest`: 통합 테스트 (`@SpringBootTest`)
- 퍼시스턴스는 현재 JPA/JDBC 기준으로 테스트

## 범위 / 우선순위
- MVC 컨트롤러: HTTP/검증/보안/에러 매핑 검증 우선
- 서비스: 상태 변경 + 협력 객체 호출 + outbox 등록 검증
- query(SQL): alias/nullable/페이징/조건 조합 검증

## 구조
- `src/test/kotlin` 아래에 프로덕션 패키지 구조 그대로 배치
- 한 테스트는 한 시나리오만 검증 (given/when/then)
- 테스트 이름은 한글 `fun \`...\`` 규칙 유지

## 현재 마이그레이션 주의
- WebFlux 전용 테스트 유틸(`WebTestClient`, reactive resolver/filter 시나리오)은 MVC 기준으로 정리.
- Optional 기반 mocking은 `findByIdOrNull` 전환 상태와 맞춘다.
- suspend 테스트는 실제 suspend 경로(core-tech-blog/infra-tech-blog/webClient 연동)에만 사용.
