# <img src="https://i.imgur.com/CHYokw0.png" alt="모아모아" width="200px" />

기술블로그 종합 서비스 **모아모아** 레포지토리입니다.

## 링크

- 서비스 URL : https://moamoa.dev

## 기술 스택

- server : Kotlin, Spring, Jpa, Mysql, Redis
- front : TypeScript, React, Vite

## 모듈 설명

- `moamoa-core/core-api`: 모아모아 메인 API 서버
- `moamoa-core/core-batch`: 모아모아 배치 작업 서버
- `moamoa-core/core-tech-blog`: 기술 블로그 도메인 공통 모델입
- `moamoa-core/core-shared`: 코어 공통 계약(인터페이스/타입)모듈
- `moamoa-infra/*`: 외부 연동 구현(Redis, JWT, Mailgun, Tech Blog 수집 등)
- `moamoa-support/*`: 공통 지원 모듈(API 문서, 로깅, 템플릿, 테스트 등)
- `moamoa-web`: 사용자 웹 프론트엔드
- `moamoa-admin/admin-api`: 관리자 API 서버
- `moamoa-admin/admin-web`: 관리자 웹 프론트엔드
