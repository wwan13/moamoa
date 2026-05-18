from __future__ import annotations

from concurrent.futures import ThreadPoolExecutor
from datetime import datetime
from urllib.error import HTTPError
from urllib.parse import urlsplit

from _common import Post, fetch_html, make_payload_raw as make_payload, normalize_tag


KEY = "banksalad"
BLOG = "Banksalad Tech Blog"
BASE_URL = "https://blog.banksalad.com"


def crawl(request, config) -> dict[str, object]:
    del config
    page = 1
    seen_keys: set[str] = set()
    posts: list[Post] = []

    while request.size is None or len(posts) < request.size:
        try:
            doc = fetch_html(_build_list_url(page))
        except HTTPError as error:
            if error.code in (403, 404):
                break
            raise
        cards = doc.select(".post_card")
        if not cards:
            break

        new_posts: list[dict[str, object]] = []
        for card in cards:
            title_el = card.select_first('.post_title a[href^="/tech/"]')
            if title_el is None:
                raise RuntimeError(f"{KEY} crawl finished but missing url for unknown")

            url = _require(title_el.abs_url("href"), "url", "")
            key = _extract_key(url)
            title = _require(title_el.text(), "title", url)
            if key in seen_keys:
                continue

            tags = []
            for tag_el in card.select('.post_tags a[href^="/tags/"]'):
                tag = normalize_tag(tag_el.text())
                if tag and tag not in tags:
                    tags.append(tag)

            thumbnail = _extract_thumbnail(card)
            if not thumbnail:
                raise RuntimeError(f"{KEY} crawl finished but missing thumbnail for {url}")

            new_posts.append(
                {
                    "key": key,
                    "title": title,
                    "description": card.select_first(".excerpt").text() if card.select_first(".excerpt") else "",
                    "tags": tags,
                    "thumbnail": thumbnail,
                    "url": url,
                }
            )
            seen_keys.add(key)

        if not new_posts:
            break

        with ThreadPoolExecutor(max_workers=10) as executor:
            details = executor.map(lambda item: _fetch_detail(item["url"]), new_posts)
            for item, detail in zip(new_posts, details):
                resolved_thumbnail = item["thumbnail"] or detail["thumbnail"]
                if not resolved_thumbnail:
                    raise RuntimeError(f"{KEY} crawl finished but missing thumbnail for {item['url']}")
                posts.append(
                    Post(
                        key=item["key"],
                        title=item["title"],
                        description=item["description"],
                        tags=item["tags"],
                        thumbnail=resolved_thumbnail,
                        publishedAt=detail["publishedAt"],
                        url=item["url"],
                        source="html",
                    )
                )
        page += 1

    if request.size is not None:
        posts = posts[: request.size]
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
    return f"{BASE_URL}/tech/" if page == 1 else f"{BASE_URL}/tech/page/{page}/"


def _extract_thumbnail(card) -> str:
    img = card.select_first(".post_preview img[data-main-image]") or card.select_first(".post_preview img[alt]")
    if img is None:
        return ""
    return img.abs_url("data-src") or img.abs_url("src")


def _fetch_detail(url: str) -> dict[str, str]:
    detail = fetch_html(url)
    detail_published = detail.select_first(".post_details > span")
    detail_thumbnail = detail.select_first('meta[property="og:image"]')
    return {
        "publishedAt": _parse_published_at(detail_published.text() if detail_published else "", url),
        "thumbnail": detail_thumbnail.attr("content").strip() if detail_thumbnail else "",
    }


def _parse_published_at(raw: str, url: str = "") -> str:
    text = raw.strip()
    if not text:
        return ""
    try:
        return datetime.strptime(text, "%d %b, %Y").strftime("%Y-%m-%dT00:00:00")
    except ValueError as error:
        raise RuntimeError(f"{KEY} crawl finished but missing publishedAt for {url or 'unknown'}") from error


def _extract_key(url: str) -> str:
    key = urlsplit(url).path.rstrip("/").split("/")[-1]
    return _require(key, "key", url)


def _require(value: str, field: str, url: str) -> str:
    trimmed = value.strip()
    if not trimmed:
        raise RuntimeError(f"{KEY} crawl finished but missing {field} for {url or 'unknown'}")
    return trimmed
