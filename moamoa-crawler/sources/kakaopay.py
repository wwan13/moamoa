from __future__ import annotations

from urllib.error import HTTPError
from urllib.parse import urlsplit

from _common import Post, fetch_html, make_payload_raw as make_payload, normalize_tag


KEY = "kakaopay"
BLOG = "KakaoPay Tech"
BASE_URL = "https://tech.kakaopay.com"


def crawl(request, config) -> dict[str, object]:
    del config
    page = 1
    seen_keys: set[str] = set()
    posts: list[Post] = []

    while request.size is None or len(posts) < request.size:
        try:
            doc = fetch_html(_build_list_url(page))
        except HTTPError as error:
            if error.code == 404 and page > 1:
                break
            raise
        items = [item for item in doc.select('li[class*="_postListItem_"]') if item.select_first('a[href^="/post/"]')]
        if not items:
            items = [item for item in doc.select("li") if item.select_first('a[href^="/post/"]')]
        if not items:
            break

        new_posts: list[Post] = []
        for item in items:
            link_el = item.select_first('a[href^="/post/"]')
            title_el = item.select_first("strong")
            time_el = item.select_first("time")
            if link_el is None or title_el is None or time_el is None:
                continue

            href = link_el.attr("href").strip()
            url = link_el.abs_url("href").strip()
            key = _extract_key_from_href(href)
            title = title_el.text().strip()
            description_el = item.select_first('div[class*="_postInfo_"] p') or item.select_first("p")
            description = description_el.text().strip() if description_el else ""
            published_at = _parse_published_at(time_el.text())
            if not url or not key or not title or not published_at or key in seen_keys:
                continue

            thumbnail_el = item.select_first("img[alt]")
            new_posts.append(
                Post(
                    key=key,
                    title=title,
                    description=description,
                    tags=_fetch_categories(url),
                    thumbnail=thumbnail_el.abs_url("src").strip() if thumbnail_el else "",
                    publishedAt=published_at,
                    url=url,
                    source="html",
                )
            )
            seen_keys.add(key)

        if not new_posts:
            break

        posts.extend(new_posts)
        page += 1

    if request.size is not None:
        posts = posts[: request.size]
    if not posts:
        raise RuntimeError(f"{KEY} crawl finished but no posts were extracted from {_build_list_url(1)}")

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
    return BASE_URL if page == 1 else f"{BASE_URL}/page/{page}/"


def _fetch_categories(post_url: str) -> list[str]:
    try:
        detail = fetch_html(post_url)
    except Exception:
        return []

    categories: list[str] = []
    for tag_el in detail.select('a[href^="/tag/"]'):
        tag = normalize_tag(tag_el.text())
        if tag and tag not in categories:
            categories.append(tag)
    return categories


def _extract_key_from_href(href: str) -> str:
    cleaned = href.split("?", 1)[0].split("#", 1)[0].strip()
    path = urlsplit(cleaned).path if "://" in cleaned else cleaned
    return path.removeprefix("/post/").strip("/").strip()


def _parse_published_at(raw: str) -> str:
    parts = [part.strip() for part in raw.split(".") if part.strip()]
    if len(parts) != 3:
        return ""
    year, month, day = (int(part) for part in parts)
    return f"{year:04d}-{month:02d}-{day:02d}T00:00:00"
