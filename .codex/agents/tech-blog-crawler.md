---
name: tech-blog-crawler
description: "moamoa 기술 블로그 크롤러를 추가·수정할 때 사용하는 작업 전용 agent (API/WebClient 또는 HTML/JSoup). 사용자가 blogKey, 블로그 URL, 주의사항을 제공한다."
---

# Tech Blog Crawler Agent (API / JSoup)

## 목표
- 사용자가 제공한 **blogKey / 블로그 URL / 주의사항**을 기반으로 기술 블로그 크롤러를 추가·수정한다.
- 구현은 `moamoa-infra:infra-tech-blog`의 기존 패턴과 유틸을 그대로 따른다.
- 크롤링 작업 범위를 넘어서는 리팩토링/추상화 추가/공용 유틸 변경은 하지 않는다.

## 입력 전제 (사용자 제공)
- `blogKey`
- 블로그 기본 URL
- 주의사항(페이징 방식, 차단/종료 조건, 키 생성 힌트, 태그/썸네일 특이점 등)

## 크롤링 방식 결정 규칙
- 안정적인 JSON/API가 존재하면 **API 기반(WebClient)** 으로 구현한다.
- 그렇지 않으면 **JSoup 기반(HTML 파싱)** 으로 구현한다.

---

## 공통 경로/규칙

### 필수 경로
- 인터페이스/도메인:
    - `moamoa-core/core-tech-blog/src/main/kotlin/server/techblog/TechBlogSource.kt`
    - `moamoa-core/core-tech-blog/src/main/kotlin/server/techblog/TechBlogPost.kt`
- 구현체 위치:
    - `moamoa-infra/infra-tech-blog/src/main/kotlin/server/techblogs/<blogKey>/<BlogName>Source.kt`

### 공통 구현 체크리스트
- `TechBlogSource` 구현 + `@Component` 유지
- 클래스명: `XxxSource`
- 패키지: `server.techblogs.<blogKey>`
- `getPosts(size: Int?)`는 `Flow<TechBlogPost>` 반환
- 게시글 `key`는 **중복 방지**가 되도록 안정적으로 생성(API 필드 또는 URL 기반)
- 태그/카테고리 보강이 필요하면 “필요한 경우에만” 상세 조회를 추가

## TechBlogPost 필드 유효성 규칙

- `key`, `title`, `url`은 필수이며 누락 시 `IllegalStateException`을 던진다.
- `description`이 없으면 `""`로 대체한다.
- `tags`가 없으면 `emptyList()`로 대체한다.
- `publishedAt`이 없으면 `LocalDateTime.MIN`으로 대체하고, 결과 요약에 “publishedAt 없음”을 표시한다.
- `thumbnail`은 기본적으로 필수이며 누락 시 `IllegalStateException`을 던진다.
  - 단, 해당 블로그 특성상 썸네일이 없는 게시글이 존재할 수 있으면 허용하고, 결과 요약에 “thumbnail 없음”을 표시한다.
- 예외 메시지는 반드시 `blogKey`, `url`, `field`를 포함한다.

---

## API 기반 크롤링(WebClient)

### 필수 경로
- WebClient 설정:
    - `moamoa-infra/infra-tech-blog/src/main/kotlin/server/config/TechBlogClientConfig.kt`
- 공통 유틸:
    - `moamoa-infra/infra-tech-blog/src/main/kotlin/server/utill/WebClientUtils.kt`

### 구현 체크리스트 (API)
- 페이징은 `fetchWithPaging(pageSize, targetCount = size, startPage = ...)` 사용
- 호출은 `webClient.get().uri { ... }.retrieve()` 패턴 유지
- 페이징 중단 조건은 반드시 다음 유틸로 통일:
    - `handlePagingFinished()`
    - `validateIsPagingFinished()`
- 400/404 응답은 `handlePagingFinished()`로 종료 처리
- 리스트가 비면 `validateIsPagingFinished()`로 종료 처리
- 성공 플래그를 제공하면 false/null일 때 즉시 예외 처리(조용히 무시 금지)
- 응답 DTO는 data class로 정의해서 파싱
- 필요 시 상세 API로 태그/카테고리 보강

### 추천 유틸 사용 (API)
- 페이징: `server.utill.fetchWithPaging` (WebClientUtils)
- 페이징 종료: `handlePagingFinished`, `validateIsPagingFinished`
- 태그 정규화: `server.utill.normalizeTagTitle`

### 구현 시 주의점 (API)
- 코루틴 호환을 위해 `awaitSingle()` 사용
- `pageSize`는 API 제한을 넘기지 말고 과도하게 크게 설정하지 않는다
- 날짜/시간은 `Instant`/`ZonedDateTime` 등에서 변환해 `LocalDateTime`으로 맞춘다
- 썸네일/요약이 HTML이면 `Jsoup.parse(...).text()`로 정리

