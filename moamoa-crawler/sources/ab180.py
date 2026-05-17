from __future__ import annotations

import html
import re
import urllib.parse
import urllib.request
import xml.etree.ElementTree as ET
from datetime import datetime, timezone
from email.utils import parsedate_to_datetime


KEY = "ab180"
BLOG = "AB180 Engineering"
BASE_URL = "https://engineering.ab180.co"
RSS_URL = "https://raw.githubusercontent.com/ab180/engineering-blog-rss-scheduler/main/rss.xml"
USER_AGENT = "Mozilla/5.0"


def crawl(request, config) -> dict[str, object]:
    del config
    root = ET.fromstring(_fetch(RSS_URL))
    posts: list[dict[str, object]] = []
    for item in root.findall("./channel/item"):
        link = _clean(item.findtext("link"))
        title = _clean(item.findtext("title"))
        if not link or not title:
            continue
        posts.append(
            {
                "key": _key(link),
                "title": title,
                "description": _clean(item.findtext("description")),
                "tags": [_clean(node.text) for node in item.findall("category") if _clean(node.text)],
                "thumbnail": _thumbnail(link),
                "publishedAt": _published(_clean(item.findtext("pubDate"))),
                "url": link,
                "source": "rss",
            }
        )
        if request.size and len(posts) >= request.size:
            break
    if not posts:
        raise RuntimeError(f"{KEY} crawl finished but no RSS items were extracted from {RSS_URL}")
    return _payload(request, RSS_URL, "rss.urllib", posts)


def _thumbnail(url: str) -> str:
    body = _fetch(url)
    return _absolute(url, _meta(body, "og:image"))


def _fetch(url: str) -> str:
    req = urllib.request.Request(url, headers={"User-Agent": USER_AGENT, "Accept": "text/html,application/xml,*/*;q=0.8"})
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


def _absolute(base_url: str, value: str) -> str:
    return urllib.parse.urljoin(base_url, html.unescape(value or ""))


def _clean(value: str | None) -> str:
    return re.sub(r"\s+", " ", re.sub(r"<[^>]+>", " ", html.unescape(value or ""))).strip()


def _key(url: str) -> str:
    return urllib.parse.urlsplit(url).path.strip("/").split("/")[-1]


def _published(value: str) -> str:
    if not value:
        return ""
    try:
        return parsedate_to_datetime(value).astimezone(timezone.utc).replace(tzinfo=None, microsecond=0).isoformat(timespec="seconds")
    except (TypeError, ValueError):
        return ""
