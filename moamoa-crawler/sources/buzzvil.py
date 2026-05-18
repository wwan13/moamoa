from __future__ import annotations

import json
import re

from concurrent.futures import ThreadPoolExecutor
from datetime import datetime, timezone
from email.utils import parsedate_to_datetime
from urllib.error import HTTPError
from urllib.parse import quote, urlsplit, urlunsplit

from _common import Post, fetch_html, make_payload_raw as make_payload, normalize_tag, normalize_url


KEY = "buzzvil"
BLOG = "Buzzvil Tech Blog"
BASE_URL = "https://tech.buzzvil.com"
LIST_URL = f"{BASE_URL}/blog"
BLOG_PATH_MARKER = "/blog/"


def crawl(request, config) -> dict[str, object]:
    del config
    items = _fetch_list_items()
    if request.size is not None:
        items = items[: request.size]

    posts: list[Post] = []
    with ThreadPoolExecutor(max_workers=10) as executor:
        details = executor.map(lambda item: _fetch_detail(item["url"]), items)
        for item, detail in zip(items, details):
            published_at = detail["publishedAt"] or item["publishedAt"]
            if not published_at:
                continue
            posts.append(
                Post(
                    key=item["key"],
                    title=item["title"],
                    description=item["description"],
                    tags=item["tags"],
                    thumbnail=detail["thumbnail"] or item["thumbnail"],
                    publishedAt=published_at,
                    url=item["url"],
                    source="html",
                )
            )

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


def _fetch_list_items() -> list[dict[str, object]]:
    doc = fetch_html(LIST_URL)
    for script in doc.select("script"):
        text = script.text()
        if "__next_f.push" not in text or '\\"items\\":[' not in text:
            continue

        decoded = _decode_next_payload(text)
        if not decoded:
            continue

        items = _extract_items(decoded)
        if items:
            return items

    raise RuntimeError(f"{KEY} crawl finished but no post links were extracted from {LIST_URL}")


def _decode_next_payload(script_text: str) -> str:
    matched = re.match(r"self\.__next_f\.push\((.*)\)$", script_text, flags=re.S)
    if not matched:
        return ""
    try:
        payload = json.loads(matched.group(1))
    except json.JSONDecodeError:
        return ""
    if not isinstance(payload, list) or len(payload) < 2 or not isinstance(payload[1], str):
        return ""
    return payload[1]


def _extract_items(decoded_payload: str) -> list[dict[str, object]]:
    marker = '"items":'
    start = decoded_payload.find(marker)
    if start < 0:
        return []

    try:
        raw_items, _ = json.JSONDecoder().raw_decode(decoded_payload[start + len(marker) :])
    except json.JSONDecodeError:
        return []
    if not isinstance(raw_items, list):
        return []

    seen_keys: set[str] = set()
    parsed: list[dict[str, object]] = []
    for item in raw_items:
        if not isinstance(item, dict):
            continue

        slug = _require(str(item.get("slug", "")), "key", LIST_URL)
        if slug in seen_keys:
            continue

        url = _normalize_post_url(f"{BASE_URL}/blog/{slug}")
        title = _require(str(item.get("title", "")), "title", url)
        category = normalize_tag(str(item.get("category", "")))
        tags = []
        if category:
            tags.append(category)
        for raw_tag in item.get("tags", []):
            tag = normalize_tag(str(raw_tag))
            if tag and tag not in tags:
                tags.append(tag)

        parsed.append(
            {
                "key": slug,
                "title": title,
                "description": str(item.get("summary", "")).strip(),
                "tags": tags,
                "thumbnail": normalize_url(BASE_URL, str(item.get("coverUrl", "")).strip()) if item.get("coverUrl") else "",
                "publishedAt": _parse_list_datetime(str(item.get("date", "")).strip()),
                "url": url,
            }
        )
        seen_keys.add(slug)

    return parsed


def _fetch_detail(url: str) -> dict[str, str]:
    doc = _fetch_detail_document(url)
    thumbnail = ""
    for selector in [
        'meta[property="og:image"]',
        'meta[property="og:image:secure_url"]',
        'meta[name="og:image"]',
        'meta[name="twitter:image"]',
        'meta[property="twitter:image"]',
        'meta[name="twitter:image:src"]',
    ]:
        element = doc.select_first(selector)
        if element and element.attr("content"):
            thumbnail = element.abs_url("content") or element.attr("content")
            break

    published_at = ""
    for selector in [
        'meta[property="article:published_time"]',
        'meta[name="article:published_time"]',
        "time[datetime]",
    ]:
        element = doc.select_first(selector)
        if not element:
            continue
        raw = element.attr("content") or element.attr("datetime")
        if raw:
            published_at = _parse_detail_datetime(raw)
            if published_at:
                break

    return {"thumbnail": thumbnail, "publishedAt": published_at}


def _fetch_detail_document(url: str):
    current_url = url
    for _ in range(5):
        try:
            return fetch_html(current_url)
        except HTTPError as error:
            if error.code != 308:
                raise
            location = error.headers.get("Location", "").strip()
            if not location:
                raise
            current_url = normalize_url(current_url, location)
    raise RuntimeError(f"{KEY} crawl finished but too many redirects for {url}")


def _parse_list_datetime(raw: str) -> str:
    if not raw:
        return ""
    for parser in (
        lambda value: datetime.strptime(value, "%a %b %d %Y %H:%M:%S GMT%z (%Z)"),
        lambda value: parsedate_to_datetime(value),
    ):
        try:
            parsed = parser(raw)
            if parsed.tzinfo is not None:
                parsed = parsed.astimezone(timezone.utc).replace(tzinfo=None)
            return parsed.replace(microsecond=0).isoformat(timespec="seconds")
        except (TypeError, ValueError):
            continue
    return ""


def _parse_detail_datetime(raw: str) -> str:
    text = raw.strip()
    if not text:
        return ""
    for parser in (
        lambda value: datetime.fromisoformat(value.replace("Z", "+00:00")),
        lambda value: datetime.strptime(value, "%a %b %d %Y %H:%M:%S GMT%z (%Z)"),
        lambda value: parsedate_to_datetime(value),
        lambda value: datetime.strptime(value, "%b %d, %Y"),
    ):
        try:
            parsed = parser(text)
            if parsed.tzinfo is not None:
                parsed = parsed.astimezone(timezone.utc).replace(tzinfo=None)
            return parsed.replace(microsecond=0).isoformat(timespec="seconds")
        except (TypeError, ValueError):
            continue
    return ""


def _normalize_post_url(url: str) -> str:
    parsed = urlsplit(url)
    encoded_path = quote(parsed.path, safe="/%")
    return urlunsplit((parsed.scheme, parsed.netloc, encoded_path, parsed.query, ""))


def _require(value: str, field: str, url: str) -> str:
    trimmed = value.strip()
    if not trimmed:
        raise RuntimeError(f"{KEY} crawl finished but missing {field} for {url or 'unknown'}")
    return trimmed
