from __future__ import annotations

import json
import re
import urllib.error
import urllib.parse
from datetime import datetime, timezone
from typing import Any, Callable

from _common import (
    Post,
    extract_links,
    fetch_json,
    fetch_text,
    html_link_posts,
    key_from_url,
    make_payload,
    normalize_space,
    normalize_tag,
    strip_html,
    unique_posts,
)


def _request_page_size(request: Any, default: int = 100) -> int:
    size = getattr(request, "size", None)
    if size is None or size <= 0:
        return default
    return min(size, default)


def _collect_paginated_posts(
    request: Any,
    *,
    start_page: int,
    build_url: Callable[[int, int], str],
    parse_page: Callable[[Any], list[Post]],
    headers: dict[str, str] | None = None,
    max_page_size: int = 100,
    stop_statuses: tuple[int, ...] = (404,),
) -> tuple[str, list[Post]]:
    page_size = _request_page_size(request, default=max_page_size)
    requested_url = build_url(start_page, page_size)
    posts: list[Post] = []
    page = start_page

    while True:
        url = build_url(page, page_size)
        try:
            data = fetch_json(url, headers=headers)
        except urllib.error.HTTPError as exc:
            if exc.code in stop_statuses:
                break
            raise

        page_posts = parse_page(data)
        if not page_posts:
            break

        posts = unique_posts([*posts, *page_posts], request.size)
        if request.size is not None and len(posts) >= request.size:
            break
        page += 1

    return requested_url, posts


def _epoch_ms(value: Any) -> str:
    try:
        number = int(value)
    except (TypeError, ValueError):
        return ""
    if number <= 0:
        return ""
    return datetime.fromtimestamp(number / 1000, tz=timezone.utc).isoformat()


def crawl_ably(request: Any) -> dict[str, object]:
    base_url = "https://ably.team"
    page_url = f"{base_url}/news?category=community"
    html = fetch_text(page_url)
    match = re.search(r'"buildId"\s*:\s*"([^"]+)"', html)
    if not match:
        raise RuntimeError("ably buildId extraction failed")
    api_url = f"{base_url}/_next/data/{match.group(1)}/news.json?category=community"
    data = fetch_json(api_url)
    articles = data.get("pageProps", {}).get("communityArticles", [])
    posts = []
    for article in articles:
        article_id = str(article.get("id") or "").strip()
        title_nodes = article.get("title") or []
        title = normalize_space((title_nodes[0] or {}).get("text") if title_nodes else "")
        if not article_id or not title:
            continue
        content = article.get("content") or []
        description = ""
        for node in content:
            if node.get("type") == "paragraph":
                description = normalize_space(node.get("text"))
                break
        image = (article.get("representationImage") or {}).get("url") or ""
        posts.append(Post(article_id, title, description, [], image, article.get("createdAt") or "", f"{base_url}/news/{article_id}", "api"))
    posts = unique_posts(posts, request.size)
    if not posts:
        raise RuntimeError("ably API returned no posts")
    return make_payload(key="ably", blog="Ably Tech Blog", base_url=base_url, requested_url=api_url, crawler="api.urllib", requested_size=request.size, posts=posts)


def crawl_gabia(request: Any) -> dict[str, object]:
    base_url = "https://library.gabia.com"
    api_url, posts = _collect_paginated_posts(
        request,
        start_page=1,
        build_url=lambda page, page_size: f"{base_url}/wp-json/wp/v2/posts?page={page}&per_page={page_size}&_embed=1",
        max_page_size=10,
        stop_statuses=(400, 404),
        parse_page=lambda items: [
            Post(
                key=str(item.get("id") or ""),
                title=strip_html((item.get("title") or {}).get("rendered")),
                description=strip_html((item.get("excerpt") or {}).get("rendered")),
                tags=[],
                thumbnail=((item.get("_embedded") or {}).get("wp:featuredmedia") or [{}])[0].get("source_url") or "",
                publishedAt=item.get("date") or "",
                url=item.get("link") or "",
                source="api",
            )
            for item in items
            if str(item.get("id") or "").strip()
            and strip_html((item.get("title") or {}).get("rendered"))
            and (item.get("link") or "")
        ],
    )
    if not posts:
        raise RuntimeError("gabia API returned no posts")
    return make_payload(key="gabia", blog="Gabia Library", base_url=base_url, requested_url=api_url, crawler="api.urllib", requested_size=request.size, posts=posts)


