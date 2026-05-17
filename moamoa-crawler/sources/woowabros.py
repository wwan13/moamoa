from __future__ import annotations

import html
import re
import subprocess
import urllib.parse
from datetime import datetime, timezone


KEY = "woowabros"
BLOG = "우아한형제들 기술블로그"
BASE_URL = "https://techblog.woowahan.com"
LIST_URL = f"{BASE_URL}/"
CARD_RE = re.compile(
    r'<a href="(https://techblog\.woowahan\.com/\d+/)">.*?<time class="post-author-date">\s*(.*?)\s*</time>.*?<h2 class="post-title">(.*?)</h2>.*?<p class="post-excerpt">(.*?)</p>',
    flags=re.DOTALL,
)


def crawl(request, config) -> dict[str, object]:
    del config
    body = _fetch(LIST_URL)
    posts: list[dict[str, object]] = []
    for url, published, title, description in CARD_RE.findall(body):
        detail = _fetch(url)
        posts.append(
            {
                "key": _key(url),
                "title": _clean(title),
                "description": _clean(description),
                "tags": [],
                "thumbnail": _absolute(url, _meta(detail, "og:image")),
                "publishedAt": _published(published),
                "url": url,
                "source": "html",
            }
        )
        if request.size and len(posts) >= request.size:
            break
    if not posts:
        raise RuntimeError(f"{KEY} crawl finished but no Woowabros post cards were extracted from {LIST_URL}")
    return _payload(request, LIST_URL, "html.curl", posts)


def _fetch(url: str) -> str:
    result = subprocess.run(["curl", "-L", "-A", "Mozilla/5.0", url], check=True, capture_output=True, text=True)
    return result.stdout


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
    match = re.search(rf'<meta[^>]+property=["\']{re.escape(name)}["\'][^>]+content=["\']([^"\']+)', body, flags=re.IGNORECASE)
    return html.unescape(match.group(1)).strip() if match else ""


def _clean(value: str) -> str:
    return re.sub(r"\s+", " ", re.sub(r"<[^>]+>", " ", html.unescape(value or ""))).strip()


def _absolute(base_url: str, value: str) -> str:
    return urllib.parse.urljoin(base_url, html.unescape(value or ""))


def _published(value: str) -> str:
    text = _clean(value)
    for pattern in ("%b.%d.%Y", "%B.%d.%Y"):
        try:
            return datetime.strptime(text, pattern).replace(microsecond=0).isoformat(timespec="seconds")
        except ValueError:
            continue
    return ""


def _key(url: str) -> str:
    return urllib.parse.urlsplit(url).path.strip("/").split("/")[-1]
