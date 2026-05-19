from __future__ import annotations

import html
import json
import re
import urllib.parse
from dataclasses import dataclass
from datetime import datetime, timezone

from _common import fetch_text


KEY = "woowabros"
BLOG = "우아한형제들 기술블로그"
BASE_URL = "https://techblog.woowahan.com/"
AJAX_URL = urllib.parse.urljoin(BASE_URL, "wp-admin/admin-ajax.php")
CAT_TAG_RE = re.compile(r'<a class="cat-tag"[^>]*>(.*?)</a>', flags=re.DOTALL)


@dataclass(frozen=True)
class Summary:
    key: str
    title: str
    description: str
    published_at: str
    url: str


@dataclass(frozen=True)
class PageResult:
    posts: list[Summary]
    max_page: int


def crawl(request, config) -> dict[str, object]:
    del config

    summaries = _fetch_summaries(request.size)
    if not summaries:
        raise RuntimeError(f"{KEY} crawl finished but no AJAX posts were extracted from {BASE_URL}")

    posts: list[dict[str, object]] = []
    for summary in summaries:
        detail = _fetch_text(summary.url)
        posts.append(
            {
                "key": summary.key,
                "title": summary.title,
                "description": summary.description,
                "tags": _categories(detail),
                "thumbnail": _absolute(summary.url, _meta(detail, "og:image")),
                "publishedAt": summary.published_at,
                "url": summary.url,
                "source": "ajax",
            }
        )

    return {
        "key": request.key,
        "blog": BLOG,
        "baseUrl": BASE_URL.rstrip("/"),
        "requestedUrl": BASE_URL,
        "crawler": "ajax.curl",
        "crawledAt": datetime.now(timezone.utc).isoformat(),
        "requestedSize": request.size,
        "postCount": len(posts),
        "posts": posts,
    }


def _fetch_summaries(size: int | None) -> list[Summary]:
    posts: list[Summary] = []
    page_no = 1
    max_page = 2**31 - 1
    first_signature: str | None = None

    while (size is None or len(posts) < size) and page_no <= max_page:
        page = _parse_page(_fetch_ajax_page(page_no))
        if not page.posts:
            break

        max_page = page.max_page if page.max_page > 0 else max_page
        signature = "|".join(post.url for post in page.posts)
        if page_no == 1:
            first_signature = signature
        elif first_signature == signature:
            break

        remaining = len(page.posts) if size is None else max(size - len(posts), 0)
        posts.extend(page.posts[:remaining])
        page_no += 1

    return posts


def _parse_page(body: str) -> PageResult:
    root = json.loads(body)
    if not root.get("success"):
        raise RuntimeError("woowabros AJAX response was not successful")

    data = root.get("data") or {}
    raw_posts = data.get("posts") or []
    posts_by_url: dict[str, Summary] = {}
    for item in raw_posts:
        summary = _parse_summary(item)
        if summary is None or summary.url in posts_by_url:
            continue
        posts_by_url[summary.url] = summary

    pagination = data.get("pagination") or {}
    max_page = pagination.get("max") or 0
    try:
        parsed_max_page = int(max_page)
    except (TypeError, ValueError):
        parsed_max_page = 0

    return PageResult(posts=list(posts_by_url.values()), max_page=parsed_max_page)


def _parse_summary(item: dict[str, object]) -> Summary | None:
    url = _clean(item.get("permalink"))
    title = _clean(item.get("post_title"))
    if not url or not title:
        return None

    return Summary(
        key=_key(url),
        title=title,
        description=_clean(item.get("excerpt")),
        published_at=_published(_clean(item.get("date"))),
        url=url,
    )


def _fetch_ajax_page(page_no: int) -> str:
    return _fetch_text(
        AJAX_URL,
        data=urllib.parse.urlencode(
            {
                "action": "get_posts_data",
                "data[post][post_status]": "publish",
                "data[post][paged]": str(page_no),
                "data[meta]": "main",
            }
        ).encode("utf-8"),
        headers={
            "X-Requested-With": "XMLHttpRequest",
            "Content-Type": "application/x-www-form-urlencoded; charset=UTF-8",
            "Accept": "application/json, text/javascript, */*; q=0.01",
            "Referer": BASE_URL,
        },
    )


def _fetch_text(url: str, *, data: bytes | None = None, headers: dict[str, str] | None = None) -> str:
    method = "POST" if data is not None else "GET"
    return fetch_text(url, method=method, data=data, headers=headers)


def _meta(body: str, name: str) -> str:
    match = re.search(rf'<meta[^>]+property=["\']{re.escape(name)}["\'][^>]+content=["\']([^"\']+)', body, flags=re.IGNORECASE)
    return html.unescape(match.group(1)).strip() if match else ""


def _categories(body: str) -> list[str]:
    tags: list[str] = []
    seen: set[str] = set()
    for raw in CAT_TAG_RE.findall(body):
        value = _clean(raw)
        if not value or value in seen:
            continue
        seen.add(value)
        tags.append(value)
    return tags


def _clean(value: object) -> str:
    return re.sub(r"\s+", " ", re.sub(r"<[^>]+>", " ", html.unescape(str(value or "")))).strip()


def _absolute(base_url: str, value: str) -> str:
    return urllib.parse.urljoin(base_url, html.unescape(value or ""))


def _published(value: str) -> str:
    text = value.replace(". ", ".")
    for pattern in (
        "%Y.%m.%d.",
        "%Y.%m.%d",
        "%b.%d.%Y",
        "%b. %d. %Y",
        "%b.%d.%Y.",
        "%b. %d. %Y.",
    ):
        try:
            return datetime.strptime(text, pattern).replace(microsecond=0).isoformat(timespec="seconds")
        except ValueError:
            continue
    return ""


def _key(url: str) -> str:
    return urllib.parse.urlsplit(url).path.strip("/").split("/")[-1]
