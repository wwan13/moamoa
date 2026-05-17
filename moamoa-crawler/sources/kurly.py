from __future__ import annotations

import html
import re
import subprocess
import urllib.parse
from datetime import datetime, timezone


KEY = "kurly"
BLOG = "Kurly Tech Blog"
BASE_URL = "https://helloworld.kurly.com"
LIST_URLS = [BASE_URL, f"{BASE_URL}/page/2"]
CARD_RE = re.compile(
    r'<article[^>]*>\s*<a href="(/blog/[^"]+)".*?<img[^>]+src="([^"]+)"[^>]*>.*?<h2[^>]*>(.*?)</h2>.*?<p[^>]*>(.*?)</p>.*?<span[^>]*>(.*?)</span>.*?<time datetime="([^"]+)"',
    flags=re.DOTALL,
)


def crawl(request, config) -> dict[str, object]:
    del config
    posts: list[dict[str, object]] = []
    seen: set[str] = set()
    for list_url in LIST_URLS:
        body = _fetch(list_url)
        for match in CARD_RE.finditer(body):
            url = _absolute(BASE_URL, match.group(1))
            if url in seen:
                continue
            seen.add(url)
            tag = _clean(match.group(5))
            posts.append(
                {
                    "key": _key(url),
                    "title": _clean(match.group(3)),
                    "description": _clean(match.group(4)),
                    "tags": [tag] if tag else [],
                    "thumbnail": _absolute(BASE_URL, match.group(2)),
                    "publishedAt": _published(_clean(match.group(6))),
                    "url": url,
                    "source": "html",
                }
            )
            if request.size and len(posts) >= request.size:
                break
        if request.size and len(posts) >= request.size:
            break
    if not posts:
        raise RuntimeError(f"{KEY} crawl finished but no Kurly post cards were extracted from {LIST_URLS[0]}")
    return _payload(request, LIST_URLS[0], "html.curl", posts)


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


def _clean(value: str) -> str:
    return re.sub(r"\s+", " ", re.sub(r"<[^>]+>", " ", html.unescape(value or ""))).strip()


def _absolute(base_url: str, value: str) -> str:
    return urllib.parse.urljoin(base_url, html.unescape(value or ""))


def _published(value: str) -> str:
    if not value:
        return ""
    try:
        parsed = datetime.fromisoformat(value.replace("Z", "+00:00"))
        if parsed.tzinfo is not None:
            parsed = parsed.astimezone(timezone.utc).replace(tzinfo=None)
        return parsed.replace(microsecond=0).isoformat(timespec="seconds")
    except ValueError:
        return ""


def _key(url: str) -> str:
    return urllib.parse.urlsplit(url).path.strip("/").split("/")[-1]
