# moamoa-crawler

`moamoa-crawler`는 기술 블로그별 Python crawler를 실행하는 독립 패키지입니다.
source는 `sources/{key}.py` 파일로 자동 발견됩니다.

## 시작하는 방법

### 1. 설치

```bash
cd moamoa-crawler
python3 -m venv .venv
.venv/bin/python -m pip install -e ".[fetchers]"
.venv/bin/scrapling install
```

`.[fetchers]`와 `scrapling install`은 Scrapling browser crawler를 사용하는 source에 필요합니다.
API, RSS, 정적 HTML source만 실행할 때는 browser가 필요하지 않지만, 전체 source를 동일한 환경에서 돌리려면 위 방식으로 설치하는 것이 가장 단순합니다.

### 2. 프로세스 실행

설치한 console script로 실행합니다.

```bash
.venv/bin/moamoa-crawler
```

옵션 없이 실행하면 기본적으로 HTTP API와 Redis Stream consumer가 함께 실행됩니다.
기본 API 주소는 `127.0.0.1:8765`이고, 기본 request stream은 `tech-blog:crawl:requests`, response stream은 `tech-blog:crawl:response`입니다.
Redis는 `REDIS_URL`이 없으면 `redis://127.0.0.1:6379/0`으로 연결합니다.

개발 중 package 설치 없이 바로 실행해야 하면 `moamoa-crawler` 디렉터리에서 module로 실행할 수도 있습니다.

```bash
.venv/bin/python -m crawler
```

### 3. HTTP API 호출

API 호출:

```bash
curl http://127.0.0.1:8765/health
curl -X POST 'http://127.0.0.1:8765/crawl?key=musinsa&size=30'
curl -X POST http://127.0.0.1:8765/crawl \
  -H 'Content-Type: application/json' \
  -d '{"key":"toss","size":30}'
```

`POST /crawl`은 요청을 내부 FIFO 큐에 넣고 crawler worker가 하나씩 실행합니다.
응답은 해당 요청의 crawl이 끝난 뒤 같은 연결로 반환됩니다.
단일 source를 확인할 때도 HTTP API에 `key`와 `size`를 넘겨 실행합니다.

### 4. Redis Stream 이벤트 발행

Redis 연결은 `REDIS_URL`을 우선 사용하고, 없으면 `.env` 또는 환경 변수의 `REDIS_HOST`, `REDIS_PORT`, `REDIS_DATABASE`, `REDIS_PASSWORD`를 조합합니다.

이벤트 발행 예시:

```bash
redis-cli XADD tech-blog:crawl:requests '*' key toss size 30
redis-cli XADD tech-blog:crawl:requests '*' key '*' size 1
redis-cli XADD tech-blog:crawl:requests '*' payload '{"key":"musinsa","size":30}'
```

consumer는 성공한 메시지를 ACK 하고, 크롤링이 완료되면 같은 결과 payload를 `tech-blog:crawl:response` stream에 `payload` 필드로 발행합니다.
예:

```bash
redis-cli XREAD COUNT 1 STREAMS tech-blog:crawl:response 0-0
```

응답 이벤트의 `payload` 값은 HTTP `POST /crawl` 성공 응답과 같은 JSON입니다.
실패 메시지는 기본적으로 ACK 하지 않습니다.

### 5. 동시 요청 처리

HTTP API와 Redis Stream 이벤트 요청은 같은 내부 FIFO 큐로 처리합니다.
따라서 API 요청과 이벤트 요청이 동시에 들어와도 crawler worker가 하나씩 순서대로 실행합니다.

## Source별 크롤링 방식

