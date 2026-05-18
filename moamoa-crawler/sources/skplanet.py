from __future__ import annotations

import re
from datetime import datetime
from urllib.parse import urlsplit

from _common import Post, fetch_html, make_payload_raw as make_payload


KEY = "skplanet"
BLOG = "SK planet Tech Topic"
BASE_URL = "https://techtopic.skplanet.com"
DATE_RE = re.compile(r"\d{4}\.\d{2}\.\d{2}")


def crawl(request, config) -> dict[str, object]:
    del config
    doc = fetch_html(BASE_URL)
    items = doc.select("article.post-list-item")
    if not items:
        raise RuntimeError(f"{KEY} crawl finished but no cards were extracted from {BASE_URL}")

    limit = request.size or len(items)
    posts: list[Post] = []
    for item in items[:limit]:
        link_el = item.select_first("h2.title a[href]")
        if link_el is None:
            continue

        url = (link_el.abs_url("href") or link_el.attr("href")).strip()
        if url.startswith("/"):
            url = BASE_URL + url
        title = link_el.text().strip()
        if not url or not title:
            continue

        description_el = item.select_first("p[itemprop=description]")
        date_text = item.select_first("small")
        matched = DATE_RE.search(date_text.text() if date_text else "")
        tags: list[str] = []
        for tag_el in item.select("div.tags a span, div.tags a"):
            tag = tag_el.text().strip()
            if tag and tag not in tags:
                tags.append(tag)

        posts.append(
            Post(
                key=_extract_key(url),
                title=title,
                description=description_el.text().strip() if description_el else "",
                tags=tags,
                thumbnail="",
                publishedAt=datetime.strptime(matched.group(0), "%Y.%m.%d").strftime("%Y-%m-%dT00:00:00") if matched else "",
                url=url,
                source="html",
            )
        )

    if not posts:
        raise RuntimeError(f"{KEY} crawl finished but no posts were extracted from {BASE_URL}")

    return make_payload(
        key=KEY,
        blog=BLOG,
        base_url=BASE_URL,
        requested_url=BASE_URL,
        crawler="html.urllib",
        requested_size=request.size,
        posts=posts,
    )


def _extract_key(url: str) -> str:
    return urlsplit(url).path.rstrip("/").split("/")[-1].strip()
