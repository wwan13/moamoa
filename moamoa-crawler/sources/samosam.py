from __future__ import annotations

from urllib.error import HTTPError
from urllib.parse import urlsplit

from _common import Post, fetch_html, make_payload_raw as make_payload, normalize_tag


KEY = "samosam"
BLOG = "3o3 Tech Blog"
BASE_URL = "https://blog.3o3.co.kr/tag/tech"


def crawl(request, config) -> dict[str, object]:
    del config
    seen_keys: set[str] = set()
    posts: list[Post] = []
    page = 1

    while request.size is None or len(posts) < request.size:
        try:
            doc = fetch_html(_build_list_url(page))
        except HTTPError as error:
            if error.code == 404 and page > 1:
                break
            raise
        items = doc.select("article.post-card")
        if not items:
            break

        new_posts: list[Post] = []
        for item in items:
            link_el = item.select_first("a.post-card-content-link")
            title_el = item.select_first("h2.post-card-title")
            thumb_el = item.select_first("img.post-card-image")
            if link_el is None or title_el is None or thumb_el is None:
                continue

            url = link_el.abs_url("href").strip()
            key = _extract_key(url)
            title = title_el.text().strip()
            published_at = _extract_published_at(item)
            thumbnail = thumb_el.abs_url("src").strip()
            if not url or not key or not title or not thumbnail or not published_at or key in seen_keys:
                continue

            excerpt_el = item.select_first(".post-card-excerpt")
            new_posts.append(
                Post(
                    key=key,
                    title=title,
                    description=excerpt_el.text().strip() if excerpt_el else "",
                    tags=_extract_tags(item),
                    thumbnail=thumbnail,
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
        raise RuntimeError(f"{KEY} crawl finished but no post cards were extracted from {_build_list_url(1)}")

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
    return f"{BASE_URL}/" if page == 1 else f"{BASE_URL}/page/{page}/"


def _extract_key(url: str) -> str:
    return urlsplit(url).path.rstrip("/").split("/")[-1].strip()


def _extract_published_at(item) -> str:
    time_el = item.select_first("time.post-card-meta-date")
    if time_el is None:
        return ""
    raw = (time_el.attr("datetime") or time_el.text()).strip()
    if not raw:
        return ""

    parts = [part.strip() for part in raw.split(".") if part.strip()]
    if len(parts) == 3:
        year, month, day = (int(part) for part in parts)
        return f"{year:04d}-{month:02d}-{day:02d}T00:00:00"
    return ""


def _extract_tags(item) -> list[str]:
    tags = []
    for tag_el in item.select(".post-card-tags .post-card-primary-tag"):
        tag = normalize_tag(tag_el.text())
        if tag and tag not in tags:
            tags.append(tag)
    return tags
