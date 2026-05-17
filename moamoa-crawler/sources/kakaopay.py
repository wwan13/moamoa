from __future__ import annotations

import html
import re
import urllib.parse
import urllib.request
from datetime import datetime, timezone


KEY = "kakaopay"
BLOG = "KakaoPay Tech Blog"
BASE_URL = "https://tech.kakaopay.com"
LIST_URL = BASE_URL
USER_AGENT = "Mozilla/5.0"
CARD_RE = re.compile(
    r'<a href="(/post/[^"]+/)">.*?<img alt="([^"]+)"[^>]+src="([^"]+)".*?<strong>(.*?)</strong>\s*<p>(.*?)</p>\s*<time>(.*?)</time>',
    flags=re.DOTALL,
)


def crawl(request, config) -> dict[str, object]:
    del config
    body = _fetch(LIST_URL)
    posts: list[dict[str, object]] = []
    for match in CARD_RE.finditer(body):
        url = _absolute(BASE_URL, match.group(1))
        posts.append(
            {
                "key": _key(url),
                "title": _clean(match.group(4)),
                "description": _clean(match.group(5)),
                "tags": [],
                "thumbnail": _absolute(BASE_URL, match.group(3)),
                "publishedAt": _published(_clean(match.group(6))),
                "url": url,
                "source": "html",
            }
        )
        if request.size and len(posts) >= request.size:
            break
    if not posts:
        raise RuntimeError(f"{KEY} crawl finished but no KakaoPay post cards were extracted from {LIST_URL}")
    return _payload(request, LIST_URL, "html.urllib", posts)


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
    if not value:
        return ""
    try:
        parsed = datetime.fromisoformat(value.replace(".", "-").replace(" ", "").replace("-", "-", 2))
        return parsed.replace(microsecond=0).isoformat(timespec="seconds")
    except ValueError:
        match = re.search(r"(20\d{2})\.\s*(\d{1,2})\.\s*(\d{1,2})", value)
        if not match:
            return ""
        return f"{match.group(1)}-{int(match.group(2)):02d}-{int(match.group(3)):02d}T00:00:00"


def _key(url: str) -> str:
    return urllib.parse.urlsplit(url).path.strip("/").split("/")[-1]
