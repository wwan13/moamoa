---
name: core-architecture
description: "moamoa 프로젝트의 아키텍처/모듈 구조를 유지·확장할 때 사용. 백엔드 모듈 추가/변경, 의존성 방향 결정, 기능 위치(core/infra/support/admin/web) 판단 시에 적용."
---

# Core Architecture

아키텍처 관련 작업 시 아래 가이드를 단일 기준으로 사용한다.

- 아키텍처 기준: `/Users/taewan/dev/moamoa/.codex/agents/guides/architecture.md`
- 모듈 경계 기준: `/Users/taewan/dev/moamoa/.codex/agents/guides/module-boundary.md`

## 현재 백엔드 기준(중요)
- core-api/admin-api는 **MVC + JPA** 기준.
- R2DBC/WebFlux 서버 패턴(reactive repository, reactive security/filter)은 사용하지 않는다.
- Redis는 blocking stack 기준(`StringRedisTemplate`/Redisson)으로 사용한다.
- WebClient 사용을 위한 의존성은 유지 가능하나, 웹 서버 계층은 MVC를 사용한다.
- 업데이트 로직은 JPQL update보다 엔티티 변경 + dirty checking 우선.
- query 계층에서 다중 DB 호출은 병렬보다 직렬을 기본으로 한다.

## 코루틴/함수 스타일 기준
- `core-tech-blog`, `infra-tech-blog`, WebClient 연동(mailSender/ai)은 `suspend` 유지 가능.
- 그 외는 기본적으로 일반 함수 사용.
- `runBlocking` 브리지는 필요한 호출 지점에서만 명시적으로 사용.

## Reactive 제한 규칙
- WebClient 기반 외부 연동 코드 외에는 reactive 타입 도입을 금지한다.
- MVC 컨트롤러/필터/보안 설정에 WebFlux 서버 패턴을 적용하지 않는다.
- JPA/Redis 영역에 `Mono`/`Flux`/reactive repository를 도입하지 않는다.
- 무분별한 `block()` 사용을 금지한다.

## 트랜잭션/아웃박스 기준
- 현재 core-api는 함수형 `server.infra.db.transaction.Transactional` DSL 사용.
- 도메인 변경 + `registerEvent(...)`(outbox 저장)를 같은 트랜잭션 블록에서 처리.

## 사용 시점
- 백엔드 모듈 추가/변경
- 모듈 간 의존성 방향 결정/수정
- 기능 위치(`core/infra/support/admin/web`) 판단
- 레이어(`api/application/domain`, 필요 시 `command/query`) 구조 결정
- `core-shared` 인터페이스(포트)와 `infra-*` 구현체 분리/확장

## 작업 절차
1. `guides/architecture.md`와 `guides/module-boundary.md`를 읽고 타깃 모듈을 고정한다.
2. 기능 추가 위치를 결정하고, 타깃 모듈 외 수정이 필요한지 점검한다.
3. 타깃 모듈 외 수정 또는 의존성 추가/변경이 필요하면 사용자 허락을 받는다.
4. 허락된 범위에서 최소 변경으로 구현한다.
5. 의존성/레이어/경계 위반 여부를 최종 검증한다.

## Shared/Infra 원칙
- `core-shared`에는 인프라 계약(인터페이스/공통 타입/어노테이션)만 둔다.
- `infra-*`에는 `core-shared` 계약의 구현체를 둔다.
- 구현체 클래스는 기본적으로 `internal`로 숨긴다.
- 애플리케이션(`core-api`, `core-batch`, `admin-api`)은 구현체가 아니라 `core-shared` 인터페이스를 우선 의존한다.
