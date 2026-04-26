# AGENT 운영 진입점

이 문서는 `moamoa-frontend`의 에이전트 라우팅과 강제 제약을 정의한다.
상세 규칙은 `guides/*.md`, 모듈별 실행 규칙은 `profiles/*.md`, 프론트 작업 절차는 `../skills/*/SKILL.md`를 따른다.

## Guides
- 공통 작업 규칙: `/Users/taewan/dev/moamoa/moamoa-frontend/.codex/agents/guides/convention.md`
- 모듈 경계 규칙: `/Users/taewan/dev/moamoa/moamoa-frontend/.codex/agents/guides/module-boundary.md`
- 기능 추가 워크플로우: `/Users/taewan/dev/moamoa/moamoa-frontend/.codex/agents/guides/feature-workflow.md`
- 커밋 규칙: `/Users/taewan/dev/moamoa/moamoa-frontend/.codex/agents/guides/commit.md`
- 프론트엔드 규칙: `/Users/taewan/dev/moamoa/moamoa-frontend/.codex/agents/guides/frontend_convention.md`

## Agent Profiles
- core-web: `/Users/taewan/dev/moamoa/moamoa-frontend/.codex/agents/profiles/moamoa-web-agent.md`
- admin-web: `/Users/taewan/dev/moamoa/moamoa-frontend/.codex/agents/profiles/admin-web-agent.md`

## 강제 제약
- 작업 시작 시 반드시 타깃 모듈 하나를 고정한다.
- 타깃 모듈 외 경로 수정은 금지한다.
- `core-web` 작업과 `admin-web` 작업은 상호 모듈 수정을 절대 금지한다.
- 의존성 추가/변경이 필요하면 작업을 멈추고 사용자 허락을 먼저 받는다.

## Skill 라우팅
- 프론트엔드(`moamoa-frontend/core-web`, `moamoa-frontend/admin-web`) 작업: `$web-frontend-architecture`

## 전용 에이전트
- React 전용: `/Users/taewan/dev/moamoa/moamoa-frontend/.codex/agents/react-specialist.toml`
- TypeScript 전용: `/Users/taewan/dev/moamoa/moamoa-frontend/.codex/agents/typescript-pro.toml`
