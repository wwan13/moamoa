from __future__ import annotations

from concurrent.futures import ThreadPoolExecutor
from datetime import datetime
from urllib.error import HTTPError
from urllib.parse import unquote

from _common import Post, fetch_html, make_payload_raw as make_payload, normalize_tag


KEY = "com2us"
BLOG = "Com2uS Tech Blog"
BASE_URL = "https://on.com2us.com/tag/%EA%B8%B0%EC%88%A0%EB%B8%94%EB%A1%9C%EA%B7%B8/"


def crawl(request, config) -> dict[str, object]:
    del config
    page = 1
    posts: list[Post] = []
    seen_keys: set[str] = set()

    while request.size is None or len(posts) < request.size:
        try:
            doc = fetch_html(_build_list_url(page))
        except HTTPError as error:
            if error.code in (403, 404):
                break
            raise
        items = doc.select("section.archive-list a.loop-grid.loop[href]")
        if not items:
            break

        new_posts: list[dict[str, object]] = []
        for link in items:
            url = link.abs_url("href")
            key = unquote(url.rstrip("/").split("/")[-1])
            title_el = link.select_first("div.title h4") or link.select_first("h4")
            if not url or not key or title_el is None or not title_el.text() or key in seen_keys:
                continue

            thumb_el = link.select_first("div.image img[src]") or link.select_first("div.image img[src]")
            tags = []
            for tag_el in link.select("div.tags li span"):
                tag = normalize_tag(tag_el.text())
                if tag and tag not in tags:
                    tags.append(tag)

            new_posts.append(
                {
                    "key": key,
                    "title": title_el.text(),
                    "description": link.select_first("div.content p").text() if link.select_first("div.content p") else "",
                    "tags": tags,
                    "thumbnail": thumb_el.abs_url("src") if thumb_el else "",
                    "url": url,
                }
            )
            seen_keys.add(key)

        if not new_posts:
            break

        with ThreadPoolExecutor(max_workers=10) as executor:
            published_ats = executor.map(lambda item: _fetch_published_at(item["url"]), new_posts)
            for item, published_at in zip(new_posts, published_ats):
                posts.append(
                    Post(
                        key=item["key"],
                        title=item["title"],
                        description=item["description"],
                        tags=item["tags"],
                        thumbnail=item["thumbnail"],
                        publishedAt=published_at,
                        url=item["url"],
                        source="html",
                    )
                )
        page += 1

    if request.size is not None:
        posts = posts[: request.size]
    if not posts:
        raise RuntimeError(f"{KEY} crawl finished but no cards were extracted from {_build_list_url(1)}")

    return make_payload(
        key=KEY,
        blog=BLOG,
        base_url="https://on.com2us.com",
        requested_url=_build_list_url(1),
        crawler="html.urllib",
        requested_size=request.size,
        posts=posts,
    )


def _build_list_url(page: int) -> str:
    return BASE_URL if page == 1 else f"{BASE_URL}page/{page}/"


def _fetch_published_at(url: str) -> str:
    detail = fetch_html(url)
    meta = detail.select_first('meta[property="article:published_time"], meta[name="article:published_time"]')
    time_el = detail.select_first("time[datetime]")
    return _parse_datetime(meta.attr("content") if meta else "") or _parse_datetime(time_el.attr("datetime") if time_el else "")


def _parse_datetime(raw: str) -> str:
    text = raw.strip()
    if not text:
        return ""
    text = text.replace("Z", "+00:00")
    try:
        return datetime.fromisoformat(text).replace(microsecond=0).isoformat(timespec="seconds")
    except ValueError:
        return ""
