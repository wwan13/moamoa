# Architecture

이 문서는 moamoa 레포의 아키텍처/모듈 경계 기준이다.
아키텍처 관련 작업의 단일 기준 문서로 사용한다.

## 전체 구조
- 멀티모듈 Gradle (Kotlin DSL)
- Backend: Spring Boot + Kotlin + WebFlux
- Frontend: `moamoa-web` (Vite + React), `moamoa-admin/admin-web` (Vite + React)

## 모듈 맵
- `moamoa-core:core-api`: 메인 API 서버
- `moamoa-core:core-batch`: 배치 처리
- `moamoa-core:core-shared`: 공통 인프라 계약(인터페이스/공통 타입)
- `moamoa-core:core-tech-blog`: tech-blog 도메인 공통 타입
- `moamoa-infra:infra-redis`: Redis 기반 cache/queue/set/messaging 구현
- `moamoa-infra:infra-caffeine`: 로컬 메모리 cache 구현
- `moamoa-infra:infra-cache`: cache 구현 라우팅/전환(failover) 구현
- `moamoa-infra:infra-mailgun`: 메일 전송 구현
- `moamoa-infra:infra-lmstudio`: AI 채팅 완성 구현
- `moamoa-infra:infra-jwt`: JWT 토큰 구현
- `moamoa-infra:infra-crypto`: 비밀번호 암호화 구현
- `moamoa-infra:infra-tech-blog`: tech-blog 수집 구현
- `moamoa-support:*`: 공통 라이브러리(OpenAPI, templates)
- `moamoa-admin`: 관리자 도메인/관리 API
- `moamoa-web`: 사용자 프론트엔드

## 패키지 루트 규칙
- `moamoa-core/core-api`: `server`
- `moamoa-core/core-batch`: `server.batch`
- `moamoa-admin/admin-api`: `server.admin`

## 의존성 방향 규칙
- 도메인/기능 코드는 `core-*`에 둔다.
- 외부 시스템 연동 코드는 `infra-*`에 둔다.
- 공통 유틸은 `support-*`에 둔다.
- 인프라 모듈의 core 역의존은 금지한다.
- `core-api`만 실행 JAR(bootJar), 나머지는 라이브러리 JAR을 유지한다.
- `core-shared`는 계약(인터페이스/공통 타입)만 보유하고 구현 코드를 두지 않는다.
- `core-api`, `core-batch`, `admin-api`는 `core-shared` 인터페이스만 바라본다.
- 구현 선택/전환 로직(예: Redis 장애 시 Caffeine fallback)은 `infra-*` 계층에서 처리한다.
- `infra-*` 구현체는 기본적으로 `internal`로 숨기고 `@Primary`/`@Qualifier`로 조합한다.

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
1. `moamoa-core:core-api`에 `feature/<name>` 생성
2. `api/application/domain`부터 구성
3. 외부 연동이 필요하면 `moamoa-infra`에 어댑터 추가
4. Gradle 의존성은 모듈 경계 단위로만 추가

## 실행 진입점
- `moamoa-core:core-api` -> `CoreApiApplication.kt`
- `moamoa-core:core-batch` -> `CoreBatchApplication.kt`
- `moamoa-admin` -> `AdminApplication.kt`
