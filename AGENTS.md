# moamoa Agent Rules

## Backend Package Root
- `moamoa-admin/admin-api`의 코틀린 패키지 루트는 항상 `server.admin`을 사용한다.
- `moamoa-core/core-batch`의 코틀린 패키지 루트는 항상 `server.batch`를 사용한다.
- `moamoa-core/core-api`의 코틀린 패키지 루트는 기존대로 `server`를 사용한다.

## Test Fixture Package
- fixture는 모듈의 패키지 루트와 동일한 prefix를 사용한다.
- `admin-api` fixture: `server.admin.fixture`
- `core-batch` fixture: `server.batch.fixture`
- `core-api` fixture: `server.fixture`
