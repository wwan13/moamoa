# <img src="https://i.imgur.com/CHYokw0.png" alt="모아모아" width="200px" />

기술블로그 종합 서비스 **모아모아** 레포지토리입니다.

## 링크

- 서비스 URL : https://moamoa.dev

## 기술 스택

- server : Kotlin, Spring, Jpa, Mysql, Redis
- front : TypeScript, React, Vite

## 모듈 설명

- `moamoa-frontend/core-web`: 사용자 웹 프론트엔드
- `moamoa-frontend/admin-web`: 관리자 웹 프론트엔드
- `moamoa-backend/admin`: 관리자 API 서버
- `moamoa-backend/core/core-api`: 모아모아 메인 API 서버
- `moamoa-backend/core/core-batch`: 모아모아 배치 작업 서버
- `moamoa-backend/core/core-enum`: 백엔드 공통 enum 모듈
- `moamoa-backend/infra/tech-blog-*`: 기술 블로그 크롤러 계약/api, HTTP 구현, Jsoup 구현, starter
- `moamoa-backend/infra/cache-*`: cache 계약/구현(redis, caffeine, resilient)/starter
- `moamoa-backend/infra/queue-*`: queue 계약/redis 구현/starter
- `moamoa-backend/infra/set-*`: set 계약/redis 구현/starter
- `moamoa-backend/infra/lock-*`: lock 계약/redisson·local·resilient 구현/starter
- `moamoa-backend/infra/messaging-*`: messaging 계약/redis 구현/starter
- `moamoa-backend/infra/token-*`: token 계약/jwt 구현/starter
- `moamoa-backend/infra/password-*`: password 계약/crypto 구현/starter
- `moamoa-backend/infra/mail-*`: mail 계약/mailgun 구현/starter
- `moamoa-backend/support/*`: 공통 지원 모듈(API 문서, 로깅, 템플릿, 테스트 등)
