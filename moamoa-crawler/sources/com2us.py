from __future__ import annotations

import html
import re
import urllib.parse
import urllib.request
from datetime import datetime, timezone


KEY = "com2us"
BLOG = "Com2uS Tech Blog"
BASE_URL = "https://on.com2us.com"
LIST_URLS = [
    f"{BASE_URL}/tag/%EA%B8%B0%EC%88%A0%EB%B8%94%EB%A1%9C%EA%B7%B8/",
    f"{BASE_URL}/tag/%EA%B8%B0%EC%88%A0%EB%B8%94%EB%A1%9C%EA%B7%B8/page/2/",
]
USER_AGENT = "Mozilla/5.0"
LINK_RE = re.compile(r'<a class="loop-grid loop" href="(https://on\.com2us\.com/tech/[^"]+/)"')


def crawl(request, config) -> dict[str, object]:
    del config
    posts: list[dict[str, object]] = []
    seen: set[str] = set()
    for list_url in LIST_URLS:
        body = _fetch(list_url)
        for url in LINK_RE.findall(body):
            if url in seen:
                continue
            seen.add(url)
            posts.append(_detail_post(url))
            if request.size and len(posts) >= request.size:
                break
        if request.size and len(posts) >= request.size:
            break
    if not posts:
        raise RuntimeError(f"{KEY} crawl finished but no Com2uS tech cards were extracted from {LIST_URLS[0]}")
    return _payload(request, LIST_URLS[0], "html.urllib", posts)


def _detail_post(url: str) -> dict[str, object]:
    body = _fetch(url)
    tags = re.findall(r'<a href="https://on\.com2us\.com/tag/[^"]+/">([^<]+)</a>', body)
    published = _first(body, r'<time[^>]+datetime="([^"]+)"') or _first(body, r"<time[^>]*>(20\d{2}\.\d{2}\.\d{2})</time>")
    return {
        "key": _key(url),
        "title": _clean(_meta(body, "og:title", last=True) or _text(body, "title")),
        "description": _clean(_meta(body, "og:description", last=True) or _meta(body, "description", last=True)),
        "tags": [_clean(tag) for tag in tags if _clean(tag)],
        "thumbnail": _absolute(url, _meta(body, "og:image", last=True)),
        "publishedAt": _published(_meta(body, "article:published_time", last=True) or published),
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


def _meta(body: str, name: str, *, last: bool = False) -> str:
    patterns = [
        rf'<meta[^>]+property=["\']{re.escape(name)}["\'][^>]+content=["\']([^"\']+)',
        rf'<meta[^>]+name=["\']{re.escape(name)}["\'][^>]+content=["\']([^"\']+)',
    ]
    for pattern in patterns:
        matches = re.findall(pattern, body, flags=re.IGNORECASE | re.DOTALL)
        if matches:
            return html.unescape(matches[-1] if last else matches[0]).strip()
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
    text = _clean(value).replace(".", "-").replace("Z", "+00:00")
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
