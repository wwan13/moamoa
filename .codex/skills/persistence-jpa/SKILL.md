---
name: persistence-jpa
description: "moamoa 퍼시스턴스 규약. 현재 기준은 MVC + JPA + JDBC query 패턴을 따른다."
---

# Persistence (JPA/JDBC)

현재 기준은 JPA/JDBC다.

## 현재 기준
- 엔티티 쓰기 경로는 Spring Data JPA + dirty checking 사용.
- 복잡 조회/집계는 `NamedParameterJdbcTemplate` SQL query 허용.
- reactive repository / R2DBC DatabaseClient 패턴은 신규로 사용하지 않는다.

## 엔티티/리포지토리
- 엔티티는 `data class`보다 일반 `class` 사용.
- 변경 가능한 필드만 `var private set`, 변경 메서드(`update...`)로 수정.
- 변경 없는 필드는 `val` 유지.
- `findById`는 가능하면 `findByIdOrNull` 사용.

## 트랜잭션
- core-api 현재 기준: 함수형 `Transactional` DSL 우선.
- 쓰기 + outbox 이벤트 등록은 동일 트랜잭션에서 처리.
- 전파 옵션이 필요하면 `transactional(propagation = ...)`을 명시.

## SQL/쿼리 주의
- query 계층의 다중 DB 접근은 기본 직렬 처리.
- 컬럼 alias는 mapper에서 사용하는 필드명과 정확히 일치시킨다.

## 금지
- reactive repository + JPA 혼용 금지.
- 서버 계층에서 `Mono/Flux` 기반 DB 접근 신규 도입 금지.