def crawl_naver(request: Any) -> dict[str, object]:
    base_url = "https://d2.naver.com"
    api_url, posts = _collect_paginated_posts(
        request,
        start_page=0,
        build_url=lambda page, page_size: f"{base_url}/api/v1/contents?categoryId=&page={page}&size={page_size}",
        parse_page=lambda data: [
            Post(
                key_from_url(base_url + rel),
                title,
                strip_html(item.get("postHtml")),
                [],
                (base_url + thumbnail) if thumbnail.startswith("/") else thumbnail,
                _epoch_ms(item.get("postPublishedAt")),
                base_url + rel,
                "api",
            )
            for item in data.get("content", [])
            for rel in [(item.get("url") or "").strip()]
            for title in [normalize_space(item.get("postTitle"))]
            for thumbnail in [item.get("postImage") or ""]
            if rel.startswith("/") and title
        ],
    )
    if not posts:
        raise RuntimeError("naver API returned no posts")
    return make_payload(key="naver", blog="NAVER D2", base_url=base_url, requested_url=api_url, crawler="api.urllib", requested_size=request.size, posts=posts)


def crawl_nhncloud(request: Any) -> dict[str, object]:
    base_url = "https://meetup.nhncloud.com"
    api_url, posts = _collect_paginated_posts(
        request,
        start_page=1,
        build_url=lambda page, page_size: f"{base_url}/tcblog/v1.0/posts?pageNo={page}&rowsPerPage={page_size}",
        parse_page=lambda data: _parse_nhncloud_posts(data),
    )
    if not posts:
        raise RuntimeError("nhncloud API returned no posts")
    return make_payload(key="nhncloud", blog="NHN Cloud Meetup", base_url=base_url, requested_url=api_url, crawler="api.urllib", requested_size=request.size, posts=posts)


def crawl_postype(request: Any) -> dict[str, object]:
    base_url = "https://postype.com"
    api_url, posts = _collect_paginated_posts(
        request,
        start_page=0,
        build_url=lambda page, page_size: f"https://api.postype.com/api/v1/channel/516863/activity/posts?page={page}&size={page_size}&sortType=RECENT",
        parse_page=lambda data: [
            Post(
                str(post_id),
                title,
                normalize_space(item.get("summary")) or normalize_space(item.get("subTitle")),
                [normalize_tag(tag) for tag in item.get("tags") or [] if normalize_tag(tag)],
                ((item.get("thumbnails") or [{}])[0].get("imagePath") or "") if item.get("thumbnails") else "",
                datetime.fromtimestamp(published, tz=timezone.utc).isoformat() if published else "",
                url,
                "api",
            )
            for wrapper in data.get("content") or []
            for item in [wrapper.get("feedItem") or {}]
            for url in [normalize_space(item.get("shortUrl"))]
            for title in [normalize_space(item.get("title"))]
            for post_id in [item.get("postId")]
            for published in [item.get("publishedAt")]
            if url and title and post_id is not None
        ],
    )
    if not posts:
        raise RuntimeError("postype API returned no posts")
    return make_payload(key="postype", blog="Postype Team", base_url=base_url, requested_url=api_url, crawler="api.urllib", requested_size=request.size, posts=posts)


def crawl_toss(request: Any) -> dict[str, object]:
    base_url = "https://toss.tech"
    api_url, posts = _collect_paginated_posts(
        request,
        start_page=1,
        build_url=lambda page, page_size: f"https://api-public.toss.im/api-public/v3/ipd-thor/api/v1/workspaces/15/posts?size={page_size}&page={page}",
        parse_page=lambda data: _parse_toss_posts(data, base_url),
    )
    if not posts:
        raise RuntimeError("toss API returned no posts")
    return make_payload(key="toss", blog="Toss Tech", base_url=base_url, requested_url=api_url, crawler="api.urllib", requested_size=request.size, posts=posts)


def crawl_kakao(request: Any) -> dict[str, object]:
    base_url = "https://tech.kakao.com"
    api_url, posts = _collect_paginated_posts(
        request,
        start_page=1,
        build_url=lambda page, _page_size: f"{base_url}/api/v2/posts?page={page}&code=blog",
        parse_page=lambda data: [
            Post(
                key_from_url(f"{base_url}/posts/{post_id}"),
                title,
                strip_html(item.get("description")),
                [normalize_space(category.get("name")) for category in item.get("categories") or [] if normalize_space(category.get("name"))],
                normalize_space(item.get("thumbnailUri") or item.get("thumb")),
                normalize_space(item.get("releaseDateTime") or item.get("releaseDate")),
                f"{base_url}/posts/{post_id}",
                "api",
            )
            for item in data.get("postList") or []
            for post_id in [item.get("id")]
            for title in [normalize_space(item.get("title"))]
            if post_id is not None and title
        ],
        headers={"Accept": "application/json", "Referer": f"{base_url}/blog"},
    )
    if not posts:
        raise RuntimeError("kakao API returned no posts")
    return make_payload(key="kakao", blog="Kakao Tech", base_url=base_url, requested_url=api_url, crawler="api.urllib", requested_size=request.size, posts=posts)


