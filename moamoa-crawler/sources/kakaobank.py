from __future__ import annotations

import html
import re
import urllib.parse
import urllib.request
from datetime import datetime, timezone


KEY = "kakaobank"
BLOG = "KakaoBank Tech Blog"
BASE_URL = "https://tech.kakaobank.com"
LIST_URLS = [BASE_URL, f"{BASE_URL}/page/2/"]
USER_AGENT = "Mozilla/5.0"
CARD_RE = re.compile(r'<div class="date">\s*(20\d{2}-\d{2}-\d{2})\s*</div>.*?<h2 class="post-item post-title">\s*<a href="([^"]+)">(.*?)</a>.*?<div class="post-item post-summary markdown-body">(.*?)</div>', flags=re.DOTALL)


def crawl(request, config) -> dict[str, object]:
    del config
    posts: list[dict[str, object]] = []
    seen: set[str] = set()
    for list_url in LIST_URLS:
        body = _fetch(list_url)
        for published, href, title, description in CARD_RE.findall(body):
            url = _absolute(BASE_URL, href)
            if url in seen:
                continue
            seen.add(url)
            detail = _fetch(url)
            tags = re.findall(r'<a href="/tags/[^"]+/">#?([^<]+)</a>', detail)
            posts.append(
                {
                    "key": _key(url),
                    "title": _clean(title),
                    "description": _clean(description),
                    "tags": [_clean(tag) for tag in tags if _clean(tag)],
                    "thumbnail": _absolute(url, _meta(detail, "og:image")),
                    "publishedAt": _published(_meta(detail, "article:published_time") or published),
                    "url": url,
                    "source": "html",
                }
            )
            if request.size and len(posts) >= request.size:
                break
        if request.size and len(posts) >= request.size:
            break
    if not posts:
        raise RuntimeError(f"{KEY} crawl finished but no KakaoBank cards were extracted from {LIST_URLS[0]}")
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
    text = _clean(value).replace("Z", "+00:00")
    if not text:
        return ""
    if "T" not in text and re.fullmatch(r"20\d{2}-\d{2}-\d{2}", text):
        return f"{text}T00:00:00"
    parsed = datetime.fromisoformat(text)
    if parsed.tzinfo is not None:
        parsed = parsed.astimezone(timezone.utc).replace(tzinfo=None)
    return parsed.replace(microsecond=0).isoformat(timespec="seconds")


def _key(url: str) -> str:
    return urllib.parse.urlsplit(url).path.strip("/").split("/")[-1]
