from __future__ import annotations

from concurrent.futures import ThreadPoolExecutor
from urllib.parse import urlsplit

from _common import Post, fetch_html, make_payload_raw as make_payload, normalize_published_at, normalize_space


KEY = "upstage"
BLOG = "The Upstage Blog"
BASE_URL = "https://www.upstage.ai"
LIST_URL = f"{BASE_URL}/blog"
KO_PATH_PREFIX = "/blog/ko/"
EXCLUDED_KEYS = {"console-kr"}


def crawl(request, config) -> dict[str, object]:
    del config
    doc = fetch_html(LIST_URL)
    cards = doc.select(f'a.all-blog-card-v2[href*="{KO_PATH_PREFIX}"]')
    if not cards:
        raise RuntimeError(f"{KEY} crawl finished but no post links were extracted from {LIST_URL}")

    collected: list[dict[str, str]] = []
    seen_keys: set[str] = set()
    for card in cards:
        post = _extract_post(card)
        if post is None or post["key"] in EXCLUDED_KEYS or post["key"] in seen_keys:
            continue
        collected.append(post)
        seen_keys.add(post["key"])
        if request.size is not None and len(collected) >= request.size:
            break

    if not collected:
        raise RuntimeError(f"{KEY} crawl finished but no post links were extracted from {LIST_URL}")

    with ThreadPoolExecutor(max_workers=10) as executor:
        details = executor.map(lambda item: _fetch_detail(item["url"]), collected)
        posts = [
            Post(
                key=item["key"],
                title=item["title"],
                description=detail["description"],
                tags=item["tags"],
                thumbnail=item["thumbnail"] or detail["thumbnail"],
                publishedAt=item["publishedAt"] or detail["publishedAt"],
                url=item["url"],
                source="html",
            )
            for item, detail in zip(collected, details)
        ]

    return make_payload(
        key=KEY,
        blog=BLOG,
        base_url=BASE_URL,
        requested_url=LIST_URL,
        crawler="html.urllib",
        requested_size=request.size,
        posts=posts,
    )


def _extract_post(card) -> dict[str, str] | None:
    url = card.abs_url("href")
    if not _is_post_url(url):
        return None

    title = _text(card.select_first("h3"))
    if not title:
        return None

    key = _extract_key(url)
    if not key:
        return None

    return {
        "key": key,
        "title": title,
        "tags": _extract_tags(card),
        "thumbnail": _extract_thumbnail(card),
        "publishedAt": _extract_published_at(card),
        "url": url,
    }


def _fetch_detail(url: str) -> dict[str, str]:
    doc = fetch_html(url)
    description = ""
    for selector in (
        'meta[name="description"]',
        'meta[property="og:description"]',
        'meta[name="twitter:description"]',
    ):
        node = doc.select_first(selector)
        if node is None:
            continue
        description = normalize_space(node.attr("content"))
        if description:
            break
    if not description:
        for node in doc.select(".text-rich-text p"):
            description = _text(node)
            if description:
                break
    if not description:
        for node in doc.select("main p"):
            candidate = _text(node)
            if _is_description(candidate):
                description = candidate
                break

    thumbnail = ""
    for selector in (
        'meta[property="og:image"]',
        'meta[name="twitter:image"]',
    ):
        node = doc.select_first(selector)
        if node is None:
            continue
        thumbnail = node.abs_url("content") or node.attr("content")
        if thumbnail:
            break

    published_at = _extract_detail_published_at(doc)
    return {
        "description": description,
        "thumbnail": thumbnail,
        "publishedAt": published_at,
    }


def _extract_tags(card) -> list[str]:
    tags: list[str] = []
    for node in card.select(".blog-card-pill"):
        tag = _text(node)
        if tag and tag not in tags:
            tags.append(tag)
    return tags


def _extract_thumbnail(card) -> str:
    image = card.select_first("img.blog-card-image")
    if image is None:
        return ""
    return image.abs_url("src") or image.abs_url("srcset")


def _extract_published_at(card) -> str:
    author_wrap = card.select_first(".blog-card-author-wrap")
    if author_wrap is None:
        return ""
    values = [_text(node) for node in author_wrap.select("div")]
    for value in reversed(values):
        normalized = normalize_published_at(value)
        if normalized:
            return normalized
    return ""


def _extract_detail_published_at(doc) -> str:
    meta_wrap = doc.select_first(".blog-hero-meta-wrap")
    if meta_wrap is None:
        return ""
    for node in meta_wrap.select("div"):
        normalized = normalize_published_at(_text(node))
        if normalized:
            return normalized
    return ""


def _extract_key(url: str) -> str:
    path = urlsplit(url).path.rstrip("/")
    return path.split("/")[-1]


def _is_post_url(url: str) -> bool:
    parsed = urlsplit(url)
    if parsed.netloc != urlsplit(BASE_URL).netloc:
        return False
    if parsed.path.rstrip("/") == "/blog":
        return False
    return parsed.path.startswith(KO_PATH_PREFIX)


def _text(node) -> str:
    if node is None:
        return ""
    return normalize_space(node.text())


def _is_description(value: str) -> bool:
    if len(value) < 20:
        return False
    blocked = {
        "Share",
        "Start building with our API or talk to our team.",
    }
    return value not in blocked
