# moamoa 프로젝트 개요

이 문서는 moamoa 레포의 기본 아키텍처와 모듈 구조를 빠르게 이해하기 위한 요약입니다.

## 전체 구조
- **멀티모듈 Gradle (Kotlin DSL)**
- **Backend**: Spring Boot 3.5.7 + Kotlin 1.9.25 + Java 21, WebFlux 기반
- **Frontend**: `moamoa-web` (Vite + React)

## 모듈 구성 (Backend)

### Core
- `moamoa-core:core-api`
  - **메인 API 서버** (Spring Boot 실행 JAR)
  - WebFlux + Validation + R2DBC(MySQL) + Security/OAuth2
  - 기능별 패키지 분리: `feature/*` (auth, member, post, tag, techblog 등)
  - 내부 계층 힌트: `api / application / domain / command / query`
- `moamoa-core:core-batch`
  - **배치 처리** (Spring Batch)
  - techblog/post/member 수집/처리 파이프라인
  - reader / processor / writer / tasklet / scheduler 구조
- `moamoa-core:core-tech-blog`
  - tech blog 관련 공통 도메인/타입

#### core-api 레이어 구조
`moamoa-core/core-api/src/main/kotlin/server` 기준 요약입니다.

- `feature/*` 도메인별 기능 모듈
  - `api`: 외부 요청/응답(컨트롤러, DTO, 라우팅)
  - `application`: 유스케이스/서비스, 트랜잭션 경계
  - `domain`: 도메인 모델, 규칙, 리포지토리 인터페이스
  - `command`: 쓰기 모델(생성/수정/삭제)
  - `query`: 조회 모델(읽기 전용)
- `infra/*`: 외부 연동(DB, 캐시, OAuth2 등) 구성/어댑터
- `config/*`: Spring 설정, 빈 구성
- `security/*`: 인증/인가 관련 구성
- `global/*`: 공통 웹 설정/필터/에러 처리
- `support/*`: 공통 유틸(페이징, URI, 도메인 공통 타입)

### Infra (외부 연동/인프라)
- `moamoa-infra:infra-redis`
  - Redis Reactive 캐시/큐/메시징 구성
- `moamoa-infra:infra-mail`
  - 메일 전송/템플릿 구성 (Thymeleaf)
- `moamoa-infra:infra-security`
  - JWT, 비밀번호 암호화 등 보안 유틸
- `moamoa-infra:infra-tech-blog`
  - **기술 블로그 크롤링/수집**
  - 각 기술 블로그별 구현 (`techblogs/*`)
  - Jsoup 사용
- `moamoa-infra:infra-ai`
  - AI 연동 클라이언트 (WebFlux 기반)

### Support
- `moamoa-support:support-api-docs`
  - SpringDoc(OpenAPI) UI
- `moamoa-support:support-templates`
  - 공통 템플릿(메일 등)

### Admin
- `moamoa-admin`
  - 관리자용 도메인/애플리케이션 계층
  - WebFlux + R2DBC(MySQL)

## 모듈 의존 관계 (요약)

```
moamoa-core:core-api
  ├─ infra-mail, infra-redis, infra-security
  ├─ support-api-docs, support-templates
  ├─ moamoa-admin
  └─ core-batch

moamoa-core:core-batch
  ├─ core-tech-blog
  ├─ infra-tech-blog
  ├─ infra-redis
  └─ infra-ai

moamoa-admin
  ├─ support-api-docs
  ├─ core-tech-blog
  └─ infra-tech-blog
```

## 실행 진입점
- `moamoa-core:core-api` → `CoreApiApplication.kt`
- `moamoa-core:core-batch` → `CoreBatchApplication.kt`
- `moamoa-admin` → `AdminApplication.kt`

## Frontend (moamoa-web)
- Vite + React 19
- 주요 라이브러리: MUI, Emotion, React Query, React Router
- `moamoa-web/` 단독 프론트엔드 스택

## 참고
- 기본 빌드 설정은 루트 `build.gradle.kts`에 통합 관리됨.
- 대부분 모듈은 **라이브러리 JAR**, `core-api`만 **실행 JAR(bootJar)** 설정.
