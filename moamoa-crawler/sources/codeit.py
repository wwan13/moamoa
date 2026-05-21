from __future__ import annotations

from urllib.error import HTTPError
from urllib.parse import urlsplit

from _common import Post, enrich_post_details, fetch_html, make_payload_raw as make_payload


KEY = "codeit"
BLOG = "코드잇 기술 블로그"
BASE_URL = "https://tech.codeit.kr"


def crawl(request, config) -> dict[str, object]:
    del config

    posts: list[Post] = []
    seen_keys: set[str] = set()
    page = 1

    while request.size is None or len(posts) < request.size:
        list_url = _build_list_url(page)
        try:
            doc = fetch_html(list_url)
        except HTTPError as error:
            if error.code == 404 and page > 1:
                break
            raise

        cards = doc.select("article.feed.public.post")
        if not cards:
            break

        new_posts: list[Post] = []
        for card in cards:
            post = _parse_post(card)
            if post.key in seen_keys:
                continue
            new_posts.append(post)
            seen_keys.add(post.key)

        if not new_posts:
            break

        posts.extend(new_posts)
        page += 1

    if request.size is not None:
        posts = posts[: request.size]
    if not posts:
        raise RuntimeError(f"{KEY} crawl finished but no post cards were extracted from {_build_list_url(1)}")

    posts = enrich_post_details(posts, limit=request.size)

    return make_payload(
        key=KEY,
        blog=BLOG,
        base_url=BASE_URL,
        requested_url=_build_list_url(1),
        crawler="html.urllib",
        requested_size=request.size,
        posts=posts,
    )


def _build_list_url(page: int) -> str:
    return f"{BASE_URL}/" if page == 1 else f"{BASE_URL}/page/{page}/"


def _parse_post(card) -> Post:
    link_el = card.select_first("a.u-permalink[href]")
    title_el = card.select_first("h2.feed-title")
    if link_el is None or title_el is None:
        raise RuntimeError(f"{KEY} crawl finished but missing post card fields")

    url = link_el.abs_url("href").strip()
    title = title_el.text().strip()
    key = _extract_key(url)
    if not url or not title or not key:
        raise RuntimeError(f"{KEY} crawl finished but missing required post values for {url or 'unknown'}")

    return Post(
        key=key,
        title=title,
        description="",
        tags=[],
        thumbnail="",
        publishedAt="",
        url=url,
        source="html",
    )


def _extract_key(url: str) -> str:
    return urlsplit(url).path.rstrip("/").split("/")[-1].strip()
