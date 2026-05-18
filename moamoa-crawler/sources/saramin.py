from __future__ import annotations

import re
from datetime import datetime
from urllib.error import HTTPError
from urllib.parse import urlsplit

from _common import Post, fetch_html, make_payload_raw as make_payload


KEY = "saramin"
BLOG = "Saramin Tech Blog"
BASE_URL = "https://saramin.github.io"
POST_URL_RE = re.compile(r"https://saramin\.github\.io/\d{4}-\d{2}-\d{2}-[\w\-]+/?")
URL_DATE_RE = re.compile(r"/(\d{4})-(\d{2})-(\d{2})-")
URL_DATE_SLASH_RE = re.compile(r"/(\d{4})/(\d{2})/(\d{2})/")


def crawl(request, config) -> dict[str, object]:
    del config
    page = 1
    last_signature: str | None = None
    seen_urls: set[str] = set()
    posts: list[Post] = []

    while request.size is None or len(posts) < request.size:
        try:
            doc = fetch_html(_build_list_url(page))
        except HTTPError as error:
            if error.code == 404 and page > 1:
                break
            raise
        post_urls = _extract_post_urls(doc)
        if not post_urls:
            break

        signature = "|".join(post_urls)
        if not signature or signature == last_signature:
            break
        last_signature = signature

        new_urls = [url for url in post_urls if url not in seen_urls]
        if not new_urls:
            break

        for url in new_urls:
            posts.append(_fetch_post(url))
            seen_urls.add(url)
            if request.size and len(posts) >= request.size:
                break
        if request.size and len(posts) >= request.size:
            break
        page += 1

    if not posts:
        raise RuntimeError(f"{KEY} crawl finished but no post links were extracted from {_build_list_url(1)}")

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
    return f"{BASE_URL}/" if page == 1 else f"{BASE_URL}/page{page}/"


def _extract_post_urls(doc) -> list[str]:
    urls = []
    for anchor in doc.select("a[href]"):
        url = anchor.abs_url("href").strip()
        if url and POST_URL_RE.match(url) and url not in urls:
            urls.append(url)
    return urls


def _fetch_post(url: str) -> Post:
    doc = fetch_html(url)
    title = (
        (doc.select_first('meta[property="og:title"]').attr("content") if doc.select_first('meta[property="og:title"]') else "")
        or (doc.select_first('meta[name="twitter:title"]').attr("content") if doc.select_first('meta[name="twitter:title"]') else "")
        or (doc.select_first("h1").text() if doc.select_first("h1") else "")
    ).strip()
    description = (
        (doc.select_first('meta[property="og:description"]').attr("content") if doc.select_first('meta[property="og:description"]') else "")
        or (doc.select_first('meta[name="description"]').attr("content") if doc.select_first('meta[name="description"]') else "")
    ).strip()
    if not description:
        for candidate in doc.select("article p, main p, section p, p"):
            text = candidate.text().strip()
            if text:
                description = text
                break

    thumbnail = (
        (doc.select_first('meta[property="og:image"]').attr("content") if doc.select_first('meta[property="og:image"]') else "")
        or (doc.select_first('meta[name="twitter:image"]').attr("content") if doc.select_first('meta[name="twitter:image"]') else "")
        or (doc.select_first("article img[src], main img[src], section img[src]").abs_url("src") if doc.select_first("article img[src], main img[src], section img[src]") else "")
    ).strip()

    published = (
        (doc.select_first('meta[property="article:published_time"]').attr("content") if doc.select_first('meta[property="article:published_time"]') else "")
        or (doc.select_first("time[datetime]").attr("datetime") if doc.select_first("time[datetime]") else "")
        or _parse_date_from_url(url)
    ).strip()

    return Post(
        key=_extract_key(url),
        title=title,
        description=description,
        tags=[],
        thumbnail=_resolve_url(thumbnail),
        publishedAt=_parse_published_at(published),
        url=url,
        source="html",
    )


def _extract_key(url: str) -> str:
    return urlsplit(url).path.rstrip("/").split("/")[-1].strip()


def _parse_date_from_url(url: str) -> str:
    matched = URL_DATE_RE.search(url) or URL_DATE_SLASH_RE.search(url)
    if not matched:
        return ""
    year, month, day = matched.groups()
    return f"{year}-{month}-{day}T00:00:00"


def _parse_published_at(raw: str) -> str:
    text = raw.strip().replace("Z", "+00:00")
    if not text:
        return ""
    for parser in (
        lambda value: datetime.fromisoformat(value).replace(microsecond=0).isoformat(timespec="seconds"),
        lambda value: datetime.strptime(value, "%Y-%m-%dT%H:%M:%S").strftime("%Y-%m-%dT%H:%M:%S"),
        lambda value: datetime.strptime(value, "%Y-%m-%d").strftime("%Y-%m-%dT00:00:00"),
    ):
        try:
            return parser(text)
        except ValueError:
            continue
    return ""


def _resolve_url(candidate: str) -> str:
    if not candidate:
        return ""
    if candidate.startswith("http://") or candidate.startswith("https://"):
        return candidate
    return f"{BASE_URL}/{candidate.lstrip('/')}"
