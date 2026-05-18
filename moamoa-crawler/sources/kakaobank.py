from __future__ import annotations

from urllib.error import HTTPError
from datetime import datetime
from urllib.parse import urlsplit

from _common import Post, fetch_html, make_payload_raw as make_payload


KEY = "kakaobank"
BLOG = "KakaoBank Tech"
BASE_URL = "https://tech.kakaobank.com"


def crawl(request, config) -> dict[str, object]:
    del config
    page = 1
    seen_keys: set[str] = set()
    posts: list[Post] = []

    while request.size is None or len(posts) < request.size:
        try:
            doc = fetch_html(_build_url(page))
        except HTTPError as error:
            if error.code == 403 and page > 1:
                break
            raise
        items = doc.select("div.post")
        if not items:
            break

        new_posts = []
        for item in items:
            link_el = item.select_first("h2.post-title > a")
            if link_el is None:
                continue

            url = link_el.abs_url("href")
            key = _extract_key(url)
            title = link_el.text().strip()
            if not url or not key or not title or key in seen_keys:
                continue

            summary_el = item.select_first("div.post-summary")
            date_el = item.select_first("div.post-info > div.date")

            new_posts.append(
                Post(
                    key=key,
                    title=title,
                    description=summary_el.text().strip() if summary_el else "",
                    tags=_extract_categories(item),
                    thumbnail="",
                    publishedAt=_parse_published_at(date_el.text().strip() if date_el else ""),
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
        raise RuntimeError(f"{KEY} crawl finished but no posts were extracted from {_build_url(1)}")

    return make_payload(
        key=KEY,
        blog=BLOG,
        base_url=BASE_URL,
        requested_url=_build_url(1),
        crawler="html.urllib",
        requested_size=request.size,
        posts=posts,
    )


def _build_url(page: int) -> str:
    return BASE_URL if page == 1 else f"{BASE_URL}/page/{page}/"


def _extract_key(url: str) -> str:
    return urlsplit(url).path.rstrip("/").split("/")[-1]


def _extract_categories(item) -> list[str]:
    categories: list[str] = []
    for tag_el in item.select("div.category a, div.sidebar-tags a"):
        tag = tag_el.text().strip()
        if tag and tag not in categories:
            categories.append(tag)
    return categories


def _parse_published_at(raw: str) -> str:
    text = raw.strip()
    if not text:
        return ""
    try:
        return datetime.strptime(text, "%Y-%m-%d").strftime("%Y-%m-%dT00:00:00")
    except ValueError:
        return ""
