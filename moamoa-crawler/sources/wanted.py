from __future__ import annotations

import re

from datetime import datetime

from _common import Post, fetch_html, make_payload_raw as make_payload


KEY = "wanted"
BLOG = "Wanted Tech Blog"
BASE_URL = "https://social.wanted.co.kr/community/team/171"
DATE_RE = re.compile(r"\d{4}\.\d{2}\.\d{2}")


def crawl(request, config) -> dict[str, object]:
    del config
    doc = fetch_html(BASE_URL)
    links = doc.select('a[href^="/community/article/"]')
    if not links:
        raise RuntimeError(f"{KEY} crawl finished but no article links were extracted from {BASE_URL}")

    seen_keys: set[str] = set()
    posts: list[Post] = []
    for link in links:
        url = link.abs_url("href").strip()
        key = url.rstrip("/").split("/")[-1]
        if not url or not key or key in seen_keys:
            continue
        posts.append(_fetch_detail(key, url))
        seen_keys.add(key)
        if request.size and len(posts) >= request.size:
            break

    return make_payload(
        key=KEY,
        blog=BLOG,
        base_url=BASE_URL,
        requested_url=BASE_URL,
        crawler="html.urllib",
        requested_size=request.size,
        posts=posts,
    )


def _fetch_detail(key: str, url: str) -> Post:
    doc = fetch_html(url)
    title = (
        (doc.select_first('meta[property="og:title"]').attr("content") if doc.select_first('meta[property="og:title"]') else "")
        or (doc.select_first('meta[name="twitter:title"]').attr("content") if doc.select_first('meta[name="twitter:title"]') else "")
        or (doc.select_first("h1").text() if doc.select_first("h1") else "")
        or (doc.select_first("h2").text() if doc.select_first("h2") else "")
    )
    description = (
        (doc.select_first('meta[property="og:description"]').attr("content") if doc.select_first('meta[property="og:description"]') else "")
        or (doc.select_first('meta[name="description"]').attr("content") if doc.select_first('meta[name="description"]') else "")
    )
    if not description:
        for candidate in doc.select("article p, section p, p"):
            if candidate.text():
                description = candidate.text()
                break

    thumbnail = (
        (doc.select_first('meta[property="og:image"]').attr("content") if doc.select_first('meta[property="og:image"]') else "")
        or (doc.select_first('meta[property="og:image:secure_url"]').attr("content") if doc.select_first('meta[property="og:image:secure_url"]') else "")
        or (doc.select_first('meta[name="twitter:image"]').attr("content") if doc.select_first('meta[name="twitter:image"]') else "")
        or (doc.select_first("article img[src]").abs_url("src") if doc.select_first("article img[src]") else "")
        or (doc.select_first("img[src]").abs_url("src") if doc.select_first("img[src]") else "")
    )
    published = (
        (doc.select_first('meta[property="article:published_time"], meta[name="article:published_time"]').attr("content") if doc.select_first('meta[property="article:published_time"], meta[name="article:published_time"]') else "")
        or (doc.select_first("time[datetime]").attr("datetime") if doc.select_first("time[datetime]") else "")
        or (doc.select_first("time").text() if doc.select_first("time") else "")
    )
    if not published:
        matched = DATE_RE.search(doc.text())
        if matched:
            published = matched.group(0)

    return Post(
        key=key,
        title=title,
        description=description,
        tags=[],
        thumbnail=thumbnail,
        publishedAt=_parse_published_at(published),
        url=url,
        source="html",
    )


def _parse_published_at(raw: str) -> str:
    text = raw.strip().replace("Z", "+00:00")
    if not text:
        return ""
    try:
        return datetime.fromisoformat(text).replace(microsecond=0).isoformat(timespec="seconds")
    except ValueError:
        pass
    for pattern in ("%Y.%m.%d", "%Y. %m. %d"):
        try:
            return datetime.strptime(text, pattern).strftime("%Y-%m-%dT00:00:00")
        except ValueError:
            continue
    return ""