| source key | 블로그 | 방식 | 설명 |
| --- | --- | --- | --- |
| `ab180` | AB180 Engineering | RSS | GitHub에 공개된 RSS XML을 `urllib`로 가져와 item을 파싱합니다. |
| `ably` | Ably Tech Blog | API | Next.js `buildId`를 추출한 뒤 `_next/data` JSON을 호출합니다. |
| `banksalad` | Banksalad Tech Blog | 정적 HTML | 목록 HTML의 링크를 파싱하고 `/tech/...` URL 패턴만 수집합니다. |
| `buzzvil` | Buzzvil Tech Blog | 정적 HTML | 목록 HTML 안의 embedded JSON slug/title을 우선 파싱하고, 실패 시 링크/정규식 fallback을 사용합니다. |
| `com2us` | Com2uS Tech Blog | 정적 HTML | WordPress 태그 목록 HTML에서 게시글 링크를 수집합니다. |
| `daangn` | Daangn Tech Blog | Browser | Medium publication `/all` 페이지를 Scrapling `StealthyFetcher`로 렌더링/스크롤한 뒤 링크를 수집합니다. |
| `danawa` | Danawa Lab | 정적 HTML | GitHub Pages 목록 HTML에서 날짜 기반 게시글 URL을 수집합니다. |
| `delightroom` | DelightRoom | Browser | Medium publication `/all` 페이지를 Scrapling으로 렌더링/스크롤합니다. |
| `devocean` | DEVOCEAN | 정적 HTML | 목록 HTML의 `goDetail` onclick 값을 정규식으로 파싱합니다. |
| `elevenst` | 11st Tech Blog | 정적 HTML | GitHub Pages 목록 HTML에서 게시글 링크를 수집합니다. |
| `flex` | flex Tech Blog | 정적 HTML | 카테고리 목록 HTML에서 날짜 기반 `/blog/yyyy/mm/dd/...` 링크를 수집합니다. |
| `gabia` | Gabia Library | API | WordPress REST API `wp-json/wp/v2/posts`를 호출합니다. |
| `gccompany` | GC Company Tech Blog | Browser | Medium 스타일 `/all` 페이지를 Scrapling으로 렌더링/스크롤합니다. |
| `goorm` | goorm TechBlog | 정적 HTML | WordPress 목록 HTML에서 게시글 링크를 수집합니다. |
| `hyperconnect` | Hyperconnect Tech Blog | 정적 HTML | GitHub Pages 목록 HTML에서 날짜 기반 게시글 URL을 수집합니다. |
| `kakao` | Kakao Tech | API | `tech.kakao.com/api/v2/posts` JSON API를 호출합니다. |
| `kakaobank` | KakaoBank Tech Blog | 정적 HTML | 목록 HTML에서 `/posts/...` 또는 루트 게시글 URL을 수집합니다. |
| `kakaomobility` | Kakao Mobility Developers | 정적 데이터 청크 | `/techblogs` 페이지가 preload 하는 VitePress `techblogs.data.*.js` 청크를 파싱하며 payload key는 `kakaoMobility`를 사용합니다. |
| `kakaopay` | KakaoPay Tech Blog | 정적 HTML | 목록 HTML에서 `/post/...` 링크를 수집합니다. |
| `kakaostyle` | Kakao Style Dev Blog | 정적 HTML | 목록 HTML에서 한국어 블로그 게시글 링크를 수집합니다. |
| `kream` | KREAM Tech Blog | Browser | Medium publication `/all` 페이지를 Scrapling으로 렌더링/스크롤합니다. |
| `ktcloud` | kt cloud Tech Blog | 정적 HTML | 목록 HTML에서 `/entry/...` 링크를 수집합니다. |
| `kurly` | Kurly Tech Blog | 정적 HTML | 목록 HTML에서 `/blog/...` 링크를 수집합니다. |
| `kyobodts` | Kyobo DTS Tech Blog | 정적 HTML | 목록 HTML에서 날짜 기반 게시글 URL을 수집합니다. |
| `line` | LINE ENGINEERING | 정적 HTML | `/ko/blog` 목록 HTML에서 상세 링크를 수집합니다. |
| `lotteon` | LotteON Tech Blog | Browser | Medium 스타일 `/all` 페이지를 Scrapling으로 렌더링/스크롤합니다. |
| `miridih` | Miridih Blog | Browser | Medium publication `/all` 페이지를 Scrapling으로 렌더링/스크롤합니다. |
| `musinsa` | MUSINSA Tech Blog | Browser | `/all` 페이지를 Scrapling `StealthyFetcher`로 렌더링하고 post URL 패턴을 수집합니다. |
| `myrealtrip` | How we build Myrealtrip | RSS + Browser | Medium RSS와 tag RSS를 먼저 파싱하고, Scrapling으로 Medium subpage 링크도 보강합니다. |
| `naver` | NAVER D2 | API | `d2.naver.com/api/v1/contents` JSON API를 호출합니다. |
| `naverplace` | NAVER Place Dev Blog | Browser | Medium publication `/all` 페이지를 Scrapling으로 렌더링/스크롤합니다. |
| `nds` | NDS Tech Blog | 정적 HTML | 목록 HTML에서 `/blog/.../{id}` 형식 링크를 수집합니다. |
| `nhncloud` | NHN Cloud Meetup | API | `meetup.nhncloud.com/tcblog/v1.0/posts` JSON API를 호출합니다. |
| `oliveyoung` | Olive Young Tech | 정적 HTML | 목록 HTML에서 날짜 기반 게시글 URL을 수집합니다. |
| `postype` | Postype Team | API | Postype 공개 API의 channel activity posts를 호출합니다. |
| `rapportlabs` | Rapport Labs Tech Blog | 정적 HTML | 목록 HTML에서 게시글 링크를 수집합니다. |
| `remember` | Remember Tech Blog | Browser | Medium 스타일 `/all` 페이지를 Scrapling으로 렌더링/스크롤합니다. |
| `samosam` | 3o3 Tech Blog | 정적 HTML | tech 태그 목록 HTML에서 게시글 링크를 수집합니다. |
| `samsung` | Samsung Tech Blog | 정적 HTML | 목록 HTML에서 `/blog/article/...` 링크를 수집합니다. |
| `saramin` | Saramin Tech Blog | 정적 HTML | GitHub Pages 목록 HTML에서 날짜 기반 게시글 URL을 수집합니다. |
| `skplanet` | SK planet Tech Topic | 정적 HTML | 목록 HTML에서 게시글 링크를 수집합니다. |
| `socar` | Socar Tech Blog | 정적 HTML | `/posts/` 목록 HTML에서 게시글 링크를 수집합니다. |
| `ssgtech` | SSG Tech Blog | Browser | Medium publication `/all` 페이지를 Scrapling으로 렌더링/스크롤합니다. |
| `tabling` | Tabling Tech Blog | Browser | Medium 스타일 `/all` 페이지를 Scrapling으로 렌더링/스크롤합니다. |
| `tmapmobility` | TMAP Mobility Tech | API | Brunch article API를 호출합니다. |
| `toss` | Toss Tech | API | Toss 공개 posts API를 호출합니다. |
| `wanted` | Wanted Tech Blog | 정적 HTML | Wanted team community 목록 HTML에서 article 링크를 수집합니다. |
| `watcha` | WATCHA Tech Blog | Browser | Medium publication `/all` 페이지를 Scrapling으로 렌더링/스크롤합니다. |
| `woowabros` | 우아한형제들 기술블로그 | Browser + AJAX | Scrapling으로 WordPress 세션을 연 뒤 `admin-ajax.php` 게시글 API를 호출합니다. |
| `yogiyo` | YOGIYO Tech Blog | Browser | Medium 스타일 `/all` 페이지를 Scrapling으로 렌더링/스크롤합니다. |

