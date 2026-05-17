from __future__ import annotations

import html
import re
import urllib.parse
import urllib.request
from datetime import datetime, timezone
from email.utils import parsedate_to_datetime


KEY = "rapportlabs"
BLOG = "Rapport Labs Tech Blog"
BASE_URL = "https://blog.rapportlabs.kr"
LIST_URLS = [BASE_URL, f"{BASE_URL}?page=2"]
USER_AGENT = "Mozilla/5.0"
CARD_RE = re.compile(
    r'\\"id\\":(\d+),\\"slug\\":\\".*?\\",\\"title\\":\\"(.*?)\\",\\"description\\":\\"(.*?)\\",\\"published\\":true,\\"published_at\\":\\"(.*?)\\".*?\\"image\\":\{\\"url\\":\\"(.*?)\\".*?\\"posts_tags\\":\[(.*?)\]',
    flags=re.DOTALL,
)


def crawl(request, config) -> dict[str, object]:
    del config
    posts: list[dict[str, object]] = []
    seen: set[str] = set()
    for list_url in LIST_URLS:
        body = _fetch(list_url)
        for post_id, title, description, published, image, tags_blob in CARD_RE.findall(body):
            url = f"{BASE_URL}/{post_id}"
            if url in seen:
                continue
            seen.add(url)
            posts.append(
                {
                    "key": post_id,
                    "title": _clean(_unescape_json(title)),
                    "description": _clean(_unescape_json(description)),
                    "tags": [_clean(_unescape_json(tag)) for tag in re.findall(r'\\"title\\":\\"(.*?)\\"', tags_blob) if _clean(_unescape_json(tag))],
                    "thumbnail": _clean(_unescape_json(image)),
                    "publishedAt": _published(_unescape_json(published)),
                    "url": url,
                    "source": "html-json",
                }
            )
            if request.size and len(posts) >= request.size:
                break
        if request.size and len(posts) >= request.size:
            break
    if not posts:
        raise RuntimeError(f"{KEY} crawl finished but no Rapport Labs post links were extracted from {LIST_URLS[0]}")
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
        rf'<meta[^>]+content=["\']([^"\']+)["\'][^>]+property=["\']{re.escape(name)}["\']',
        rf'<meta[^>]+content=["\']([^"\']+)["\'][^>]+name=["\']{re.escape(name)}["\']',
    ]
    for pattern in patterns:
        match = re.search(pattern, body, flags=re.IGNORECASE | re.DOTALL)
        if match:
            return html.unescape(match.group(1)).strip()
    return ""


def _text(body: str, tag: str) -> str:
    match = re.search(rf"<{tag}[^>]*>(.*?)</{tag}>", body, flags=re.IGNORECASE | re.DOTALL)
    return _clean(match.group(1)) if match else ""


def _clean(value: str) -> str:
    return re.sub(r"\s+", " ", re.sub(r"<[^>]+>", " ", html.unescape(value or ""))).strip()


def _absolute(base_url: str, value: str) -> str:
    if not value:
        return ""
    return urllib.parse.urljoin(base_url, html.unescape(value))


def _key(url: str) -> str:
    return urllib.parse.urlsplit(url).path.strip("/").split("/")[-1]


def _unescape_json(value: str) -> str:
    return value.replace("\\/", "/").replace("\\n", " ").replace('\\"', '"')


def _published(value: str) -> str:
    text = _clean(value)
    if not text:
        return ""
    text = text.replace("Z", "+00:00")
    text = re.sub(r"(\.\d{1,5})([+-]\d{2}:\d{2})$", lambda m: "." + m.group(1)[1:].ljust(6, "0") + m.group(2), text)
    for candidate in (text, text.rstrip(".").replace(".", "-")):
        try:
            parsed = datetime.fromisoformat(candidate)
            if parsed.tzinfo is not None:
                parsed = parsed.astimezone(timezone.utc).replace(tzinfo=None)
            return parsed.replace(microsecond=0).isoformat(timespec="seconds")
        except ValueError:
            pass
    try:
        return parsedate_to_datetime(text).astimezone(timezone.utc).replace(tzinfo=None, microsecond=0).isoformat(timespec="seconds")
    except (TypeError, ValueError):
        return ""
