from __future__ import annotations

import re
from urllib.parse import urlsplit, urlunsplit

from _common import Post, fetch_text, make_payload_raw as make_payload, normalize_published_at, normalize_tag, parse_html, parse_xml, strip_html, unique_posts


KEY = "tonymoly"
BLOG = "토니모리 테크블로그 : TonyTech"
BASE_URL = "https://tonymoly-tech.medium.com"
RSS_URL = f"{BASE_URL}/feed"


def crawl(request, config) -> dict[str, object]:
    del config

    posts = _fetch_rss_posts()
    posts = unique_posts(posts, request.size)
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


def _fetch_rss_posts() -> list[Post]:
    root = parse_xml(fetch_text(RSS_URL))
    items = root.xpath(".//item")
    if not items:
        raise RuntimeError(f"{KEY} crawl finished but no RSS items were extracted from {RSS_URL}")

    posts: list[Post] = []
    for item in items:
        url = _normalize_post_url(_xpath_text(item, "link"))
        if not _is_post_url(url):
            continue

        title = _xpath_text(item, "title").strip()
        if not title:
            continue

        encoded_html = _xpath_text(item, "*[local-name()='encoded']")
        description = _extract_description(encoded_html)
        thumbnail = _extract_thumbnail(encoded_html)
        published_at = normalize_published_at(_xpath_text(item, "pubDate")) or normalize_published_at(_xpath_text(item, "*[local-name()='updated']"))
        tags = _extract_tags(item)

        posts.append(
            Post(
                key=_key_from_url(url),
                title=title,
                description=description,
                tags=tags,
                thumbnail=thumbnail,
                publishedAt=published_at,
                url=url,
                source="rss",
            )
        )

    return posts


def _extract_description(encoded_html: str) -> str:
    if not encoded_html:
        return ""

    doc = parse_html(encoded_html, BASE_URL)
    parts: list[str] = []

    subtitle = doc.select_first("h3")
    if subtitle and subtitle.text():
        parts.append(subtitle.text())

    quote = doc.select_first("blockquote")
    if quote and quote.text():
        parts.append(quote.text())

    for paragraph in doc.select("p"):
        text = paragraph.text()
        if not text:
            continue
        if text.startswith("Continue reading on Medium"):
            continue
        if text == "GPT 요약":
            continue
        parts.append(text)
        if len(parts) >= 2:
            break

    if parts:
        return " ".join(parts).strip()
    return strip_html(encoded_html)


def _extract_thumbnail(encoded_html: str) -> str:
    if not encoded_html:
        return ""

    doc = parse_html(encoded_html, BASE_URL)
    image = doc.select_first("img[src]")
    if image:
        return image.abs_url("src")

    match = re.search(r'<img[^>]+src=["\']([^"\']+)["\']', encoded_html, flags=re.IGNORECASE)
    return match.group(1).strip() if match else ""


def _extract_tags(item) -> list[str]:
    tags: list[str] = []
    for category in item.xpath("./category"):
        tag = normalize_tag("".join(category.itertext()).strip())
        if tag and tag not in tags:
            tags.append(tag)
    return tags


def _xpath_text(node, expr: str) -> str:
    values = node.xpath(f"./{expr}/text()")
    return "".join(value.strip() for value in values if value and value.strip())


def _normalize_post_url(url: str) -> str:
    parsed = urlsplit(url.strip())
    return urlunsplit((parsed.scheme, parsed.netloc, parsed.path, "", ""))


def _is_post_url(url: str) -> bool:
    parsed = urlsplit(url)
    if parsed.netloc != "tonymoly-tech.medium.com":
        return False
    if parsed.path in {"", "/", "/all"}:
        return False
    if parsed.path.startswith(("/tagged/", "/m/", "/me/", "/search")):
        return False
    return re.search(r"-[0-9a-f]{12,}/?$", parsed.path) is not None


def _key_from_url(url: str) -> str:
    path = urlsplit(url).path.rstrip("/")
    return path.split("/")[-1]
