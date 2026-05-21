from __future__ import annotations

from datetime import datetime
from urllib.error import HTTPError
from urllib.parse import urlsplit

from _common import Post, fetch_html, make_payload_raw as make_payload


KEY = "spoqa"
BLOG = "Spoqa 기술 블로그"
BASE_URL = "https://spoqa.github.io"


def crawl(request, config) -> dict[str, object]:
    del config
    page = 1
    last_signature: str | None = None
    seen_keys: set[str] = set()
    posts: list[Post] = []

    while request.size is None or len(posts) < request.size:
        list_url = _build_list_url(page)
        try:
            doc = fetch_html(list_url)
        except HTTPError as error:
            if error.code == 404 and page > 1:
                break
            raise

        cards = doc.select(".posts .post-item")
        if not cards:
            break

        signature = "|".join(
            card.select_first(".post-title a[href]").abs_url("href")
            for card in cards
            if card.select_first(".post-title a[href]") is not None
        )
        if not signature or signature == last_signature:
            break
        last_signature = signature

        added_on_page = 0
        for card in cards:
            post = _parse_post(card)
            if post.key in seen_keys:
                continue
            posts.append(post)
            seen_keys.add(post.key)
            added_on_page += 1
            if request.size is not None and len(posts) >= request.size:
                break

        if added_on_page == 0:
            break
        page += 1

    if not posts:
        raise RuntimeError(f"{KEY} crawl finished but no post links were extracted from {_build_list_url(1)}")

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
    return f"{BASE_URL}/" if page == 1 else f"{BASE_URL}/page{page}"


def _parse_post(card) -> Post:
    title_link = card.select_first(".post-title a[href]")
    if title_link is None:
        raise RuntimeError(f"{KEY} crawl finished but missing url for unknown")

    url = _require(title_link.abs_url("href"), "url", "")
    key = _extract_key(url)
    title = _require(title_link.text(), "title", url)
    description = card.select_first(".post-description").text() if card.select_first(".post-description") else ""
    published_at_text = card.select_first(".post-date").text() if card.select_first(".post-date") else ""

    return Post(
        key=key,
        title=title,
        description=description,
        tags=[],
        thumbnail="",
        publishedAt=_parse_published_at(published_at_text, url),
        url=url,
        source="html",
    )


def _extract_key(url: str) -> str:
    path = urlsplit(url).path.rstrip("/")
    key = path.split("/")[-1].removesuffix(".html")
    return _require(key, "key", url)


def _parse_published_at(raw: str, url: str) -> str:
    text = raw.strip()
    if not text:
        raise RuntimeError(f"{KEY} crawl finished but missing publishedAt for {url}")
    try:
        return datetime.strptime(text, "%Y년 %m월 %d일").strftime("%Y-%m-%dT00:00:00")
    except ValueError as error:
        raise RuntimeError(f"{KEY} crawl finished but invalid publishedAt for {url}: {text}") from error


def _require(value: str, field: str, url: str) -> str:
    trimmed = value.strip()
    if not trimmed:
        raise RuntimeError(f"{KEY} crawl finished but missing {field} for {url or 'unknown'}")
    return trimmed
