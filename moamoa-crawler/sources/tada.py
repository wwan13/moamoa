from __future__ import annotations

import json
import re

from _common import Post, fetch_text, make_payload, normalize_published_at, normalize_url, unique_posts


KEY = "tada"
BLOG = "타다 Tech"
BASE_URL = "https://blog-tech.tadatada.com"
REQUESTED_URL = f"{BASE_URL}/?count=1000"
NEXT_FLIGHT_RE = re.compile(r'self\.__next_f\.push\(\[1,"(.*?)"\]\)</script>', flags=re.DOTALL)


def crawl(request, config) -> dict[str, object]:
    del config

    body = fetch_text(REQUESTED_URL)
    posts = unique_posts(_extract_posts(body), request.size)
    if not posts:
        raise RuntimeError(f"{KEY} crawl finished but no post links were extracted from {REQUESTED_URL}")

    return make_payload(
        key=KEY,
        blog=BLOG,
        base_url=BASE_URL,
        requested_url=REQUESTED_URL,
        crawler="next-flight.urllib",
        requested_size=request.size,
        posts=posts,
    )


def _extract_posts(body: str) -> list[Post]:
    posts: list[Post] = []
    for raw_chunk in NEXT_FLIGHT_RE.findall(body):
        if "articles" not in raw_chunk:
            continue

        chunk = json.loads(f'"{raw_chunk}"')
        for article in _extract_articles(chunk):
            post = _to_post(article)
            if post is not None:
                posts.append(post)

    return posts


def _extract_articles(chunk: str) -> list[dict[str, object]]:
    marker = '"articles":'
    start = chunk.find(marker)
    if start < 0:
        return []

    payload = chunk[start + len(marker) :]
    depth = 0
    end = -1
    for index, char in enumerate(payload):
        if char == "[":
            depth += 1
        elif char == "]":
            depth -= 1
            if depth == 0:
                end = index + 1
                break

    if end < 0:
        return []

    try:
        articles = json.loads(payload[:end])
    except json.JSONDecodeError:
        return []

    return [article for article in articles if isinstance(article, dict)]


def _to_post(article: dict[str, object]) -> Post | None:
    slug = _string(article.get("slug"))
    title = _string(article.get("title"))
    if not slug or not title:
        return None

    url = normalize_url(BASE_URL, f"/articles/{slug}")
    image_url = _string(article.get("imageUrl"))
    tags = [tag for tag in (_string(value) for value in article.get("tags") or []) if tag]

    return Post(
        key=slug,
        title=title,
        description=_string(article.get("excerpt")),
        tags=tags,
        thumbnail=normalize_url(BASE_URL, image_url) if image_url else "",
        publishedAt=normalize_published_at(article.get("date")),
        url=url,
        source="next-flight",
    )


def _string(value: object) -> str:
    return value.strip() if isinstance(value, str) else ""
