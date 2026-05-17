from __future__ import annotations

import html
import re
import urllib.parse
import urllib.request
from datetime import datetime, timezone
from email.utils import parsedate_to_datetime


KEY = "saramin"
BLOG = "Saramin Tech Blog"
BASE_URL = "https://saramin.github.io"
LIST_URLS = [f"{BASE_URL}/", f"{BASE_URL}/page2/"]
USER_AGENT = "Mozilla/5.0"
LINK_RE = re.compile(r'href="(/20\d{2}-\d{2}-\d{2}-[^"]+/?)"')


def crawl(request, config) -> dict[str, object]:
    del config
    posts: list[dict[str, object]] = []
    seen: set[str] = set()
    for list_url in LIST_URLS:
        body = _fetch(list_url)
        for href in LINK_RE.findall(body):
            url = _absolute(BASE_URL, href)
            if url in seen:
                continue
            seen.add(url)
            posts.append(_detail_post(url))
            if request.size and len(posts) >= request.size:
                break
        if request.size and len(posts) >= request.size:
            break
    if not posts:
        raise RuntimeError(f"{KEY} crawl finished but no post links were extracted from {LIST_URLS[0]}")
    return _payload(request, LIST_URLS[0], "html.urllib", posts)


def _detail_post(url: str) -> dict[str, object]:
    body = _fetch(url)
    return {
        "key": _key(url),
        "title": _clean(_meta(body, "og:title") or _text(body, "title")),
        "description": _clean(_meta(body, "og:description") or _meta(body, "description")),
        "tags": [],
        "thumbnail": _absolute(url, _meta(body, "og:image") or "/assets/img/favicons/apple-touch-icon.png"),
        "publishedAt": _published(_meta(body, "article:published_time") or _date_from_url(url)),
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
        rf'<meta[^>]+content=["\']([^"\']+)["\'][^>]+property=["\']{re.escape(name)}["\']',
        rf'<meta[^>]+content=["\']([^"\']+)["\'][^>]+name=["\']{re.escape(name)}["\']',
    ]
    for pattern in patterns:
        match = re.search(pattern, body, flags=re.IGNORECASE | re.DOTALL)
        if match:
            return html.unescape(match.group(1)).strip()
    return ""


def _text(body: str, tag: str) -> str:
    match = re.search(rf"<{tag}[^>]*>(.*?)</{tag}>", body, flags=re.IGNORECASE | re.DOTALL)
    return _clean(match.group(1)) if match else ""


def _clean(value: str) -> str:
    return re.sub(r"\s+", " ", re.sub(r"<[^>]+>", " ", html.unescape(value or ""))).strip()


def _absolute(base_url: str, value: str) -> str:
    if not value:
        return ""
    return urllib.parse.urljoin(base_url, html.unescape(value))


def _key(url: str) -> str:
    return urllib.parse.urlsplit(url).path.strip("/").split("/")[-1]


def _date_from_url(url: str) -> str:
    match = re.search(r"/(20\d{2})-(\d{2})-(\d{2})-", url)
    return f"{match.group(1)}-{match.group(2)}-{match.group(3)}" if match else ""


def _published(value: str) -> str:
    text = _clean(value)
    if not text:
        return ""
    text = text.replace("Z", "+00:00")
    for candidate in (text, text.rstrip(".").replace(".", "-")):
        try:
            parsed = datetime.fromisoformat(candidate)
            if parsed.tzinfo is not None:
                parsed = parsed.astimezone(timezone.utc).replace(tzinfo=None)
            return parsed.replace(microsecond=0).isoformat(timespec="seconds")
        except ValueError:
            pass
    try:
        return parsedate_to_datetime(text).astimezone(timezone.utc).replace(tzinfo=None, microsecond=0).isoformat(timespec="seconds")
    except (TypeError, ValueError):
        return ""
