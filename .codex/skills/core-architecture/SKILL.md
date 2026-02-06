---
name: core-architecture
description: "moamoa 프로젝트의 아키텍처/모듈 구조를 유지·확장할 때 사용. 백엔드 모듈 추가/변경, 의존성 방향 결정, 기능 위치(core/infra/support/admin/web) 판단 시에 적용."
---

# Core Architecture

moamoa의 멀티모듈 경계와 레이어 구조를 기존 방식대로 유지한다.

## 빠른 체크
- 실행 진입점 구분: core-api(API), core-batch(배치), admin(어드민 API), web(프론트).
- 도메인 로직은 `moamoa-core`에, 외부 연동은 `moamoa-infra`에 둔다.
- 공통 유틸은 `moamoa-support`에만 둔다.

## 패키지 루트 규칙
- `moamoa-admin/admin-api`: `server.admin`
- `moamoa-core/core-batch`: `server.batch`
- `moamoa-core/core-api`: `server`

## 현재 모듈 맵
- `moamoa-core:core-api`: 메인 API 서버 (WebFlux + R2DBC + Security)
- `moamoa-core:core-batch`: 배치 처리 (Spring Batch)
- `moamoa-core:core-tech-blog`: tech-blog 도메인 추상층, 구현은 infra-tech-blog 모듈
- `moamoa-infra:*`: 외부 연동 (redis, mail, security, tech-blog crawling, ai)
- `moamoa-support:*`: 공통 라이브러리 (OpenAPI, templates)
- `moamoa-admin`: 관리자 도메인/관리 API
- `moamoa-web`: 프론트엔드 (Vite + React)

## 의존성 방향 규칙
- 도메인/기능 코드는 `core-*`에 둔다.
- 인프라 모듈은 core에서만 사용하며 역의존은 금지.
- `core-api`만 실행 JAR(bootJar), 나머지는 라이브러리 JAR.

## 신규 기능 추가 절차
1. `moamoa-core:core-api`에 `feature/<name>` 생성.
2. 기본 레이어는 `api / application / domain` 3단으로 구성한다.
    - `api`: 요청/응답 DTO, 라우팅/컨트롤러. 비즈니스 로직 금지.
    - `application`: 유스케이스 조립, 트랜잭션 경계.
    - `domain`: 도메인 모델/규칙, 리포지토리 인터페이스.

   #### Command / Query 분리 기준
    - 기본적으로는 `command / query`를 분리하지 않는다.
    - **복잡한 조회 로직이 존재하는 경우에만 분리한다.**
    - 복잡한 조회의 기준은 **JOIN이 1개 이상 포함되는 조회**이다.

   ##### 분리 시 구조
    - 쓰기 경로: `api → command (application/domain)`
        - 생성/수정/삭제 및 도메인 규칙 처리
    - 읽기 경로: `api → query`
        - JOIN, 집계, 페이징 등 조회 전용 로직
        - 조회 전용 DTO/DAO 사용 가능 (도메인 엔티티 강제하지 않음)
        - Service 에서 r2dbc DatabaseClient 직접 사용 가능
 
3. 외부 연동이 필요하면 `moamoa-infra`에 어댑터 추가.
4. Gradle 의존성은 모듈 경계만 추가(불필요한 공유 모듈 지양).

## 애매할 때 기준
- 거대한 공용 모듈보다 작고 명시적인 모듈 의존성 선호.
