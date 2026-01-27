---
name: persistence-r2dbc
description: "moamoa의 R2DBC 퍼시스턴스, DatabaseClient 쿼리, 트랜잭션 경계 작업 시 사용. core-api/admin/batch에 적용."
---

# Persistence (R2DBC)

R2DBC + DatabaseClient + 코루틴 브리지 패턴을 유지한다.

## 쿼리 패턴
- 다건 조회: `DatabaseClient.sql(...).bind(...).map { ... }.all().asFlow()`
- 단건/집계:
    - 결과가 반드시 1건이면 `awaitSingle()`
    - 결과가 없을 수 있으면 `awaitSingleOrNull()` 후 도메인 예외/nullable로 처리
- 쿼리 서비스에서 raw SQL 작성 + Row 매핑

## 매핑 헬퍼
- `server.infra.db.Extensions`의 `Row.getOrDefault(...)`, `Row.getInt01(...)` 사용

## 트랜잭션
- 트랜잭션은 반드시 application 레이어(service)에서만 사용한다.
- `@Transactional`은 사용하지 않고, 람다 형식의 `server.infra.db.Transactional`만 사용한다.
- 여러 write를 하나의 원자적 작업으로 묶어야 할 때만 트랜잭션을 적용한다.
- 필요 시 전파 옵션을 명시적으로 선택한다.

## 금지
- WebFlux 경로에서 blocking JDBC 호출 금지
- R2DBC 흐름에 blocking 라이브러리 혼용 금지
- reactive/flow 결과를 무분별하게 `toList()/block()`로 강제 수집하지 않는다 (필요한 레이어에서만 명시적으로 처리)

## 권장 위치
- 읽기 모델: `feature/*/query`
- 쓰기 모델: `feature/*/command`
- DB 공통 유틸: `server.infra.db`