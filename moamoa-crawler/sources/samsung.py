from __future__ import annotations

import html
import re
import urllib.parse
import urllib.request
from datetime import datetime, timezone


KEY = "samsung"
BLOG = "Samsung Tech Blog"
BASE_URL = "https://techblog.samsung.com"
LIST_URLS = [f"{BASE_URL}/?page=1&"]
USER_AGENT = "Mozilla/5.0"
CARD_RE = re.compile(r'<a href="(/blog/article/\d+)">.*?<img src="([^"]+)".*?<h3>(.*?)</h3>.*?<span class="date">\s*(.*?)\s*</span>', flags=re.DOTALL)


def crawl(request, config) -> dict[str, object]:
    del config
    body = _fetch(LIST_URLS[0])
    posts: list[dict[str, object]] = []
    for href, image, title, published in CARD_RE.findall(body):
        url = _absolute(BASE_URL, href)
        detail = _fetch(url)
        posts.append(
            {
                "key": _key(url),
                "title": _clean(title),
                "description": _clean(_meta(detail, "og:description") or _meta(detail, "description")),
                "tags": [],
                "thumbnail": _absolute(BASE_URL, image),
                "publishedAt": _published(_meta(detail, "article:published_time") or published),
                "url": url,
                "source": "html",
            }
        )
        if request.size and len(posts) >= request.size:
            break
    if not posts:
        raise RuntimeError(f"{KEY} crawl finished but no Samsung blog cards were extracted from {LIST_URLS[0]}")
    return _payload(request, LIST_URLS[0], "html.urllib", posts)


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


def _clean(value: str) -> str:
    return re.sub(r"\s+", " ", re.sub(r"<[^>]+>", " ", html.unescape(value or ""))).strip()


def _absolute(base_url: str, value: str) -> str:
    return urllib.parse.urljoin(base_url, html.unescape(value or ""))


def _published(value: str) -> str:
    text = _clean(value)
    if not text:
        return ""
    for pattern in ("%b %d, %Y", "%B %d, %Y"):
        try:
            return datetime.strptime(text, pattern).replace(microsecond=0).isoformat(timespec="seconds")
        except ValueError:
            continue
    return ""


def _key(url: str) -> str:
    return urllib.parse.urlsplit(url).path.strip("/").split("/")[-1]
