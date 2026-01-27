---
name: tech-blog-crawl-jsoup
description: "moamoa 기술 블로그 크롤러를 HTML/JSoup 기반으로 추가·수정할 때 사용. TechBlogSource 구현, 목록/상세 페이지 파싱, paging 중단 조건, 썸네일/태그 추출 등 JSoup 크롤링 작업에 적용."
---

# Tech Blog Crawl (JSoup)

JSoup으로 HTML을 파싱하는 tech blog 소스를 `moamoa-infra:infra-tech-blog`에 구현한다.

## 필수 경로
- 인터페이스/도메인: `moamoa-core/core-tech-blog/src/main/kotlin/server/techblog/TechBlogSource.kt`, `TechBlogPost.kt`
- 구현체 위치: `moamoa-infra/infra-tech-blog/src/main/kotlin/server/techblogs/<blogKey>/<BlogName>Source.kt`
- 공통 유틸: `moamoa-infra/infra-tech-blog/src/main/kotlin/server/utill/JsoupUtils.kt`

## 구현 체크리스트
- `TechBlogSource` 구현 + `@Component` 유지.
- 클래스명은 `XxxSource`, 패키지는 `server.techblogs.<blogKey>`로 맞춘다.
- `getPosts(size: Int?)`는 `Flow<TechBlogPost>`를 반환한다.
- 게시글 `key`는 URL에서 안정적으로 추출(중복 방지 기준).
- `publishedAt`이 없으면 `LocalDateTime.MIN`으로 기본값 처리.
- 썸네일이 없으면 기본 이미지 URL을 지정.

## 크롤링 기본 흐름
1. 목록 페이지 URL 빌더 작성: `buildListUrl(page: Int)`.
2. `fetchWithPaging(targetCount = size, buildUrl = ::buildListUrl, timeoutMs = ...)`로 목록 파싱.
3. HTML 파싱은 `Jsoup` 셀렉터로 필요한 필드만 추출.
4. 페이지 종료 조건은 `PagingFinishedException`으로 제어.
5. 상세 페이지 태그/카테고리 보강이 필요하면 `flatMapMerge`로 후처리.

## 페이징/중단 규칙
- 목록이 비면 `PagingFinishedException()`을 던져 중단.
- 동일 페이지 반복(시그니처/중복 감지) 시 중단.
- key 중복 제거 후 해당 페이지에서 새 글이 없으면 중단.
- `JsoupUtils.jsoup`은 403/404/4xx 응답 시 자동으로 `PagingFinishedException`을 던진다.

## 추천 유틸 사용
- 목록 파싱: `server.utill.fetchWithPaging` (JsoupUtils)
- 상세 문서 요청: `server.utill.jsoup(url, timeoutMs)`
- 태그 정규화: 필요 시 `server.utill.normalizeTagTitle`

## 구현 시 주의점
- selector는 최소 범위로 좁히고, `absUrl("href")`로 절대 URL을 사용.
- `URI(url).path` 기반 key 추출은 `trimEnd('/')`를 먼저 적용.
- 날짜 포맷은 사이트별로 다르므로 `DateTimeFormatter` 다중 시도 패턴을 사용.
- 상세 요청은 `flatMapMerge(concurrency = 10)` 수준으로 과도한 병렬을 피한다.

## 예시 흐름 참고
- 목록+상세 파싱: `server.techblogs.kakaopay.KakaoPaySource`
- 반복 페이지 감지: `server.techblogs.buzzvil.BuzzvilSource`
- 태그 보강: `server.techblogs.woowabros.WoowabrosSource`
