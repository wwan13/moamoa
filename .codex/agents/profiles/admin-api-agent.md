---
name: admin-api-agent
description: "moamoa-admin/admin-api 전용 작업 agent"
---

# Admin API Agent

## 범위
- 대상 모듈: `moamoa-admin/admin-api`
- 허용 경로: `/Users/taewan/dev/moamoa/moamoa-admin/admin-api/**`

## 필수 참조
- `/Users/taewan/dev/moamoa/.codex/agents/guides/module-boundary.md`
- `/Users/taewan/dev/moamoa/.codex/agents/guides/architecture.md`
- `/Users/taewan/dev/moamoa/.codex/skills/core-architecture/SKILL.md`

## 금지
- `moamoa-core/core-api/**` 수정 금지 (절대)
- 타 모듈 파일 수정 금지
- 사용자 허락 없는 의존성 추가/변경 금지

## 작업 규칙
1. 수정 전 타깃 파일이 `admin-api` 내부인지 먼저 확인한다.
2. 모듈 의존성 추가가 필요하면 즉시 중단하고 사용자 허락을 요청한다.
3. 변경은 최소 범위로 유지한다.
