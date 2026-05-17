from __future__ import annotations

import html
import re
import urllib.parse
import urllib.request
from datetime import datetime, timezone


KEY = "samosam"
BLOG = "3o3 Tech Blog"
BASE_URL = "https://blog.3o3.co.kr"
LIST_URLS = [f"{BASE_URL}/tag/tech/", f"{BASE_URL}/tag/tech/page/2/"]
USER_AGENT = "Mozilla/5.0"
CARD_RE = re.compile(
    r'<article class="post-card.*?<a class="post-card-image-link" href="([^"]+)".*?<img class="post-card-image"[^>]+src="([^"]+)".*?<span class="post-card-primary-tag">([^<]*)</span>.*?<h2 class="post-card-title">\s*(.*?)\s*</h2>.*?<div class="post-card-excerpt">(.*?)</div>.*?<time class="post-card-meta-date" datetime="([^"]+)"',
    flags=re.DOTALL,
)


def crawl(request, config) -> dict[str, object]:
    del config
    posts: list[dict[str, object]] = []
    seen: set[str] = set()
    for list_url in LIST_URLS:
        body = _fetch(list_url)
        for href, image, tag, title, description, published in CARD_RE.findall(body):
            url = _absolute(BASE_URL, href)
            if url in seen:
                continue
            seen.add(url)
            clean_tag = _clean(tag)
            posts.append(
                {
                    "key": _key(url),
                    "title": _clean(title),
                    "description": _clean(description),
                    "tags": [clean_tag] if clean_tag else [],
                    "thumbnail": _absolute(BASE_URL, image),
                    "publishedAt": _published(published),
                    "url": url,
                    "source": "html",
                }
            )
            if request.size and len(posts) >= request.size:
                break
        if request.size and len(posts) >= request.size:
            break
    if not posts:
        raise RuntimeError(f"{KEY} crawl finished but no 3o3 tech cards were extracted from {LIST_URLS[0]}")
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


def _clean(value: str) -> str:
    return re.sub(r"\s+", " ", re.sub(r"<[^>]+>", " ", html.unescape(value or ""))).strip()


def _absolute(base_url: str, value: str) -> str:
    return urllib.parse.urljoin(base_url, html.unescape(value or ""))


def _published(value: str) -> str:
    text = _clean(value).replace(".", "-")
    return datetime.fromisoformat(text).replace(microsecond=0).isoformat(timespec="seconds") if text else ""


def _key(url: str) -> str:
    return urllib.parse.urlsplit(url).path.strip("/").split("/")[-1]
