---
name: tech-blog-crawl-api
description: "moamoa 기술 블로그 크롤러를 API(WebClient/JSON) 기반으로 추가·수정할 때 사용. TechBlogSource 구현, API 페이징, 응답 검증/중단 처리, 태그/썸네일 매핑 등 API 크롤링 작업에 적용."
---

# Tech Blog Crawl (API)

API 호출(WebClient)로 tech blog 소스를 `moamoa-infra:infra-tech-blog`에 구현한다.

## 필수 경로
- 인터페이스/도메인: `moamoa-core/core-tech-blog/src/main/kotlin/server/techblog/TechBlogSource.kt`, `TechBlogPost.kt`
- 구현체 위치: `moamoa-infra/infra-tech-blog/src/main/kotlin/server/techblogs/<blogKey>/<BlogName>Source.kt`
- WebClient 설정: `moamoa-infra/infra-tech-blog/src/main/kotlin/server/config/TechBlogClientConfig.kt`
- 공통 유틸: `moamoa-infra/infra-tech-blog/src/main/kotlin/server/utill/WebClientUtils.kt`

## 구현 체크리스트
- `TechBlogSource` 구현 + `@Component` 유지.
- 클래스명은 `XxxSource`, 패키지는 `server.techblogs.<blogKey>`로 맞춘다.
- `getPosts(size: Int?)`는 `Flow<TechBlogPost>`를 반환한다.
- 게시글 `key`는 API 필드 또는 URL 기반으로 안정적으로 생성.
- `publishedAt`이 없으면 `LocalDateTime.MIN`으로 기본값 처리.
- 썸네일이 없으면 기본 이미지 URL을 지정.

## API 크롤링 기본 흐름
1. `fetchWithPaging(pageSize, targetCount = size, startPage = ...)` 사용.
2. `webClient.get().uri { ... }.retrieve()`로 호출.
3. 페이징 중단 조건은 `handlePagingFinished()` + `validateIsPagingFinished()`로 통일.
4. 응답 DTO를 data class로 정의해 파싱.
5. 필요 시 상세 API 호출로 태그/카테고리 보강.

## 페이징/중단 규칙
- 400/404 응답은 `handlePagingFinished()`로 종료 처리.
- 리스트가 비면 `validateIsPagingFinished()`로 종료 처리.
- API가 성공 플래그를 제공하면 false/null일 때 즉시 예외 처리.

## 추천 유틸 사용
- 페이징: `server.utill.fetchWithPaging` (WebClientUtils)
- 페이징 종료: `handlePagingFinished`, `validateIsPagingFinished`
- 태그 정규화: `server.utill.normalizeTagTitle`

## 구현 시 주의점
- `awaitSingle()` 사용으로 coroutine 호환 유지.
- `pageSize`는 API 제한에 맞추되 과도하게 크게 설정하지 않는다.
- 날짜/시간은 `Instant`/`ZonedDateTime` 등으로 변환해 `LocalDateTime`으로 맞춘다.
- 썸네일/본문 요약이 HTML이면 `Jsoup.parse(...).text()`로 정리한다.

## 예시 흐름 참고
- 단순 API 목록: `server.techblogs.toss.TossSource`
- 목록+상세 보강: `server.techblogs.naver.NaverSource`
- WP JSON API: `server.techblogs.gabia.GabiaSource`
