from __future__ import annotations

import json
import re
from datetime import datetime

from _common import Post, fetch_text, make_payload_raw as make_payload, normalize_url


KEY = "kakaoMobility"
BLOG = "Kakao Mobility Developers"
BASE_URL = "https://developers.kakaomobility.com"
LIST_URL = f"{BASE_URL}/techblogs"


def crawl(request, config) -> dict[str, object]:
    del config
    list_page = fetch_text(LIST_URL)
    data_url = _extract_data_url(list_page)
    raw_items = _extract_items(fetch_text(data_url))

    posts: list[Post] = []
    for item in raw_items:
        posts.append(_to_post(item))
        if request.size and len(posts) >= request.size:
            break

    if not posts:
        raise RuntimeError(f"{KEY} crawl finished but no posts were extracted from {data_url}")

    return make_payload(
        key=KEY,
        blog=BLOG,
        base_url=BASE_URL,
        requested_url=data_url,
        crawler="vitepress-data.urllib",
        requested_size=request.size,
        posts=posts,
    )


def _extract_data_url(list_page: str) -> str:
    match = re.search(r'(/assets/chunks/techblogs\.data\.[^"\s]+\.js)', list_page)
    if match is None:
        raise RuntimeError(f"{KEY} list page data chunk was not found: {LIST_URL}")
    return normalize_url(BASE_URL, match.group(1))


def _extract_items(raw_data: str) -> list[dict[str, object]]:
    match = re.search(r"JSON\.parse\(`(.*)`\);export\{", raw_data, re.S)
    if match is None:
        raise RuntimeError(f"{KEY} data chunk payload was not found")

    return json.loads(match.group(1))


def _to_post(item: dict[str, object]) -> Post:
    url = _require_field(str(item.get("link") or ""), "url")
    resolved_url = normalize_url(BASE_URL, url)
    title = _require_field(str(item.get("title") or ""), "title", resolved_url)
    key = _require_field(_normalize_key(resolved_url), "key", resolved_url)
    description = str(item.get("description") or "").strip()
    thumbnail = normalize_url(BASE_URL, str(item.get("image") or "")) if item.get("image") else ""

    return Post(
        key=key,
        title=title,
        description=description,
        tags=[],
        thumbnail=thumbnail,
        publishedAt=_parse_published_at(str(item.get("date") or "")),
        url=resolved_url,
        source="vitepress-data",
    )


def _parse_published_at(raw: str) -> str:
    text = raw.strip()
    if not text:
        return ""
    try:
        return datetime.strptime(text, "%Y.%m.%d").strftime("%Y-%m-%dT00:00:00")
    except ValueError:
        return ""


def _normalize_key(raw: str) -> str:
    cleaned = raw.strip().rstrip("/").removesuffix(".html")
    if not cleaned:
        return ""
    return cleaned.split("/")[-1]


def _require_field(value: str, field: str, url: str | None = None) -> str:
    trimmed = value.strip()
    if trimmed:
        return trimmed

    url_value = url if url else "unknown"
    raise RuntimeError(f"blogKey={KEY}, url={url_value}, field={field}")
