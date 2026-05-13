import html
import re
import urllib.parse
import urllib.request
from datetime import datetime, timezone
from typing import Any, Optional


KEY = "kyobodts"
BLOG = "Kyobo DTS Tech Blog"
BASE_URL = "https://blog.kyobodts.co.kr"
LIST_URL = BASE_URL
USER_AGENT = (
    "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) "
    "AppleWebKit/537.36 (KHTML, like Gecko) "
    "Chrome/120.0.0.0 Safari/537.36"
)
POST_URL_RE = re.compile(r"^https://blog\.kyobodts\.co\.kr/\d{4}/\d{2}/\d{2}/[^?#]+/?$")
CARD_BLOCK_RE = re.compile(r'<div class="elementor-widget-wrap elementor-element-populated">(.*?)</div>\s*</div>\s*</div>\s*</section>', re.DOTALL)
TITLE_RE = re.compile(
    r'<h[1-6][^>]*class="elementor-heading-title[^"]*"[^>]*>\s*<a href="([^"]+)">(.*?)</a>\s*</h[1-6]>',
    re.DOTALL,
)
IMAGE_RE = re.compile(r'<img[^>]+src="([^"]+)"', re.DOTALL)
DESCRIPTION_RE = re.compile(
    r'data-widget_type="text-editor\.default".*?<div class="elementor-widget-container">\s*(?:<style.*?</style>\s*)?(.*?)</div>',
    re.DOTALL,
)


def crawl(request, config) -> dict[str, object]:
    del config

    body = _fetch_text(LIST_URL)

    posts_by_url: dict[str, dict[str, Any]] = {}
    for block in CARD_BLOCK_RE.findall(body):
        title_match = TITLE_RE.search(block)
        if not title_match:
            continue

        url = _url_without_query(_normalize_url(BASE_URL, title_match.group(1)))
        if not POST_URL_RE.match(url):
            continue

        image_match = IMAGE_RE.search(block)
        description_match = DESCRIPTION_RE.search(block)
        title = _normalize_space(_strip_html(title_match.group(2))) or _title_from_url(url)
        posts_by_url[url] = {
            "key": _key_from_url(url),
            "title": title,
            "description": _normalize_space(_strip_html(description_match.group(1))) if description_match else "",
            "tags": [],
            "thumbnail": _normalize_url(BASE_URL, image_match.group(1)) if image_match else "",
            "publishedAt": _published_at_from_url(url),
            "url": url,
            "source": "html",
        }

        if request.size is not None and len(posts_by_url) >= request.size:
            break

    posts = [post for post in posts_by_url.values() if post]
    if not posts:
        raise RuntimeError(f"{KEY} crawl finished but no post links were extracted from {LIST_URL}")

    return {
        "key": KEY,
        "blog": BLOG,
        "baseUrl": BASE_URL,
        "requestedUrl": LIST_URL,
        "crawler": "html.urllib",
        "crawledAt": datetime.now(timezone.utc).isoformat(),
        "requestedSize": request.size,
        "postCount": len(posts),
        "posts": posts,
    }


def _fetch_text(url: str) -> str:
    request = urllib.request.Request(
        url,
        headers={
            "User-Agent": USER_AGENT,
            "Accept": "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8",
        },
    )
    with urllib.request.urlopen(request, timeout=20) as response:
        charset = response.headers.get_content_charset() or "utf-8"
        return response.read().decode(charset, errors="replace")


def _normalize_space(value: Optional[str]) -> str:
    if not value:
        return ""
    return re.sub(r"\s+", " ", html.unescape(value).replace("\u00a0", " ")).strip()


def _strip_html(value: Optional[str]) -> str:
    if not value:
        return ""
    return re.sub(r"<[^>]+>", " ", value)


def _normalize_url(base_url: str, value: str) -> str:
    absolute = urllib.parse.urljoin(base_url, html.unescape(value).strip())
    parsed = urllib.parse.urlsplit(absolute)
    scheme = parsed.scheme or "https"
    return urllib.parse.urlunsplit((scheme, parsed.netloc, parsed.path, parsed.query, ""))


def _url_without_query(url: str) -> str:
    parsed = urllib.parse.urlsplit(url)
    return urllib.parse.urlunsplit(("https", parsed.netloc, parsed.path, "", ""))


def _key_from_url(url: str) -> str:
    path = urllib.parse.urlsplit(url).path.strip("/")
    return path.split("/")[-1] or urllib.parse.urlsplit(url).netloc


def _title_from_url(url: str) -> str:
    value = urllib.parse.unquote(_key_from_url(url))
    value = re.sub(r"[-_]+", " ", value)
    return _normalize_space(value)


def _published_at_from_url(url: str) -> str:
    match = re.search(r"/(\d{4})/(\d{2})/(\d{2})/", urllib.parse.urlsplit(url).path)
    if not match:
        return ""
    return f"{match.group(1)}-{match.group(2)}-{match.group(3)}T12:00:00"
