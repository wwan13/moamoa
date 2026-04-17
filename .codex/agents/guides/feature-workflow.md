# Feature Workflow

이 문서는 신규 기능 추가 시 `API 계약 확정 -> 병렬 구현 -> 통합 검증` 순서로 작업을 운영하는 기준이다.

## 목적
- 프론트와 백엔드가 서로 다른 가정으로 구현되는 문제를 줄인다.
- `core-api`, `admin-api`, `moamoa-web`, `admin-web` 작업을 안전하게 병렬화한다.
- 병렬 구현 전 계약과 수정 경계를 먼저 고정한다.

## 기본 원칙
- 구현 전에 반드시 `Plan` 단계에서 API 계약을 먼저 확정한다.
- `Plan` 단계에서는 구현하지 않고 계약과 작업 경계만 고정한다.
- 병렬 구현은 확정된 계약을 기준으로만 시작한다.
- 각 작업은 하나의 타깃 모듈만 책임진다.
- 공통 파일을 여러 작업이 동시에 수정하지 않는다.

## 전체 흐름
1. 요구사항 정리
2. `Plan`으로 API 계약 확정
3. 병렬 작업 단위 분리
4. 백엔드/프론트 병렬 구현
5. 메인 에이전트 통합 검증
6. 누락 수정 및 마무리

## Plan 단계
`Plan`의 목적은 구현을 시작하기 전에 아래 항목을 확정하는 것이다.

- 대상 기능 이름과 범위
- 변경 대상 앱
  - `moamoa-core/core-api`
  - `moamoa-admin/admin-api`
  - `moamoa-web`
  - `moamoa-admin/admin-web`
- 엔드포인트 경로와 HTTP method
- request DTO, response DTO
- 검증 규칙
- 인증 요구사항과 헤더
- 에러 응답과 상태 코드
- 프론트 화면/쿼리 단위
- 병렬 작업 ownership

### Plan 완료 조건
아래 항목이 모두 채워지기 전에는 병렬 구현을 시작하지 않는다.

- API path가 확정되었는가
- request/response shape가 확정되었는가
- 상태 코드와 에러 응답이 정해졌는가
- 인증 처리 방식이 정해졌는가
- 각 작업의 수정 경로가 분리되었는가

## 권장 Plan 출력 형식
다음 형식을 기본 템플릿으로 사용한다.

```md
## Feature
- name:
- summary:

## API Contract
- app:
- endpoint:
- method:
- auth:
- request:
- response:
- validation:
- errors:

## Front Scope
- moamoa-web:
- admin-web:

## Backend Scope
- core-api:
- admin-api:

## Parallel Ownership
- worker 1:
- worker 2:
- worker 3:
- worker 4:

## Done Criteria
- backend implemented
- frontend connected
- contract verified
```

## 병렬 구현 단계
`Plan`이 끝나면 그 결과를 기준으로 백엔드와 프론트를 병렬로 진행한다.

### 권장 분할
- worker 1: `moamoa-core/core-api/**`
- worker 2: `moamoa-admin/admin-api/**`
- worker 3: `moamoa-web/**`
- worker 4: `moamoa-admin/admin-web/**`

### 병렬 시작 조건
- 각 worker의 수정 범위가 겹치지 않아야 한다.
- DTO 계약은 이미 확정되어 있어야 한다.
- 공통 상수, 공용 타입, 공통 설정 파일 수정이 필요하면 병렬 구현 전에 먼저 처리하거나 작업을 재분할한다.

### 병렬 금지 조건
아래 경우는 병렬보다 순차 작업이 안전하다.

- 공통 모듈 의존성 추가가 필요한 경우
- 여러 앱이 같은 계약 파일을 직접 수정해야 하는 경우
- 요구사항 자체가 아직 흔들리는 경우
- 화면 구조와 API shape가 함께 탐색 중인 경우

## 검증 단계
병렬 작업이 끝나면 메인 에이전트가 아래를 통합 확인한다.

- API 계약과 실제 구현 일치 여부
- core-api/admin-api 컴파일 및 테스트
- moamoa-web/admin-web 빌드 또는 타입 체크
- 인증/인가 흐름
- 검증 실패 시 에러 응답 형태
- mutation 이후 query invalidation
- 라우트 연결과 실제 화면 동작

## 역할 분리 원칙
- `Plan` 작성자는 계약과 ownership만 고정한다.
- 각 worker는 자신의 타깃 모듈만 수정한다.
- 메인 에이전트는 최종 통합 검증과 계약 불일치 조정만 담당한다.

## 이 저장소에서의 권장 조합
- API 계약 정리: `$api-contract`
- 백엔드 구조 판단: `$core-architecture`
- 프론트 구현 규칙: `$web-frontend-architecture`
- 테스트 추가/보강: `$testing-style`

## 빠른 실행 규칙
- 기능 추가 요청을 받으면 먼저 `Plan`으로 API 계약을 만든다.
- `Plan`이 완료되면 각 타깃 모듈별 worker를 병렬로 시작한다.
- worker 결과를 메인 에이전트가 모아 검증하고 누락을 마무리한다.
