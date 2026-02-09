# AGENT 운영 진입점

이 문서는 moamoa 저장소의 에이전트 라우팅과 강제 제약을 정의한다.
상세 규칙은 `guides/*.md`, 모듈별 실행 규칙은 `profiles/*.md`, 도메인 작업 절차는 `skills/*/SKILL.md`를 따른다.

## Guides
- 아키텍처: `/Users/taewan/dev/moamoa/.codex/agents/guides/architecture.md`
- 공통 작업 규칙: `/Users/taewan/dev/moamoa/.codex/agents/guides/convention.md`
- 모듈 경계 규칙: `/Users/taewan/dev/moamoa/.codex/agents/guides/module-boundary.md`
- 커밋 규칙: `/Users/taewan/dev/moamoa/.codex/agents/guides/commit.md`
- 프론트엔드 규칙: `/Users/taewan/dev/moamoa/.codex/agents/guides/frontend_convention.md`

## Agent Profiles
- core-api: `/Users/taewan/dev/moamoa/.codex/agents/profiles/core-api-agent.md`
- admin-api: `/Users/taewan/dev/moamoa/.codex/agents/profiles/admin-api-agent.md`
- moamoa-web: `/Users/taewan/dev/moamoa/.codex/agents/profiles/moamoa-web-agent.md`
- admin-web: `/Users/taewan/dev/moamoa/.codex/agents/profiles/admin-web-agent.md`

## 강제 제약
- 작업 시작 시 반드시 타깃 모듈 하나를 고정한다.
- 타깃 모듈 외 경로 수정은 금지한다.
- `core-api` 작업과 `admin-api` 작업은 상호 모듈 수정을 절대 금지한다.
- 의존성 추가/변경이 필요하면 작업을 멈추고 사용자 허락을 먼저 받는다.

## Skill 라우팅
- 아키텍처/모듈 구조/의존성 방향 변경: `$core-architecture`
- 프론트엔드(`moamoa-web`, `moamoa-admin/admin-web`) 작업: `$web-frontend-architecture`
- API 계약 변경: `$api-contract`
- R2DBC/DatabaseClient/트랜잭션 경계 작업: `$persistence-r2dbc`
- 테스트 추가/수정: `$testing-style`

## 기존 전용 에이전트
- 기술 블로그 크롤러: `/Users/taewan/dev/moamoa/.codex/agents/tech-blog-crawler.md`
- 테스트 작성: `/Users/taewan/dev/moamoa/.codex/agents/write-test.md`
