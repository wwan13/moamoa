from __future__ import annotations

from datetime import datetime

from _common import Post, fetch_html, make_payload_raw as make_payload


KEY = "samsung"
BLOG = "Samsung Tech Blog"
BASE_URL = "https://techblog.samsung.com"


def crawl(request, config) -> dict[str, object]:
    del config
    posts: list[Post] = []
    seen_keys: set[str] = set()
    page = 1

    while request.size is None or len(posts) < request.size:
        doc = fetch_html(_build_list_url(page))
        items = doc.select("ul.blog-list > li")
        if not items:
            break

        new_posts = []
        for item in items:
            link_el = item.select_first('a[href^="/blog/article/"]')
            title_el = item.select_first("h3")
            date_el = item.select_first("span.date")
            thumb_el = item.select_first("img")
            if link_el is None or title_el is None or date_el is None or thumb_el is None:
                continue

            url = link_el.abs_url("href")
            key = url.rstrip("/").split("/")[-1]
            if not url or not key or key in seen_keys:
                continue

            detail = fetch_html(url)
            description_el = detail.select_first("article.txt-group p strong")
            tags = []
            for tag_el in detail.select('p.tag-list a[href*="tagName="]'):
                tag = tag_el.text()
                if tag and tag not in tags:
                    tags.append(tag)

            new_posts.append(
                Post(
                    key=key,
                    title=title_el.text(),
                    description=description_el.text() if description_el else "",
                    tags=tags,
                    thumbnail=thumb_el.abs_url("src"),
                    publishedAt=datetime.strptime(date_el.text(), "%B %d, %Y").strftime("%Y-%m-%dT00:00:00"),
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
        raise RuntimeError(f"{KEY} crawl finished but no blog cards were extracted from {_build_list_url(1)}")

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
    return f"{BASE_URL}/?page={page}&"
