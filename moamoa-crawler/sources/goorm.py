from __future__ import annotations

import re
import urllib.parse
from datetime import datetime, timedelta, timezone

from _common import (
    Post,
    fetch_html,
    fetch_text,
    key_from_url,
    make_payload,
    normalize_published_at,
    normalize_space,
    normalize_url,
    parse_html,
    unique_posts,
)


KEY = "goorm"
BLOG = "goorm TechBlog"
BASE_URL = "https://tech.goorm.io"
LIST_URLS = ["https://tech.goorm.io", "https://tech.goorm.io/page/2/"]
GENERIC_DESCRIPTIONS = {
    "We are creating an ecosystem centered on developer growth",
    "구름은 ‘모두가 개발자가 된다’는 비전으로 ‘개발자 성장 중심’ 생태계를 만들어 나가고 있습니다. 아이디어를 즉시 코드로 바꾸는 AI 클라우드 개발 환경 Arkain, AI·SW 교육 플랫폼 구름EDU, 알고리즘 문제 풀이 서비스 구름LEVEL, 코딩 테스트 플랫폼 Devth,",
}


def crawl(request, config) -> dict[str, object]:
    posts: list[Post] = []

    for list_url in LIST_URLS:
        doc = fetch_html(list_url)
        posts.extend(_extract_posts_from_list(doc))
        posts = unique_posts(posts, request.size)
        if request.size is not None and len(posts) >= request.size:
            break

    posts = unique_posts(posts, request.size)
    if not posts:
        raise RuntimeError(f"{KEY} crawl finished but no post links were extracted from {LIST_URLS[0]}")

    posts = [_enrich_post(post) for post in posts]

    return make_payload(
        key=KEY,
        blog=BLOG,
        base_url=BASE_URL,
        requested_url=LIST_URLS[0],
        crawler="html.urllib",
        requested_size=request.size,
        posts=posts,
    )


def _extract_posts_from_list(doc) -> list[Post]:
    posts: list[Post] = []
    for article in doc.select("article.post"):
        post = _extract_post(article)
        if post is not None:
            posts.append(post)
    return unique_posts(posts, 10_000)


def _extract_post(article) -> Post | None:
    url = _extract_post_url(article)
    if not url or not _is_post_url(url):
        return None

    title = normalize_space(_text(article.select_first("h2.post-title, h2.entry-title")))
    if not title:
        return None

    return Post(
        key=key_from_url(url),
        title=title,
        description="",
        tags=[],
        thumbnail=_extract_thumbnail(article),
        publishedAt=_extract_published_at(article),
        url=url,
        source="html",
    )


def _enrich_post(post: Post) -> Post:
    try:
        body = fetch_text(post.url)
    except Exception:
        return post

    doc = parse_html(body, post.url)
    description = _extract_description(doc)
    thumbnail = post.thumbnail or _extract_detail_thumbnail(body, post.url)
    published_at = post.publishedAt or _extract_detail_published_at(doc)

    return Post(
        key=post.key,
        title=post.title,
        description=description,
        tags=post.tags,
        thumbnail=thumbnail,
        publishedAt=published_at,
        url=post.url,
        source=post.source,
    )


def _extract_post_url(article) -> str:
    for selector in ("a.post-link[href]", ".post-media a[href]", "a[href]"):
        link = article.select_first(selector)
        if link is None:
            continue
        href = link.attr("href")
        if href:
            return normalize_url(BASE_URL, href)
    return ""


def _extract_thumbnail(article) -> str:
    image = article.select_first(".post-media img[src], .post-media img[data-lazy-src], img[src]")
    if image is None:
        return ""
    return (
        image.abs_url("src")
        or image.abs_url("data-lazy-src")
        or normalize_url(BASE_URL, image.attr("src") or image.attr("data-lazy-src"))
    )


def _extract_published_at(article) -> str:
    time_node = article.select_first("time.entry-date, time[datetime], .posted-on time")
    if time_node is not None:
        published_at = normalize_published_at(time_node.attr("datetime"))
        if published_at:
            return published_at

    link = article.select_first(".posted-on a[title], .posted-on a[href]")
    if link is not None:
        published_at = normalize_published_at(link.attr("title"))
        if published_at:
            return published_at

    if time_node is not None:
        published_at = _parse_visible_published_at(time_node.text())
        if published_at:
            return published_at

    return ""


def _extract_description(doc) -> str:
    for selector in (
        'meta[name="description"]',
        'meta[property="og:description"]',
        'meta[name="twitter:description"]',
    ):
        node = doc.select_first(selector)
        if node is None:
            continue
        description = normalize_space(node.attr("content"))
        if description and description not in GENERIC_DESCRIPTIONS:
            return description

    for node in doc.select("div.entry-content h2 ~ p, article .entry-content p"):
        description = normalize_space(node.text())
        if _is_valid_description(description):
            return description

    return ""


def _extract_detail_thumbnail(body: str, base_url: str) -> str:
    match = re.search(
        r'<meta[^>]+property=["\']og:image["\'][^>]+content=["\']([^"\']+)["\']',
        body,
        flags=re.IGNORECASE,
    )
    if match:
        return normalize_url(base_url, match.group(1))
    return ""


def _extract_detail_published_at(doc) -> str:
    time_node = doc.select_first("time.entry-date, time[datetime], article time[datetime]")
    if time_node is None:
        return ""
    return normalize_published_at(time_node.attr("datetime")) or _parse_visible_published_at(time_node.text())


def _parse_visible_published_at(value: str) -> str:
    text = normalize_space(value)
    if not text:
        return ""

    normalized = normalize_published_at(text)
    if normalized:
        return normalized

    relative = re.fullmatch(r"(\d+)\s+days?\s+ago", text, flags=re.IGNORECASE)
    if relative:
        days = int(relative.group(1))
        return (datetime.now(timezone.utc) - timedelta(days=days)).replace(microsecond=0).isoformat(timespec="seconds")

    return ""


def _is_valid_description(value: str) -> bool:
    if not value or len(value) < 20:
        return False
    if value in GENERIC_DESCRIPTIONS:
        return False
    if value.startswith("※"):
        return False
    if value.startswith("구름은 ") and "모두가 개발자가 된다" in value:
        return False
    if value.startswith("COMMIT 신청하기"):
        return False
    return True


def _is_post_url(url: str) -> bool:
    parsed = urllib.parse.urlsplit(url)
    if parsed.netloc != urllib.parse.urlsplit(BASE_URL).netloc:
        return False
    path = urllib.parse.unquote(parsed.path)
    if path in ("", "/") or path.startswith("/page/") or path.startswith("/category/"):
        return False
    return not any(part in path for part in ("/comments/", "/feed/", "/wp-admin/"))


def _text(node) -> str:
    if node is None:
        return ""
    value = getattr(node, "text", None)
    if callable(value):
        value = value()
    return normalize_space(str(value)) if value else ""