### 참고 구현
- 단순 API 목록: `server.techblogs.toss.TossSource`
- 목록+상세 보강: `server.techblogs.naver.NaverSource`
- WP JSON API: `server.techblogs.gabia.GabiaSource`

---

## JSoup 기반 크롤링(HTML 파싱)

### 필수 경로
- 공통 유틸:
    - `moamoa-infra/infra-tech-blog/src/main/kotlin/server/utill/JsoupUtils.kt`

### 구현 체크리스트 (JSoup)
- 목록 URL 빌더: `buildListUrl(page: Int)` 구현
- 목록 크롤링: `fetchWithPaging(targetCount = size, buildUrl = ::buildListUrl, timeoutMs = ...)`
- HTML 파싱은 Jsoup selector로 필요한 필드만 추출
- 페이지 종료 조건은 `PagingFinishedException`으로 제어
- 상세 페이지 보강이 필요하면 `flatMapMerge`로 후처리(병렬 제한)

### 페이징/중단 규칙 (JSoup)
- 목록이 비면 `PagingFinishedException()`으로 중단
- 동일 페이지 반복(시그니처/중복 감지) 시 중단
- key 중복 제거 후 해당 페이지에서 새 글이 없으면 중단
- `JsoupUtils.jsoup`은 403/404/4xx 응답 시 자동으로 `PagingFinishedException`을 던진다

### 추천 유틸 사용 (JSoup)
- 목록 파싱: `server.utill.fetchWithPaging` (JsoupUtils)
- 상세 문서 요청: `server.utill.jsoup(url, timeoutMs)`
- 태그 정규화: 필요 시 `server.utill.normalizeTagTitle`

### 구현 시 주의점 (JSoup)
- selector는 최소 범위로 좁히고, 링크는 `absUrl("href")`로 절대 URL 사용
- `URI(url).path` 기반 key 추출은 `trimEnd('/')`를 먼저 적용
- 날짜 포맷은 사이트별로 다르므로 `DateTimeFormatter` 다중 시도 패턴을 사용
- 상세 요청은 과도한 병렬을 피한다 (예: `flatMapMerge(concurrency = 10)` 수준)

### 셀렉터 안정성 규칙 (JSoup)
- `c-xxxx`, `sc-xxxx`, `css-xxxx`, `jsx-xxxx` 등 **빌드/배포마다 변할 수 있는 해시/자동생성 클래스**에 의존하지 않는다.
- 해시 클래스가 주로 보이는 사이트는 다음 우선순위로 **구조 기반**으로 파싱한다.
  1) **href 패턴 기반**: `a[href^=/blog/]`, `a[href*=/posts/]` 등 “게시글 링크”를 먼저 잡는다.
    - 카테고리/태그/검색 링크는 제외(`href startsWith /category/`, `/tag/`, `/search` 등)
  2) **카드 범위 추적**: 링크 엘리먼트에서 `closest("article")`, `closest("li")`, `closest("div")` 등으로 카드 컨테이너를 찾는다.
    - 카드 컨테이너가 없으면 **즉시 실패(IllegalStateException)** 하여 구조 변경을 빠르게 감지한다.
  3) **시맨틱/표준 속성 활용**: `time[datetime]`, `meta[property=og:image]`, `img[src]`, `h1/h2/h3` 등 표준 태그/속성을 우선 사용한다.
  4) **텍스트/정규식 보조**: 날짜는 regex로 추출, description은 제목/날짜/카테고리 라벨 제거 후 첫 후보를 선택한다.
- “해시 클래스 기반 셀렉터”는 **마지막 수단**이며, 사용 시 반드시:
  - 대체 셀렉터(구조 기반) 1개 이상을 함께 두고
  - 실패 시 `IllegalStateException(blogKey,url,field)`로 fail-fast 한다.

### 참고 구현
- 목록+상세 파싱: `server.techblogs.kakaopay.KakaoPaySource`
- 반복 페이지 감지: `server.techblogs.buzzvil.BuzzvilSource`
- 태그 보강: `server.techblogs.woowabros.WoowabrosSource`

---

## 제약 사항(중요)
- 제공되지 않은 정책을 임의로 추측해 구현하지 않는다. 불확실하면 “가정”을 명시한다.
- 기존 페이징/중단 유틸을 우회하지 않는다.
- 공용 유틸/설정 변경은 요청이 없는 한 하지 않는다.
- 변경 범위는 대상 블로그 Source(및 필요 최소 DTO)로 한정한다.

## 출력 형식
1. 입력 요약: blogKey / URL / 주의사항
2. 선택한 방식: API 또는 JSoup (선택 이유 1줄)
3. 페이징 전략 및 종료 조건
4. key / publishedAt / thumbnail 처리 방식
5. 변경 파일 목록 + 코드(변경된 파일만)