## 방식별 기준

| 방식 | 구현 기준 |
| --- | --- |
| API | 공개 JSON API, WordPress REST API, Next.js data route 등 구조화된 응답을 `urllib`로 호출합니다. |
| RSS | RSS XML을 `urllib`로 가져와 `<item>` 단위로 파싱합니다. |
| 정적 HTML | 목록 페이지 HTML을 `urllib`로 가져오고, anchor/link 또는 source별 정규식으로 게시글 URL을 추출합니다. |
| Browser | Scrapling `StealthyFetcher`로 렌더링된 DOM을 가져오고, 필요하면 스크롤 후 링크를 추출합니다. |
| Browser + AJAX | 브라우저 세션 또는 초기 요청이 필요한 사이트에서 렌더링/세션 확보 후 내부 AJAX/API를 호출합니다. |

## Source 추가 규칙

새 source는 `sources/{key}.py`를 만들고 `crawl(request, config) -> dict[str, object]` 함수를 노출하면 됩니다.
`jobs.py`가 `sources/`의 비공개 파일이 아닌 `.py` 파일을 자동 발견하므로 별도 registry 수정은 필요 없습니다.

최소 반환 형태:

```python
def crawl(request, config) -> dict[str, object]:
    return {
        "key": request.key,
        "blog": "Blog display name",
        "baseUrl": "https://example.com",
        "requestedUrl": "https://example.com/posts",
        "crawler": "html.urllib",
        "crawledAt": "...",
        "requestedSize": request.size,
        "postCount": 0,
        "posts": [],
    }
```

검증:

```bash
python3 -m compileall -q crawler.py jobs.py service.py redis_stream.py sources
.venv/bin/moamoa-crawler
```

다른 터미널에서 호출:

```bash
curl -X POST 'http://127.0.0.1:8765/crawl?key={key}&size=5'
```
