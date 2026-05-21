from __future__ import annotations

from datetime import datetime
from urllib.parse import urlsplit

from _common import Post, fetch_html, make_payload_raw as make_payload, normalize_tag


KEY = "theswing"
BLOG = "더스윙 기술 블로그"
BASE_URL = "https://www.theswing.tech"
LIST_URL = f"{BASE_URL}/"


def crawl(request, config) -> dict[str, object]:
    del config
    doc = fetch_html(LIST_URL)
    cards = doc.select("article.post-card")
    if not cards:
        raise RuntimeError(f"{KEY} crawl finished but no post cards were extracted from {LIST_URL}")

    posts: list[Post] = []
    seen_keys: set[str] = set()

    for card in cards:
        post = _parse_post(card)
        if post.key in seen_keys:
            continue
        posts.append(post)
        seen_keys.add(post.key)
        if request.size is not None and len(posts) >= request.size:
            break

    if not posts:
        raise RuntimeError(f"{KEY} crawl finished but no post links were extracted from {LIST_URL}")

    return make_payload(
        key=KEY,
        blog=BLOG,
        base_url=BASE_URL,
        requested_url=LIST_URL,
        crawler="html.urllib",
        requested_size=request.size,
        posts=posts,
    )


def _parse_post(card) -> Post:
    link_el = card.select_first("a.post-card-content-link[href]") or card.select_first("a.post-card-image-link[href]")
    if link_el is None:
        raise RuntimeError(f"{KEY} crawl finished but missing url for unknown")

    url = _require(link_el.abs_url("href"), "url", "")
    key = _extract_key(url)

    title_el = card.select_first(".post-card-title")
    title = _require(title_el.text() if title_el else "", "title", url)

    description_el = card.select_first(".post-card-excerpt")
    tag_el = card.select_first(".post-card-primary-tag")
    time_el = card.select_first("time[datetime]")
    image_el = card.select_first("img.post-card-image")

    tags: list[str] = []
    primary_tag = normalize_tag(tag_el.text()) if tag_el else ""
    if primary_tag:
        tags.append(primary_tag)

    return Post(
        key=key,
        title=title,
        description=description_el.text() if description_el else "",
        tags=tags,
        thumbnail=image_el.abs_url("src") if image_el else "",
        publishedAt=_parse_published_at(time_el.attr("datetime") if time_el else "", url),
        url=url,
        source="html",
    )


def _extract_key(url: str) -> str:
    path = urlsplit(url).path.rstrip("/")
    key = path.split("/")[-1]
    return _require(key, "key", url)


def _parse_published_at(raw: str, url: str) -> str:
    text = raw.strip()
    if not text:
        raise RuntimeError(f"{KEY} crawl finished but missing publishedAt for {url}")
    try:
        return datetime.strptime(text, "%Y-%m-%d").strftime("%Y-%m-%dT00:00:00")
    except ValueError as error:
        raise RuntimeError(f"{KEY} crawl finished but invalid publishedAt for {url}: {text}") from error


def _require(value: str, field: str, url: str) -> str:
    trimmed = value.strip()
    if not trimmed:
        raise RuntimeError(f"{KEY} crawl finished but missing {field} for {url or 'unknown'}")
    return trimmed
