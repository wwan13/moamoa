---
name: testing-style
description: "moamoa 테스트 스타일. core-api/admin/batch에서 테스트 추가/수정 시 사용."
---

# Testing Style

이 문서는 core-api / admin / core-batch 모듈에서 테스트를 추가·수정할 때의 기본 규칙을 정의한다.

---

## 도구

- JUnit 5 (`spring-boot-starter-test`)
- MockK
- Assertion은 Kotest assertions (`kotest-assertions-core`)
- 코루틴 테스트는 `kotlinx-coroutines-test`의 `runTest` 사용

---

## 테스트 타입

프로젝트 공통 베이스 클래스를 상속해서 작성한다.

- `UnitTest`: 순수 단위 테스트 (Spring Context 없음)
- `RepositoryTest`: R2DBC 슬라이스 테스트 (`@DataR2dbcTest`)
- `IntegrationTest`: 통합 테스트 (`@SpringBootTest`)

---

## 범위 / 우선순위

- WebFlux 컨트롤러: slice 테스트 우선 (HTTP/검증/매핑 중심)
- 서비스: 유스케이스 단위 테스트 우선 (상태 변경 + 협력 객체 호출 검증)
- 퍼시스턴스: R2DBC 기준으로 검증 (blocking JDBC 사용 금지)

---

## 구조

- `src/test/kotlin` 아래에 프로덕션 패키지 구조 그대로 배치
- 모듈별로 테스트를 분리한다.
    - core-api: API/유스케이스/쿼리 서비스
    - admin: 관리자 기능, 권한/정책 중심
    - core-batch: 배치 실행/스케줄/재처리 흐름 중심

---

## 네이밍

- 테스트 이름은 항상 한글 (`fun \`...\``)
- 클래스명
    - 단위 테스트: `XxxTest`
    - 통합 테스트: `XxxIntegrationTest`
- 한 테스트는 한 시나리오만 검증 (given/when/then 유지)

---

## 기본 커버리지 체크리스트

### API / Controller
- 요청 검증 (validation)
- 인증/인가 (권한별 성공/실패)
- 에러 매핑 (예외 → 상태코드/응답 바디)
- 요청/응답 DTO 매핑 (필드 누락/기본값 포함)

### Service / Usecase
- 성공 시나리오 (저장/수정 결과)
- 실패 시나리오 (없는 리소스, 금지된 동작, 중복/멱등)
- outbox/이벤트 등록 여부 (필요한 경우)

### Query / SQL (R2DBC)
- SQL 매핑 (컬럼 alias/타입/nullable)
- 페이징/정렬 (경계값 포함)
- 조건 조합 (필터 유무에 따른 결과)

---

## 고정 원칙

- suspend 함수 테스트는 반드시 `runTest` 사용
- 외부 시스템(HTTP/Redis/Stream)은 단위 테스트에서 직접 호출하지 않는다.
- DB가 필요한 검증은 `RepositoryTest` 또는 `IntegrationTest`로 분리한다.