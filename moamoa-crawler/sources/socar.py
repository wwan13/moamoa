from __future__ import annotations

import html
import re
import urllib.parse
import urllib.request
from datetime import datetime, timezone


KEY = "socar"
BLOG = "Socar Tech Blog"
BASE_URL = "https://tech.socarcorp.kr"
LIST_URLS = [f"{BASE_URL}/posts/", f"{BASE_URL}/posts/page2/"]
USER_AGENT = "Mozilla/5.0"
CARD_RE = re.compile(
    r'<article class="post-preview">.*?<a href="([^"]+)">.*?<h2 class="post-title">(.*?)</h2>.*?(?:<h3 class="post-subtitle">(.*?)</h3>)?.*?<span class="date">([^<]+)</span>.*?<span class="category"><a href="[^"]+">([^<]+)</a></span>.*?(?:<span class="tag"><a href="[^"]+">([^<]+)</a></span>)?',
    flags=re.DOTALL,
)


def crawl(request, config) -> dict[str, object]:
    del config
    posts: list[dict[str, object]] = []
    seen: set[str] = set()
    for list_url in LIST_URLS:
        body = _fetch(list_url)
        for href, title, subtitle, published, category, tag in CARD_RE.findall(body):
            url = _absolute(BASE_URL, href)
            if url in seen:
                continue
            seen.add(url)
            detail = _fetch(url)
            tags = [_clean(category)]
            if _clean(tag):
                tags.append(_clean(tag))
            posts.append(
                {
                    "key": _key(url),
                    "title": _clean(title),
                    "description": _clean(_meta(detail, "og:description") or subtitle),
                    "tags": tags,
                    "thumbnail": _absolute(url, _meta(detail, "og:image")),
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
        raise RuntimeError(f"{KEY} crawl finished but no Socar post cards were extracted from {LIST_URLS[0]}")
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
    match = re.search(rf'<meta[^>]+property=["\']{re.escape(name)}["\'][^>]+content=["\']([^"\']+)', body, flags=re.IGNORECASE)
    return html.unescape(match.group(1)).strip() if match else ""


def _clean(value: str) -> str:
    return re.sub(r"\s+", " ", re.sub(r"<[^>]+>", " ", html.unescape(value or ""))).strip()


def _absolute(base_url: str, value: str) -> str:
    return urllib.parse.urljoin(base_url, html.unescape(value or ""))


def _published(value: str) -> str:
    return datetime.fromisoformat(_clean(value)).replace(microsecond=0).isoformat(timespec="seconds") if _clean(value) else ""


def _key(url: str) -> str:
    return urllib.parse.urlsplit(url).path.strip("/").removesuffix(".html").split("/")[-1]
