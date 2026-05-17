from __future__ import annotations

import html
import re
import urllib.parse
import urllib.request
from datetime import datetime, timezone


KEY = "skplanet"
BLOG = "SK planet Tech Topic"
BASE_URL = "https://techtopic.skplanet.com"
LIST_URLS = [BASE_URL]
USER_AGENT = "Mozilla/5.0"
CARD_RE = re.compile(
    r'<article class="post-list-item".*?<div class="tags">(.*?)</div>.*?<a itemProp="url" href="([^"]+)"><span itemProp="headline">(.*?)</span></a>.*?<p itemProp="description">(.*?)</p>.*?<small>(20\d{2}\.\d{2}\.\d{2})',
    flags=re.DOTALL,
)


def crawl(request, config) -> dict[str, object]:
    del config
    body = _fetch(LIST_URLS[0])
    posts: list[dict[str, object]] = []
    for tags_html, href, title, description, published in CARD_RE.findall(body):
        url = _absolute(BASE_URL, href)
        detail = _fetch(url)
        posts.append(
            {
                "key": _key(url),
                "title": _clean(title),
                "description": _clean(_meta(detail, "og:description") or description),
                "tags": [_clean(tag) for tag in re.findall(r"<span[^>]*>(.*?)</span>", tags_html) if _clean(tag)],
                "thumbnail": _absolute(url, _meta(detail, "og:image")),
                "publishedAt": _published(_meta(detail, "article:published_time") or published),
                "url": url,
                "source": "html",
            }
        )
        if request.size and len(posts) >= request.size:
            break
    if not posts:
        raise RuntimeError(f"{KEY} crawl finished but no SK planet cards were extracted from {LIST_URLS[0]}")
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
    text = _clean(value).replace(".", "-")
    if not text:
        return ""
    parsed = datetime.fromisoformat(text.replace("Z", "+00:00"))
    if parsed.tzinfo is not None:
        parsed = parsed.astimezone(timezone.utc).replace(tzinfo=None)
    return parsed.replace(microsecond=0).isoformat(timespec="seconds")


def _key(url: str) -> str:
    return urllib.parse.urlsplit(url).path.strip("/").split("/")[-1]
