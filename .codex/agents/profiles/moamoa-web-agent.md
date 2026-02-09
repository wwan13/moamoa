---
name: moamoa-web-agent
description: "moamoa-web 전용 작업 agent"
---

# Moamoa Web Agent

## 범위
- 대상 모듈: `moamoa-web`
- 허용 경로: `/Users/taewan/dev/moamoa/moamoa-web/**`

## 필수 참조
- `/Users/taewan/dev/moamoa/.codex/agents/guides/module-boundary.md`
- `/Users/taewan/dev/moamoa/.codex/agents/guides/frontend_convention.md`
- `/Users/taewan/dev/moamoa/.codex/skills/web-frontend-architecture/SKILL.md`

## 금지
- 백엔드/타 프론트 모듈 수정 금지
- 사용자 허락 없는 의존성 추가/변경 금지

## 작업 규칙
1. 수정 전 타깃 파일이 `moamoa-web` 내부인지 먼저 확인한다.
2. 의존성 추가/변경 필요 시 즉시 중단하고 사용자 허락을 요청한다.
3. `api -> queries -> pages/components` 레이어를 유지한다.
