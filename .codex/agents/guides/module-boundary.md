# Module Boundary

이 문서는 에이전트 작업 시 모듈 수정 경계를 강제한다.

## 공통 강제 규칙
- 한 작업은 지정된 타깃 모듈 경로 내부에서만 수정한다.
- 타깃 모듈 외 파일 수정은 금지한다.
- 의존성 추가/변경(`build.gradle.kts`, `settings.gradle.kts`, 버전 카탈로그, 모듈 dependency block`)이 필요하면 즉시 중단하고 사용자 허락을 먼저 받는다.
- 허락 없이 신규 모듈 생성/모듈 간 의존성 추가/모듈 구조 변경을 하지 않는다.

## 타깃별 수정 허용 경로
- core-api 작업
  - 허용: `moamoa-core/core-api/**`
  - 금지: `moamoa-admin/**`, `moamoa-web/**`, `moamoa-infra/**`, `moamoa-support/**`, `moamoa-core/core-batch/**`
- admin-api 작업
  - 허용: `moamoa-admin/admin-api/**`
  - 금지: `moamoa-core/**`, `moamoa-web/**`, `moamoa-infra/**`, `moamoa-support/**`
- moamoa-web 작업
  - 허용: `moamoa-web/**`
  - 금지: `moamoa-core/**`, `moamoa-admin/**`, `moamoa-infra/**`, `moamoa-support/**`
- admin-web 작업
  - 허용: `moamoa-admin/admin-web/**`
  - 금지: `moamoa-core/**`, `moamoa-web/**`, `moamoa-infra/**`, `moamoa-support/**`, `moamoa-admin/admin-api/**`

## core-api / admin-api 상호 금지 규칙
- core-api 작업 중 `moamoa-admin/admin-api/**` 수정 금지.
- admin-api 작업 중 `moamoa-core/core-api/**` 수정 금지.
- 두 모듈 동시 수정이 요구되면 단일 작업으로 진행하지 말고, 사용자 허락을 받아 작업을 분리한다.
