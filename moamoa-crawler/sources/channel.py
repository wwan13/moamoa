from __future__ import annotations

import json
from datetime import datetime, timezone
from typing import Any
from urllib.parse import urlsplit, urlunsplit

from _common import Post, fetch_json, make_payload


BASE_URL = "https://tech.channel.io"
API_URL = "https://document-api.channel.io/website/v1/ko/spaces/$tech-blog/articles/query"

TOPICS: list[tuple[str, str]] = [
    ("86a214a2", "ai"),
    ("e8ccfdd9", "개발문화"),
    ("62d94185", "백앤드"),
    ("b2d05c50", "프론트앤드"),
    ("207e9832", "모바일"),
    ("d3899113", "데브옵스"),
    ("291580a3", "디자인/PM"),
]


def crawl(request, config) -> dict[str, object]:
    del config

    merged_posts: dict[str, tuple[int, Post]] = {}

    for topic_slug, topic_tag in TOPICS:
        since: str | None = None

        while True:
            payload = {
                "limit": 25,
                "order": "desc",
                "expression": {
                    "and": [
                        {
                            "or": [
                                {
                                    "key": "topic_slug",
                                    "operator": "$eq",
                                    "type": "string",
                                    "values": [topic_slug],
                                }
                            ]
                        }
                    ]
                },
            }
            if since:
                payload["since"] = since

            page = fetch_json(
                API_URL,
                method="POST",
                data=json.dumps(payload).encode("utf-8"),
                headers={"Content-Type": "application/json", "Accept": "application/json"},
            )

            page_posts = _deserialize_posts(page, topic_tag)
            for published_at_ms, post in page_posts:
                existing = merged_posts.get(post.url)
                if existing is None:
                    merged_posts[post.url] = (published_at_ms, post)
                    continue

                merged_posts[post.url] = (
                    max(existing[0], published_at_ms),
                    Post(
                        key=existing[1].key or post.key,
                        title=existing[1].title or post.title,
                        description=existing[1].description or post.description,
                        tags=_merge_tags(existing[1].tags, post.tags),
                        thumbnail=existing[1].thumbnail or post.thumbnail,
                        publishedAt=existing[1].publishedAt or post.publishedAt,
                        url=post.url,
                        source=post.source,
                    ),
                )

            since = page.get("next")
            if not since:
                break

    posts = [post for _, post in sorted(merged_posts.values(), key=lambda item: item[0], reverse=True)]
    if request.size is not None:
        posts = posts[: request.size]

    if not posts:
        raise RuntimeError(f"channel API returned no posts from {API_URL}")

    return make_payload(
        key="channel",
        blog="Channel Tech Blog",
        base_url=BASE_URL,
        requested_url=API_URL,
        crawler="api.urllib",
        requested_size=request.size,
        posts=posts,
    )


def _deserialize_posts(page: dict[str, Any], topic_tag: str) -> list[tuple[int, Post]]:
    result: list[tuple[int, Post]] = []

    for article in page.get("articles") or []:
        url = _normalize_article_url((article.get("website") or {}).get("url"))
        title = str(article.get("title") or "").strip()
        article_id = str(article.get("id") or "").strip()
        if not url or not title or not article_id:
            continue

        published_at_ms = _to_epoch_ms(article.get("publishedAt"))
        published_at = _epoch_ms_to_iso(published_at_ms)

        result.append(
            (
                published_at_ms,
                Post(
                    key=article_id,
                    title=title,
                    description=str(article.get("summary") or "").strip(),
                    tags=[topic_tag],
                    thumbnail=str(article.get("coverImageUrl") or article.get("opengraphMetaImageUrl") or "").strip(),
                    publishedAt=published_at,
                    url=url,
                    source="api",
                ),
            )
        )

    return result


def _normalize_article_url(value: Any) -> str:
    raw = str(value or "").strip()
    if not raw:
        return ""

    parsed = urlsplit(raw)
    path = parsed.path or ""

    if parsed.netloc == "docs.channel.io" and path.startswith("/tech-blog/ko/articles/"):
        path = path.removeprefix("/tech-blog")
        return urlunsplit(("https", "tech.channel.io", path, parsed.query, ""))

    if parsed.netloc == "docs.channel.io":
        return urlunsplit(("https", "tech.channel.io", path, parsed.query, ""))

    return raw


def _merge_tags(left: list[str], right: list[str]) -> list[str]:
    merged: list[str] = []
    for tag in [*left, *right]:
        if tag and tag not in merged:
            merged.append(tag)
    return merged


def _to_epoch_ms(value: Any) -> int:
    try:
        number = int(value)
    except (TypeError, ValueError):
        return 0
    return number if number > 0 else 0


def _epoch_ms_to_iso(value: int) -> str:
    if value <= 0:
        return ""
    return datetime.fromtimestamp(value / 1000, tz=timezone.utc).isoformat()
