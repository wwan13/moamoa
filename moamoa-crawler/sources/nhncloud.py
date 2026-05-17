from __future__ import annotations

import html
import json
import re
import urllib.request
from datetime import datetime, timezone


KEY = "nhncloud"
BLOG = "NHN Cloud Meetup"
BASE_URL = "https://meetup.nhncloud.com"
API_URL = f"{BASE_URL}/tcblog/v1.0/posts?pageNo=1&rowsPerPage=30"
USER_AGENT = "Mozilla/5.0"


def crawl(request, config) -> dict[str, object]:
    del config
    data = json.loads(_fetch(API_URL, accept="application/json"))
    if not (data.get("header") or {}).get("isSuccessful"):
        raise RuntimeError(f"{KEY} crawl failed: NHN Cloud API was not successful")
    posts: list[dict[str, object]] = []
    for item in data.get("posts") or []:
        lang = item.get("postPerLang") or {}
        post_id = item.get("postId")
        title = _clean(lang.get("title"))
        if post_id is None or not title:
            continue
        posts.append(
            {
                "key": str(post_id),
                "title": title,
                "description": _clean(item.get("contentPreview") or lang.get("description")),
                "tags": [],
                "thumbnail": _clean(lang.get("repImageUrl")),
                "publishedAt": _published(_clean(item.get("regDt") or item.get("publishTime") or item.get("regTime"))),
                "url": f"{BASE_URL}/posts/{post_id}",
                "source": "api",
            }
        )
        if request.size and len(posts) >= request.size:
            break
    if not posts:
        raise RuntimeError(f"{KEY} crawl finished but NHN Cloud API returned no posts: {API_URL}")
    return _payload(request, API_URL, "api.urllib", posts)


def _fetch(url: str, *, accept: str) -> str:
    req = urllib.request.Request(url, headers={"User-Agent": USER_AGENT, "Accept": accept})
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


def _clean(value: object) -> str:
    return re.sub(r"\s+", " ", html.unescape(str(value or "")).replace("\u00a0", " ")).strip()


def _published(value: str) -> str:
    if not value:
        return ""
    value = value.replace("Z", "+00:00")
    value = re.sub(r"([+-]\d{2})(\d{2})$", r"\1:\2", value)
    if "T" not in value and re.fullmatch(r"20\d{2}\.\d{2}\.\d{2}", value):
        return f"{value.replace('.', '-')}T00:00:00"
    try:
        parsed = datetime.fromisoformat(value)
        if parsed.tzinfo is not None:
            parsed = parsed.astimezone(timezone.utc).replace(tzinfo=None)
        return parsed.replace(microsecond=0).isoformat(timespec="seconds")
    except ValueError:
        return ""
