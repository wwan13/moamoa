from __future__ import annotations

from concurrent.futures import ThreadPoolExecutor
from datetime import datetime
from urllib.error import HTTPError
from urllib.parse import urlsplit

from _common import Post, fetch_html, make_payload_raw as make_payload, normalize_tag


KEY = "elevenst"
BLOG = "11ST Tech Blog"
BASE_URL = "https://11st-tech.github.io"


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
        items = doc.select("ul#post-list > li.post-item")
        if not items:
            break

        basic_posts: list[dict[str, object]] = []
        for item in items:
            link_el = next((link for link in item.select("a[href]") if link.select_first("h3.post-title")), None) or item.select_first("a[href]")
            title_el = item.select_first("h3.post-title")
            if link_el is None or title_el is None:
                continue
            url = link_el.abs_url("href").strip()
            key = _extract_key(url)
            if not url or not key or not title_el.text() or key in seen_keys:
                continue
            tags = []
            for tag_el in item.select("p.post-tags a.tag, p.post-tags a"):
                tag = normalize_tag(tag_el.text())
                if tag and tag not in tags:
                    tags.append(tag)
            basic_posts.append(
                {
                    "key": key,
                    "title": title_el.text(),
                    "description": item.select_first("p.post-excerpt").text() if item.select_first("p.post-excerpt") else "",
                    "tags": tags,
                    "url": url,
                }
            )
            seen_keys.add(key)

        if not basic_posts:
            break

        with ThreadPoolExecutor(max_workers=10) as executor:
            published_ats = executor.map(lambda base: _fetch_published_at_from_url(base["url"]), basic_posts)
            for base, published_at in zip(basic_posts, published_ats):
                posts.append(
                    Post(
                        key=base["key"],
                        title=base["title"],
                        description=base["description"],
                        tags=base["tags"],
                        thumbnail="",
                        publishedAt=published_at,
                        url=base["url"],
                        source="html",
                    )
                )
                if request.size and len(posts) >= request.size:
                    break

        if request.size and len(posts) >= request.size:
            break
        page += 1

    if not posts:
        raise RuntimeError(f"{KEY} crawl finished but no post items were extracted from {_build_list_url(1)}")

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


def _fetch_published_at_from_url(url: str) -> str:
    detail = fetch_html(url)
    return _fetch_published_at(detail)


def _fetch_published_at(detail) -> str:
    meta = detail.select_first('meta[property="article:published_time"]')
    if meta and meta.attr("content"):
        return _parse_published_at(meta.attr("content"))
    post_date = detail.select_first("#post-date, .post-date")
    if post_date and post_date.text():
        return _parse_published_at(post_date.text())
    time_el = detail.select_first("time[datetime]")
    if time_el and time_el.attr("datetime"):
        return _parse_published_at(time_el.attr("datetime"))
    return ""


def _parse_published_at(raw: str) -> str:
    text = raw.strip().replace("Z", "+00:00")
    if not text:
        return ""
    try:
        return datetime.fromisoformat(text).replace(microsecond=0).isoformat(timespec="seconds")
    except ValueError:
        pass
    try:
        return datetime.strptime(text, "%Y-%m-%d").strftime("%Y-%m-%dT00:00:00")
    except ValueError:
        return ""


def _extract_key(url: str) -> str:
    return urlsplit(url).path.rstrip("/")