def crawl_tmapmobility(request: Any) -> dict[str, object]:
    posts = []
    api_url = "https://api.brunch.co.kr/v2/article/@tmapmobility?lastTime=0&thumbnail=Y&membershipContent=false"
    next_url: str | None = api_url

    while next_url:
        data = fetch_json(next_url)
        items = (data.get("data") or {}).get("list") or []
        if not items:
            break

        page_posts = []
        for item in items:
            profile_id = normalize_space(item.get("profileId"))
            article_no = item.get("no")
            title = normalize_space(item.get("title"))
            if not profile_id or article_no is None or not title:
                continue
            thumbnail = item.get("articleImageForHomeOrDefault") or item.get("articleImageForHome") or ""
            for image in item.get("articleImageList") or []:
                if not thumbnail and image.get("type") == "cover":
                    thumbnail = image.get("url") or ""
            tags = [normalize_tag(tag) for tag in (item.get("articleKeywordNameAsCsv") or "").split(",") if normalize_tag(tag)]
            page_posts.append(
                Post(
                    str(item.get("contentId") or article_no),
                    title,
                    normalize_space(item.get("contentSummary")),
                    tags,
                    thumbnail,
                    _epoch_ms(item.get("publishTimestamp") or item.get("publishTime")),
                    f"https://brunch.co.kr/@{profile_id}/{article_no}",
                    "api",
                )
            )

        posts = unique_posts([*posts, *page_posts], request.size)
        if request.size is not None and len(posts) >= request.size:
            break

        next_url = (data.get("data") or {}).get("nextUrl")

    if not posts:
        raise RuntimeError("tmapmobility API returned no posts")
    return make_payload(key="tmapmobility", blog="TMAP Mobility Tech", base_url="https://brunch.co.kr/@tmapmobility", requested_url=api_url, crawler="api.urllib", requested_size=request.size, posts=posts)


def _parse_nhncloud_posts(data: dict[str, Any]) -> list[Post]:
    if not (data.get("header") or {}).get("isSuccessful"):
        raise RuntimeError("nhncloud meetup API was not successful")

    posts = []
    for item in data.get("posts") or []:
        lang = item.get("postPerLang") or {}
        title = normalize_space(lang.get("title"))
        post_id = item.get("postId")
        if not title or post_id is None:
            continue
        posts.append(
            Post(
                str(post_id),
                title,
                normalize_space(item.get("contentPreview") or lang.get("description")),
                [],
                normalize_space(lang.get("repImageUrl")),
                item.get("regDt") or item.get("publishTime") or item.get("regTime") or "",
                f"https://meetup.nhncloud.com/posts/{post_id}",
                "api",
            )
        )
    return posts


def _parse_toss_posts(data: dict[str, Any], base_url: str) -> list[Post]:
    if data.get("resultType") != "SUCCESS":
        raise RuntimeError("toss API was not successful")

    posts = []
    for item in (data.get("success") or {}).get("results") or []:
        post_id = item.get("id")
        title = normalize_space(item.get("title"))
        if post_id is None or not title:
            continue
        posts.append(Post(str(post_id), title, normalize_space(item.get("subtitle")), [normalize_tag(item.get("category"))] if item.get("category") else [], (item.get("thumbnailConfig") or {}).get("imageUrl") or "", item.get("publishedTime") or "", f"{base_url}/article/{post_id}", "api"))
    return posts


def crawl_woowabros(request: Any, config: Any) -> dict[str, object]:
    from scrapling.fetchers import StealthyFetcher

    base_url = "https://techblog.woowahan.com/"

    def fetch_page(page_no: int) -> list[Post]:
        StealthyFetcher.fetch(base_url, headless=config.headless, network_idle=True, wait=config.wait)
        body = urllib.parse.urlencode(
            {
                "action": "get_posts_data",
                "data[post][post_status]": "publish",
                "data[post][paged]": str(page_no),
                "data[meta]": "main",
            }
        ).encode("utf-8")
        text = fetch_text(
            f"{base_url}wp-admin/admin-ajax.php",
            method="POST",
            data=body,
            headers={
                "X-Requested-With": "XMLHttpRequest",
                "Content-Type": "application/x-www-form-urlencoded; charset=UTF-8",
                "Accept": "application/json, text/javascript, */*; q=0.01",
                "Referer": base_url,
            },
        )
        data = json.loads(text)
        if not data.get("success"):
            raise RuntimeError("woowabros AJAX response was not successful")
        posts = []
        for item in (data.get("data") or {}).get("posts") or []:
            url = normalize_space(item.get("permalink"))
            title = normalize_space(item.get("post_title"))
            if not url or not title:
                continue
            posts.append(Post(key_from_url(url), title, normalize_space(item.get("excerpt")), [], "", normalize_space(item.get("date")), url, "browser"))
        return posts

    posts = unique_posts(fetch_page(1), request.size)
    if not posts:
        raise RuntimeError("woowabros browser crawl returned no posts")
    return make_payload(key="woowabros", blog="우아한형제들 기술블로그", base_url=base_url, requested_url=base_url, crawler="scrapling.StealthyFetcher", requested_size=request.size, posts=posts)
