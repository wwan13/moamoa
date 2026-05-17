from __future__ import annotations

import html
import json
import re
import urllib.parse
import urllib.request
from datetime import datetime, timezone


KEY = "kakao"
BLOG = "Kakao Tech"
BASE_URL = "https://tech.kakao.com"
API_URL = f"{BASE_URL}/api/v2/posts?page=1&code=blog"
USER_AGENT = "Mozilla/5.0"


def crawl(request, config) -> dict[str, object]:
    del config
    req = urllib.request.Request(
        API_URL,
        headers={"User-Agent": USER_AGENT, "Accept": "application/json", "Referer": f"{BASE_URL}/blog"},
    )
    with urllib.request.urlopen(req, timeout=20) as response:
        data = json.loads(response.read().decode(response.headers.get_content_charset() or "utf-8", errors="replace"))
    posts: list[dict[str, object]] = []
    for item in data.get("postList") or []:
        post_id = item.get("id")
        title = _clean(item.get("title"))
        if post_id is None or not title:
            continue
        url = f"{BASE_URL}/posts/{post_id}"
        posts.append(
            {
                "key": _key(url),
                "title": title,
                "description": _clean(item.get("description")),
                "tags": [_clean(category.get("name")) for category in item.get("categories") or [] if _clean(category.get("name"))],
                "thumbnail": _clean(item.get("thumbnailUri") or item.get("thumb")),
                "publishedAt": _published(_clean(item.get("releaseDateTime") or item.get("releaseDate"))),
                "url": url,
                "source": "api",
            }
        )
        if request.size and len(posts) >= request.size:
            break
    if not posts:
        raise RuntimeError(f"{KEY} crawl finished but Kakao API returned no posts: {API_URL}")
    return _payload(request, API_URL, "api.urllib", posts)


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


def _clean(value: object) -> str:
    return re.sub(r"\s+", " ", re.sub(r"<[^>]+>", " ", html.unescape(str(value or "")))).strip()


def _published(value: str) -> str:
    if not value:
        return ""
    value = value.replace("Z", "+00:00").replace(".", "-")
    if re.fullmatch(r"20\d{2}-\d{2}-\d{2}", value):
        return f"{value}T00:00:00"
    try:
        parsed = datetime.fromisoformat(value)
        if parsed.tzinfo is not None:
            parsed = parsed.astimezone(timezone.utc).replace(tzinfo=None)
        return parsed.replace(microsecond=0).isoformat(timespec="seconds")
    except ValueError:
        return ""


def _key(url: str) -> str:
    return urllib.parse.urlsplit(url).path.strip("/").split("/")[-1]
