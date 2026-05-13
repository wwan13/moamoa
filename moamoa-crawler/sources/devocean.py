from __future__ import annotations

import re

from _common import Post, extract_meta, fetch_text, make_payload, normalize_published_at, strip_html, unique_posts


BASE_URL = "https://devocean.sk.com"
LIST_URL = f"{BASE_URL}/blog/index.do?p=BLOG"


def crawl(request, config) -> dict[str, object]:
    body = fetch_text(LIST_URL)
    posts = []
    for match in re.finditer(
        r"<h3[^>]+onclick=[\"']goDetail\(this,[\"'](\d+)[\"'][^>]*>(.*?)</h3>",
        body,
        flags=re.IGNORECASE | re.DOTALL,
    ):
        post_id = match.group(1)
        title = strip_html(match.group(2))
        if not title:
            continue
        url = f"{BASE_URL}/blog/techBoardDetail.do?ID={post_id}&boardType=techBlog"
        posts.append(Post(post_id, title, "", [], "", "", url, "html"))

    posts = unique_posts(posts, request.size)
    posts = [_enrich_post(post) for post in posts]
    if not posts:
        raise RuntimeError(f"devocean crawl finished but no post links were extracted from {LIST_URL}")

    return make_payload(
        key="devocean",
        blog="DEVOCEAN",
        base_url=BASE_URL,
        requested_url=LIST_URL,
        crawler="html.urllib",
        requested_size=request.size,
        posts=posts,
    )


def _enrich_post(post: Post) -> Post:
    body = fetch_text(post.url)
    description = strip_html(extract_meta(body, "description"))
    if description == "데보션 (DEVOCEAN) 기술 블로그 , 개발자 커뮤니티이자 내/외부 소통과 성장 플랫폼":
        description = ""
    thumbnail = extract_meta(body, "og:image")
    published_at = (
        extract_meta(body, "article:published_time")
        or extract_meta(body, "publishdate")
        or extract_meta(body, "date")
        or _extract_page_date(body)
    )
    return Post(
        key=post.key,
        title=post.title,
        description=description,
        tags=post.tags,
        thumbnail=thumbnail or post.thumbnail,
        publishedAt=normalize_published_at(published_at) or post.publishedAt,
        url=post.url,
        source=post.source,
    )


def _extract_page_date(body: str) -> str:
    for pattern in (
        r"<time[^>]+datetime=[\"']([^\"']+)[\"']",
        r"class=[\"'][^\"']*(?:upload-date|published|post-date|date)[^\"']*[\"'][^>]*>([^<]+)<",
        r"id=[\"'][^\"']*(?:Regdate|regdate|date)[^\"']*[\"'][^>]*>([^<]+)<",
    ):
        match = re.search(pattern, body, flags=re.IGNORECASE | re.DOTALL)
        if match:
            return strip_html(match.group(1))
    return ""
