from __future__ import annotations

import html
import re
import urllib.parse
import urllib.request
from datetime import datetime, timezone


KEY = "wanted"
BLOG = "Wanted Tech Blog"
BASE_URL = "https://social.wanted.co.kr"
LIST_URLS = [f"{BASE_URL}/community/team/171"]
USER_AGENT = "Mozilla/5.0"
LINK_RE = re.compile(r'href="(/community/article/\d+)"')


def crawl(request, config) -> dict[str, object]:
    del config
    body = _fetch(LIST_URLS[0])
    posts: list[dict[str, object]] = []
    seen: set[str] = set()
    for href in LINK_RE.findall(body):
        url = _absolute(BASE_URL, href)
        if url in seen:
            continue
        seen.add(url)
        posts.append(_detail_post(url))
        if request.size and len(posts) >= request.size:
            break
    if not posts:
        raise RuntimeError(f"{KEY} crawl finished but no Wanted article links were extracted from {LIST_URLS[0]}")
    return _payload(request, LIST_URLS[0], "html.urllib", posts)


def _detail_post(url: str) -> dict[str, object]:
    body = _fetch(url)
    published = _first(body, r'\\"createdAt\\":\\"([^\\"]+)')
    return {
        "key": _key(url),
        "title": _clean(_meta(body, "og:title") or _text(body, "title")),
        "description": _clean(_meta(body, "og:description") or _meta(body, "description")),
        "tags": [],
        "thumbnail": _absolute(url, _meta(body, "og:image")),
        "publishedAt": _published(published),
        "url": url,
        "source": "html",
    }


def _fetch(url: str) -> str:
    req = urllib.request.Request(url, headers={"User-Agent": USER_AGENT, "Accept": "text/html,*/*;q=0.8"})
    with urllib.request.urlopen(req, timeout=20) as response:
        charset = response.headers.get_content_charset() or "utf-8"
        return response.read().decode(charset, errors="replace")


def _payload(request, requested_url: str, crawler: str, posts: list[dict[str, object]]) -> dict[str, object]:
    return {
        "key": request.key,
        "blog": BLOG,
        "baseUrl": BASE_URL,
        "requestedUrl": requested_url,
        "crawler": crawler,
        "crawledAt": datetime.now(timezone.utc).isoformat(),
        "requestedSize": request.size,
        "postCount": len(posts),
        "posts": posts,
    }


def _meta(body: str, name: str) -> str:
    patterns = [
        rf'<meta[^>]+property=["\']{re.escape(name)}["\'][^>]+content=["\']([^"\']+)',
        rf'<meta[^>]+name=["\']{re.escape(name)}["\'][^>]+content=["\']([^"\']+)',
    ]
    for pattern in patterns:
        match = re.search(pattern, body, flags=re.IGNORECASE | re.DOTALL)
        if match:
            return html.unescape(match.group(1)).strip()
    return ""


def _text(body: str, tag: str) -> str:
    match = re.search(rf"<{tag}[^>]*>(.*?)</{tag}>", body, flags=re.IGNORECASE | re.DOTALL)
    return match.group(1) if match else ""


def _first(body: str, pattern: str) -> str:
    match = re.search(pattern, body, flags=re.IGNORECASE | re.DOTALL)
    return match.group(1) if match else ""


def _clean(value: str) -> str:
    return re.sub(r"\s+", " ", re.sub(r"<[^>]+>", " ", html.unescape(value or ""))).strip()


def _absolute(base_url: str, value: str) -> str:
    return urllib.parse.urljoin(base_url, html.unescape(value or ""))


def _published(value: str) -> str:
    text = _clean(value).replace("Z", "+00:00")
    if not text:
        return ""
    parsed = datetime.fromisoformat(text)
    if parsed.tzinfo is not None:
        parsed = parsed.astimezone(timezone.utc).replace(tzinfo=None)
    return parsed.replace(microsecond=0).isoformat(timespec="seconds")


def _key(url: str) -> str:
    return urllib.parse.urlsplit(url).path.strip("/").split("/")[-1]
