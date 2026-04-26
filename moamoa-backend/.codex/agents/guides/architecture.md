# Architecture

이 문서는 `moamoa-backend`의 아키텍처/모듈 경계 기준이다.
아키텍처 관련 작업의 단일 기준 문서로 사용한다.

## 전체 구조
- 멀티모듈 Gradle (Kotlin DSL)
- Backend: Spring Boot + Kotlin + WebFlux
- Frontend: `moamoa-frontend/core-web` (Vite + React), `moamoa-frontend/admin-web` (Vite + React)

## 모듈 맵
- `moamoa-backend:core:core-api`: 메인 API 서버
- `moamoa-backend:core:core-batch`: 배치 처리
- `moamoa-backend:infra:*`: 기능별 `api/impl/starter` 모듈
- `moamoa-backend:infra:cache-*`: cache 계약/구현(redis, caffeine, resilient)/starter
- `moamoa-backend:infra:queue-*`: queue 계약/redis 구현/starter
- `moamoa-backend:infra:set-*`: set 계약/redis 구현/starter
- `moamoa-backend:infra:lock-*`: lock 계약/redisson·local·resilient 구현/starter
- `moamoa-backend:infra:messaging-*`: messaging 계약/redis 구현/starter
- `moamoa-backend:infra:token-*`: token 계약/jwt 구현/starter
- `moamoa-backend:infra:password-*`: password 계약/crypto 구현/starter
- `moamoa-backend:infra:mail-*`: mail 계약/mailgun 구현/starter
- `moamoa-backend:support:*`: 공통 라이브러리(OpenAPI, templates)
- `moamoa-backend:admin`: 관리자 API

## 패키지 루트 규칙
- `moamoa-backend/core/core-api`: `server`
- `moamoa-backend/core/core-batch`: `server.batch`
- `moamoa-backend/admin`: `server.admin`

## 의존성 방향 규칙
- 도메인/기능 코드는 `core-*`에 둔다.
- 외부 시스템 연동 코드는 `infra-*`에 둔다.
- 공통 유틸은 `support-*`에 둔다.
- 인프라 모듈의 core 역의존은 금지한다.
- `core-api`와 `admin`만 실행 JAR(bootJar), 나머지는 라이브러리 JAR을 유지한다.
- 인프라 계약은 `moamoa-backend:infra:*‑api`에 둔다.
- 애플리케이션(`core-api`, `core-batch`, `admin-api`)은 구현 모듈 대신 `*-starter` 의존을 기본으로 한다.
- 앱에서 계약 타입을 사용할 때는 starter의 전이 의존(`api(...)`)을 우선 사용하고, 별도 `*-api` 직접 의존은 특별한 이유가 있을 때만 추가한다.
- 구현 선택/전환 로직(예: Redis 장애 시 Caffeine fallback)은 `*-resilient` 구현 모듈에서 처리한다.
- 구현 모듈 클래스는 기본적으로 `internal`로 숨기고 starter 자동구성에서 조합한다.
- starter는 `AutoConfiguration.imports` 기반으로 자동구성을 제공한다.

## core-api 레이어 규칙
기본 레이어는 `feature/<name>/api`, `application`, `domain` 3단으로 구성한다.

- `api`: 요청/응답 DTO, 컨트롤러/라우팅
- `application`: 유스케이스 조립, 트랜잭션 경계
- `domain`: 도메인 모델/규칙, 리포지토리 인터페이스

### command/query 분리 기준
- 기본적으로 `command/query`를 분리하지 않는다.
- 조회가 복잡할 때만 분리한다.
- 복잡한 조회 기준: JOIN이 1개 이상 포함되는 조회.

분리 시 구조:
- 쓰기 경로: `api -> command`
- 읽기 경로: `api -> query`

## 신규 기능 추가 절차
1. `moamoa-backend:core:core-api`에 `feature/<name>` 생성
2. `api/application/domain`부터 구성
3. 외부 연동이 필요하면 `moamoa-backend:infra`에 어댑터 추가
4. Gradle 의존성은 모듈 경계 단위로만 추가

## 실행 진입점
- `moamoa-backend:core:core-api` -> `CoreApiApplication.kt`
- `moamoa-backend:core:core-batch` -> `CoreBatchApplication.kt`
- `moamoa-backend:admin` -> `AdminApplication.kt`
