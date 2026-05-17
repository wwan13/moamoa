from __future__ import annotations

import html
import re
import urllib.parse
import urllib.request
from datetime import datetime, timezone


KEY = "nds"
BLOG = "NDS Tech Blog"
BASE_URL = "https://tech.cloud.nongshim.co.kr"
LIST_URLS = [f"{BASE_URL}/post/", f"{BASE_URL}/post/page/2/"]
USER_AGENT = "Mozilla/5.0"
LINK_RE = re.compile(r'href="(https://tech\.cloud\.nongshim\.co\.kr/blog/[^"]+/[0-9]+/)"')


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
        raise RuntimeError(f"{KEY} crawl finished but no NDS post links were extracted from {LIST_URLS[0]}")
    return _payload(request, LIST_URLS[0], "html.urllib", posts)


def _detail_post(url: str) -> dict[str, object]:
    body = _fetch(url)
    return {
        "key": _key(url),
        "title": _clean(_meta(body, "og:title")).removesuffix(" - NDS Cloud Tech Blog"),
        "description": _clean(_meta(body, "og:description") or _meta(body, "description")),
        "tags": _keywords(body),
        "thumbnail": _absolute(url, _meta(body, "og:image")),
        "publishedAt": _published(_meta(body, "article:published_time")),
        "url": url,
        "source": "html",
    }


def _keywords(body: str) -> list[str]:
    match = re.search(r'"keywords":\[(.*?)\]', body, flags=re.DOTALL)
    if not match:
        return []
    return [_clean(part) for part in re.findall(r'"([^"]+)"', match.group(1)) if _clean(part)]


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


def _key(url: str) -> str:
    return urllib.parse.urlsplit(url).path.strip("/").split("/")[-1]


def _published(value: str) -> str:
    if not value:
        return ""
    parsed = datetime.fromisoformat(value.replace("Z", "+00:00"))
    if parsed.tzinfo is not None:
        parsed = parsed.astimezone(timezone.utc).replace(tzinfo=None)
    return parsed.replace(microsecond=0).isoformat(timespec="seconds")
