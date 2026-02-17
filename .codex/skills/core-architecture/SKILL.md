---
name: core-architecture
description: "moamoa 프로젝트의 아키텍처/모듈 구조를 유지·확장할 때 사용. 백엔드 모듈 추가/변경, 의존성 방향 결정, 기능 위치(core/infra/support/admin/web) 판단 시에 적용."
---

# Core Architecture

아키텍처 관련 작업 시 아래 가이드를 단일 기준으로 사용한다.

- 아키텍처 기준: `/Users/taewan/dev/moamoa/.codex/agents/guides/architecture.md`
- 모듈 경계 기준: `/Users/taewan/dev/moamoa/.codex/agents/guides/module-boundary.md`

## 사용 시점
- 백엔드 모듈 추가/변경
- 모듈 간 의존성 방향 결정/수정
- 기능 위치(`core/infra/support/admin/web`) 판단
- 레이어(`api/application/domain`, 필요 시 `command/query`) 구조 결정
- `core-shared` 인터페이스(포트)와 `infra-*` 구현체 분리/확장

## 작업 절차
1. 먼저 `guides/architecture.md`와 `guides/module-boundary.md`를 읽고 타깃 모듈을 고정한다.
2. 기능 추가 위치를 결정하고, 타깃 모듈 외 수정이 필요한지 점검한다.
3. 타깃 모듈 외 수정 또는 의존성 추가/변경이 필요하면 즉시 중단하고 사용자 허락을 받는다.
4. 허락된 범위에서 최소 변경으로 구현한다.
5. 최종 변경이 가이드의 의존성/레이어/경계 규칙을 위반하지 않는지 검증한다.

## Shared/Infra 원칙
- `core-shared`에는 인프라 계약(인터페이스/공통 타입/어노테이션)만 둔다.
- `infra-*`에는 `core-shared` 계약의 구현체를 둔다.
- 구현체 클래스는 기본적으로 `internal`로 숨긴다.
- 애플리케이션(`core-api`, `core-batch`, `admin-api`)은 구현체가 아니라 `core-shared` 인터페이스만 의존한다.
- 구현 선택/전환(예: Redis/Caffeine failover)은 `infra-*` 계층에서 처리한다.
- 구현체 빈 이름이 충돌하거나 라우팅이 필요하면 `@Qualifier`/`@Primary`를 인프라 계층에서만 사용한다.
- AOP/프록시를 사용할 경우 대상 pointcut은 인터페이스 경계를 명확히 제한한다.

## 빠른 체크
- 도메인 로직은 `core-*`, 외부 연동은 `infra-*`, 공통 유틸은 `support-*`인가?
- `infra -> core` 역의존이 생기지 않았는가?
- 타깃 모듈 외 파일을 수정하지 않았는가?
- `core-api`와 `admin-api`를 동시에 수정하지 않았는가?
- `core-shared`에는 구현 코드가 들어가지 않았는가?
- `infra-*` 구현체가 외부로 불필요하게 노출되지 않았는가(`internal`)?
