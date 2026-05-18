from __future__ import annotations

from urllib.error import HTTPError
from urllib.parse import urlsplit

from _common import Post, fetch_html, make_payload_raw as make_payload


KEY = "socar"
BLOG = "Socar Tech Blog"
BASE_URL = "https://tech.socarcorp.kr"


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
        items = doc.select("article.post-preview")
        if not items:
            break

        new_posts: list[Post] = []
        for item in items:
            link_el = item.select_first("a[href]")
            title_el = item.select_first("h2.post-title")
            if link_el is None or title_el is None:
                continue

            url = link_el.abs_url("href").strip()
            key = _extract_key_from_url(url)
            title = title_el.text().strip()
            if not url or not key or not title or key in seen_keys:
                continue

            tags = []
            for tag_el in item.select("span.tag a"):
                tag = tag_el.text().strip()
                if tag and tag not in tags:
                    tags.append(tag)

            subtitle_el = item.select_first("h3.post-subtitle")
            date_el = item.select_first("span.date")
            published_at = _parse_published_at(date_el.text().strip() if date_el else "")
            post = Post(
                key=key,
                title=title,
                description=subtitle_el.text().strip() if subtitle_el else "",
                tags=tags,
                thumbnail="",
                publishedAt=published_at,
                url=url,
                source="html",
            )
            seen_keys.add(key)
            new_posts.append(post)

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
    return f"{BASE_URL}/posts/" if page == 1 else f"{BASE_URL}/posts/page{page}/"


def _extract_key_from_url(url: str) -> str:
    path = urlsplit(url.split("?", 1)[0].split("#", 1)[0]).path
    return path.rstrip("/").removeprefix("/").removesuffix(".html").strip()


def _parse_published_at(raw: str) -> str:
    return f"{raw}T00:00:00" if raw else ""
