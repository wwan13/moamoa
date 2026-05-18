from __future__ import annotations

from datetime import datetime
from urllib.error import HTTPError
from urllib.parse import urlsplit

from _common import Post, fetch_html, make_payload_raw as make_payload, normalize_tag


KEY = "nds"
BLOG = "NDS Tech Insight"
BASE_URL = "https://tech.cloud.nongshim.co.kr"
ELLIPSIS = "…"


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
        articles = doc.select("#blog-entries article.blog-entry")
        if not articles:
            break

        new_posts: list[Post] = []
        for article in articles:
            link_el = article.select_first('h2.blog-entry-title a[rel="bookmark"][href]')
            if link_el is None:
                continue

            url = link_el.abs_url("href").strip()
            key = _extract_key(url)
            title = link_el.text().strip()
            if not url or not key or not title or key in seen_keys:
                continue

            tags = []
            for tag_el in article.select(".blog-entry-category a[href]"):
                tag = normalize_tag(tag_el.text())
                if tag and tag not in tags:
                    tags.append(tag)

            thumb_el = article.select_first(".thumbnail img[src]")
            date_el = article.select_first(".blog-entry-date")
            published_at = _parse_published_at(date_el.text().strip() if date_el else "")
            if not published_at:
                continue
            new_posts.append(
                Post(
                    key=key,
                    title=title,
                    description=_extract_description(article),
                    tags=tags,
                    thumbnail=thumb_el.abs_url("src").strip() if thumb_el else "",
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
        raise RuntimeError(f"{KEY} crawl finished but no articles were extracted from {_build_list_url(1)}")

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
    return f"{BASE_URL}/post/" if page == 1 else f"{BASE_URL}/post/page/{page}/"


def _extract_key(url: str) -> str:
    return urlsplit(url).path.rstrip("/").split("/")[-1].strip()


def _extract_description(article) -> str:
    summary = article.select_first(".blog-entry-summary")
    if summary is None:
        return ""
    text = summary.text().strip()
    return "" if not text or text == ELLIPSIS else text


def _parse_published_at(raw: str) -> str:
    text = raw.strip()
    if not text:
        return ""
    try:
        return datetime.strptime(text, "%Y-%m-%d").strftime("%Y-%m-%dT00:00:00")
    except ValueError:
        return ""
