from __future__ import annotations

from concurrent.futures import ThreadPoolExecutor
from datetime import datetime, timezone
from email.utils import parsedate_to_datetime
from urllib.parse import urlsplit

from _common import Post, fetch_html, fetch_text, make_payload_raw as make_payload, normalize_tag, parse_xml, strip_html


KEY = "ab180"
BLOG = "AB180 Engineering"
BASE_URL = "https://engineering.ab180.co"
RSS_URL = "https://raw.githubusercontent.com/ab180/engineering-blog-rss-scheduler/main/rss.xml"
STORY_PATH_MARKER = "/stories/"


def crawl(request, config) -> dict[str, object]:
    del config
    items = _fetch_rss_items()
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
                    thumbnail=detail["thumbnail"],
                    publishedAt=published_at,
                    url=item["url"],
                    source="rss",
                )
            )

    if not posts:
        raise RuntimeError(f"{KEY} crawl finished but no RSS items were extracted from {RSS_URL}")

    return make_payload(
        key=KEY,
        blog=BLOG,
        base_url=BASE_URL,
        requested_url=RSS_URL,
        crawler="rss.urllib",
        requested_size=request.size,
        posts=posts,
    )


def _fetch_rss_items() -> list[dict[str, object]]:
    root = parse_xml(fetch_text(RSS_URL))
    elements = root.xpath(".//item")
    if not elements:
        raise RuntimeError(f"{KEY} crawl finished but no RSS items were extracted from {RSS_URL}")

    seen_keys: set[str] = set()
    parsed: list[dict[str, object]] = []
    for item in elements:
        url = _extract_url(item)
        if STORY_PATH_MARKER not in url:
            continue

        key = _extract_key(url)
        if not key or key in seen_keys:
            continue

        title = _require(_xpath_text(item, "title"), "title", url)
        raw_description = _xpath_text(item, "*[local-name()='encoded']") or _xpath_text(item, "description")
        tags = []
        for category in item.xpath("./category"):
            tag = normalize_tag("".join(category.itertext()).strip())
            if tag and tag not in tags:
                tags.append(tag)

        parsed.append(
            {
                "key": key,
                "title": title,
                "description": strip_html(raw_description) if raw_description else "",
                "tags": tags,
                "publishedAt": _parse_rfc_1123(_xpath_text(item, "pubDate")),
                "url": url,
            }
        )
        seen_keys.add(key)

    if not parsed:
        raise RuntimeError(f"{KEY} crawl finished but no RSS items were extracted from {RSS_URL}")
    return parsed


def _fetch_detail(url: str) -> dict[str, str]:
    doc = fetch_html(url)
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
    if not thumbnail:
        raise RuntimeError(f"{KEY} crawl finished but missing thumbnail for {url}")

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


def _xpath_text(node, expr: str) -> str:
    values = node.xpath(f"./{expr}/text()")
    return "".join(value.strip() for value in values if value and value.strip())


def _extract_url(item) -> str:
    links = ["".join(link.itertext()).strip() for link in item.xpath("./link") if "".join(link.itertext()).strip()]
    url = next((value for value in links if STORY_PATH_MARKER in value), "") or (links[0] if links else "")
    return _require(url, "url", "")


def _extract_key(url: str) -> str:
    path = urlsplit(url).path.rstrip("/")
    key = path.removeprefix(STORY_PATH_MARKER)
    return _require(key, "key", url)


def _parse_rfc_1123(raw: str) -> str:
    if not raw:
        return ""
    try:
        return parsedate_to_datetime(raw).astimezone(timezone.utc).replace(tzinfo=None, microsecond=0).isoformat(timespec="seconds")
    except (TypeError, ValueError):
        return ""


def _parse_detail_datetime(raw: str) -> str:
    text = raw.strip()
    if not text:
        return ""
    for parser in (
        lambda value: datetime.fromisoformat(value.replace("Z", "+00:00")),
        lambda value: datetime.strptime(value, "%Y-%m-%d"),
    ):
        try:
            parsed = parser(text)
            if parsed.tzinfo is not None:
                parsed = parsed.astimezone(timezone.utc).replace(tzinfo=None)
            return parsed.replace(microsecond=0).isoformat(timespec="seconds")
        except ValueError:
            continue
    return ""


def _require(value: str, field: str, url: str) -> str:
    trimmed = value.strip()
    if not trimmed:
        raise RuntimeError(f"{KEY} crawl finished but missing {field} for {url or 'unknown'}")
    return trimmed
