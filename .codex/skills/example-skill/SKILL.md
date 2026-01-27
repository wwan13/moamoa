---
name: example-skill
description: Spring Boot + Kotlin backend coding conventions
---

이 스킬은 Spring Boot와 Kotlin 기반 백엔드 코드를 작성할 때
다음 규칙을 따르도록 합니다.

- 엔티티를 API 응답으로 직접 반환하지 않는다
- 트랜잭션은 서비스 레이어에서만 사용한다
- 테스트는 JUnit5 + MockK를 사용한다