from __future__ import annotations

import json
import re
import urllib.parse
from datetime import datetime, timezone
from typing import Any

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
    if size is None:
        return default
    return size


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
    api_url = f"{base_url}/wp-json/wp/v2/posts?page=1&per_page={_request_page_size(request)}&_embed=1"
    items = fetch_json(api_url)
    posts = []
    for item in items:
        embedded = item.get("_embedded") or {}
        media = embedded.get("wp:featuredmedia") or []
        thumbnail = (media[0] or {}).get("source_url") if media else ""
        posts.append(
            Post(
                key=str(item.get("id") or ""),
                title=strip_html((item.get("title") or {}).get("rendered")),
                description=strip_html((item.get("excerpt") or {}).get("rendered")),
                tags=[],
                thumbnail=thumbnail or "",
                publishedAt=item.get("date") or "",
                url=item.get("link") or "",
                source="api",
            )
        )
    posts = unique_posts([p for p in posts if p.key and p.title and p.url], request.size)
    if not posts:
        raise RuntimeError("gabia API returned no posts")
    return make_payload(key="gabia", blog="Gabia Library", base_url=base_url, requested_url=api_url, crawler="api.urllib", requested_size=request.size, posts=posts)


def crawl_naver(request: Any) -> dict[str, object]:
    base_url = "https://d2.naver.com"
    api_url = f"{base_url}/api/v1/contents?categoryId=&page=0&size={_request_page_size(request)}"
    data = fetch_json(api_url)
    posts = []
    for item in data.get("content", []):
        rel = (item.get("url") or "").strip()
        title = normalize_space(item.get("postTitle"))
        if not rel.startswith("/") or not title:
            continue
        url = base_url + rel
        thumbnail = item.get("postImage") or ""
        if thumbnail and thumbnail.startswith("/"):
            thumbnail = base_url + thumbnail
        posts.append(Post(key_from_url(url), title, strip_html(item.get("postHtml")), [], thumbnail, _epoch_ms(item.get("postPublishedAt")), url, "api"))
    posts = unique_posts(posts, request.size)
    if not posts:
        raise RuntimeError("naver API returned no posts")
    return make_payload(key="naver", blog="NAVER D2", base_url=base_url, requested_url=api_url, crawler="api.urllib", requested_size=request.size, posts=posts)


def crawl_nhncloud(request: Any) -> dict[str, object]:
    base_url = "https://meetup.nhncloud.com"
    api_url = f"{base_url}/tcblog/v1.0/posts?pageNo=1&rowsPerPage={_request_page_size(request)}"
    data = fetch_json(api_url)
    if not (data.get("header") or {}).get("isSuccessful"):
        raise RuntimeError("nhncloud meetup API was not successful")
    posts = []
    for item in data.get("posts") or []:
        lang = item.get("postPerLang") or {}
        title = normalize_space(lang.get("title"))
        post_id = item.get("postId")
        if not title or post_id is None:
            continue
        posts.append(Post(str(post_id), title, normalize_space(lang.get("description")), [], normalize_space(lang.get("repImageUrl")), item.get("publishTime") or item.get("regTime") or "", f"{base_url}/posts/{post_id}", "api"))
    posts = unique_posts(posts, request.size)
    if not posts:
        raise RuntimeError("nhncloud API returned no posts")
    return make_payload(key="nhncloud", blog="NHN Cloud Meetup", base_url=base_url, requested_url=api_url, crawler="api.urllib", requested_size=request.size, posts=posts)


def crawl_postype(request: Any) -> dict[str, object]:
    base_url = "https://postype.com"
    api_url = f"https://api.postype.com/api/v1/channel/516863/activity/posts?page=0&size={_request_page_size(request)}&sortType=RECENT"
    data = fetch_json(api_url)
    posts = []
    for wrapper in data.get("content") or []:
        item = wrapper.get("feedItem") or {}
        url = normalize_space(item.get("shortUrl"))
        title = normalize_space(item.get("title"))
        post_id = item.get("postId")
        if not url or not title or post_id is None:
            continue
        thumbnails = item.get("thumbnails") or []
        tags = [normalize_tag(tag) for tag in item.get("tags") or [] if normalize_tag(tag)]
        published = item.get("publishedAt")
        published_at = datetime.fromtimestamp(published, tz=timezone.utc).isoformat() if published else ""
        description = normalize_space(item.get("summary")) or normalize_space(item.get("subTitle"))
        posts.append(Post(str(post_id), title, description, tags, (thumbnails[0] or {}).get("imagePath") if thumbnails else "", published_at, url, "api"))
    posts = unique_posts(posts, request.size)
    if not posts:
        raise RuntimeError("postype API returned no posts")
    return make_payload(key="postype", blog="Postype Team", base_url=base_url, requested_url=api_url, crawler="api.urllib", requested_size=request.size, posts=posts)


def crawl_toss(request: Any) -> dict[str, object]:
    base_url = "https://toss.tech"
    api_url = f"https://api-public.toss.im/api-public/v3/ipd-thor/api/v1/workspaces/15/posts?size={_request_page_size(request)}&page=1"
    data = fetch_json(api_url)
    if data.get("resultType") != "SUCCESS":
        raise RuntimeError("toss API was not successful")
    posts = []
    for item in (data.get("success") or {}).get("results") or []:
        post_id = item.get("id")
        if post_id is None:
            continue
        posts.append(Post(str(post_id), normalize_space(item.get("title")), normalize_space(item.get("subtitle")), [normalize_tag(item.get("category"))] if item.get("category") else [], (item.get("thumbnailConfig") or {}).get("imageUrl") or "", item.get("publishedTime") or "", f"{base_url}/article/{post_id}", "api"))
    posts = unique_posts([p for p in posts if p.title], request.size)
    if not posts:
        raise RuntimeError("toss API returned no posts")
    return make_payload(key="toss", blog="Toss Tech", base_url=base_url, requested_url=api_url, crawler="api.urllib", requested_size=request.size, posts=posts)


def crawl_tmapmobility(request: Any) -> dict[str, object]:
    api_url = "https://api.brunch.co.kr/v2/article/@tmapmobility?lastTime=0&thumbnail=Y&membershipContent=false"
    data = fetch_json(api_url)
    posts = []
    for item in (data.get("data") or {}).get("list") or []:
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
        posts.append(Post(str(item.get("contentId") or article_no), title, normalize_space(item.get("contentSummary")), tags, thumbnail, _epoch_ms(item.get("publishTimestamp") or item.get("publishTime")), f"https://brunch.co.kr/@{profile_id}/{article_no}", "api"))
    posts = unique_posts(posts, request.size)
    if not posts:
        raise RuntimeError("tmapmobility API returned no posts")
    return make_payload(key="tmapmobility", blog="TMAP Mobility Tech", base_url="https://brunch.co.kr/@tmapmobility", requested_url=api_url, crawler="api.urllib", requested_size=request.size, posts=posts)


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
