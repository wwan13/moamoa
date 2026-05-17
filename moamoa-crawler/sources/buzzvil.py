from __future__ import annotations

import html
import re
import urllib.parse
import urllib.request
import xml.etree.ElementTree as ET
from datetime import datetime, timezone
from email.utils import parsedate_to_datetime


KEY = "buzzvil"
BLOG = "Buzzvil Tech Blog"
BASE_URL = "https://tech.buzzvil.com"
LIST_URL = f"{BASE_URL}/blog"
FEED_URL = f"{BASE_URL}/feed.xml"
USER_AGENT = "Mozilla/5.0"


def crawl(request, config) -> dict[str, object]:
    del config
    root = ET.fromstring(_fetch(FEED_URL))
    posts: list[dict[str, object]] = []
    for item in root.findall("./channel/item"):
        url = _clean(item.findtext("link"))
        title = _clean(item.findtext("title"))
        if not url or not title:
            continue
        posts.append(
            {
                "key": _key(url),
                "title": title,
                "description": _clean(item.findtext("description")),
                "tags": [_clean(node.text) for node in item.findall("category") if _clean(node.text)],
                "thumbnail": _thumbnail(url),
                "publishedAt": _published(_clean(item.findtext("pubDate"))),
                "url": url,
                "source": "rss",
            }
        )
        if request.size and len(posts) >= request.size:
            break
    if not posts:
        raise RuntimeError(f"{KEY} crawl finished but no Buzzvil feed items were extracted from {FEED_URL}")
    return _payload(request, FEED_URL, "rss.urllib", posts)


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


def _thumbnail(url: str) -> str:
    return _absolute(url, _meta(_fetch(url), "og:image"))


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


def _clean(value: str | None) -> str:
    return re.sub(r"\s+", " ", re.sub(r"<[^>]+>", " ", html.unescape(value or ""))).strip()


def _absolute(base_url: str, value: str) -> str:
    return urllib.parse.urljoin(base_url, value)


def _published(value: str) -> str:
    if not value:
        return ""
    try:
        parsed = parsedate_to_datetime(value)
        return parsed.astimezone(timezone.utc).replace(tzinfo=None, microsecond=0).isoformat(timespec="seconds")
    except (TypeError, ValueError):
        return ""


def _key(url: str) -> str:
    return urllib.parse.urlsplit(url).path.strip("/").split("/")[-1]
