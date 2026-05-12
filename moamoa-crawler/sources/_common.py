from __future__ import annotations

import html
import json
import re
import subprocess
import urllib.parse
import urllib.request
from dataclasses import asdict, dataclass
from datetime import datetime, timezone
from html.parser import HTMLParser
from typing import Any, Iterable


USER_AGENT = (
    "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) "
    "AppleWebKit/537.36 (KHTML, like Gecko) "
    "Chrome/120.0.0.0 Safari/537.36"
)


@dataclass(frozen=True)
class Post:
    key: str
    title: str
    description: str
    tags: list[str]
    thumbnail: str
    publishedAt: str
    url: str
    source: str


@dataclass(frozen=True)
class Link:
    url: str
    text: str


class AnchorParser(HTMLParser):
    def __init__(self, base_url: str):
        super().__init__(convert_charrefs=True)
        self.base_url = base_url
        self.links: list[Link] = []
        self._href_stack: list[str | None] = []
        self._text_stack: list[list[str]] = []

    def handle_starttag(self, tag: str, attrs: list[tuple[str, str | None]]) -> None:
        attrs_map = {name.lower(): value for name, value in attrs}
        if tag.lower() == "a":
            href = attrs_map.get("href")
            self._href_stack.append(href)
            self._text_stack.append([])

    def handle_data(self, data: str) -> None:
        if self._text_stack:
            self._text_stack[-1].append(data)

    def handle_endtag(self, tag: str) -> None:
        if tag.lower() != "a" or not self._href_stack:
            return
        href = self._href_stack.pop()
        text = normalize_space(" ".join(self._text_stack.pop()))
        if href:
            self.links.append(Link(url=normalize_url(self.base_url, href), text=text))


def fetch_text(url: str, *, method: str = "GET", data: bytes | None = None, headers: dict[str, str] | None = None) -> str:
    request_headers = {
        "User-Agent": USER_AGENT,
        "Accept": "text/html,application/xhtml+xml,application/xml,application/json;q=0.9,*/*;q=0.8",
    }
    if headers:
        request_headers.update(headers)
    req = urllib.request.Request(url, data=data, method=method, headers=request_headers)
    try:
        with urllib.request.urlopen(req, timeout=20) as response:
            charset = response.headers.get_content_charset() or "utf-8"
            return response.read().decode(charset, errors="replace")
    except Exception:
        command = ["curl", "-fsSL", "--max-time", "25", "-X", method]
        for name, value in request_headers.items():
            command.extend(["-H", f"{name}: {value}"])
        if data is not None:
            command.extend(["--data-binary", data.decode("utf-8")])
        command.append(url)
        completed = subprocess.run(command, check=True, capture_output=True, text=True)
        return completed.stdout


def fetch_json(url: str, *, method: str = "GET", data: bytes | None = None, headers: dict[str, str] | None = None) -> Any:
    text = fetch_text(url, method=method, data=data, headers=headers)
    return json.loads(text)


def normalize_space(value: str | None) -> str:
    if not value:
        return ""
    return re.sub(r"\s+", " ", html.unescape(value).replace("\u00a0", " ")).strip()


def strip_html(value: str | None) -> str:
    if not value:
        return ""
    return normalize_space(re.sub(r"<[^>]+>", " ", value))


def normalize_tag(value: str | None) -> str:
    return normalize_space(value).lstrip("#")


def normalize_url(base_url: str, value: str) -> str:
    absolute = urllib.parse.urljoin(base_url, html.unescape(value).strip())
    parsed = urllib.parse.urlsplit(absolute)
    return urllib.parse.urlunsplit((parsed.scheme, parsed.netloc, parsed.path, parsed.query, ""))


def url_without_query(url: str) -> str:
    parsed = urllib.parse.urlsplit(url)
    return urllib.parse.urlunsplit((parsed.scheme, parsed.netloc, parsed.path, "", ""))


def key_from_url(url: str) -> str:
    parsed = urllib.parse.urlsplit(url_without_query(url))
    path = parsed.path.strip("/")
    if not path:
        return parsed.netloc
    return path.removesuffix(".html").split("/")[-1] or path


def title_from_url(url: str) -> str:
    key = urllib.parse.unquote(key_from_url(url))
    key = re.sub(r"[-_]+", " ", key)
    return normalize_space(key)


def extract_links(body: str, base_url: str) -> list[Link]:
    parser = AnchorParser(base_url)
    parser.feed(body)
    return parser.links


def extract_meta(body: str, name: str) -> str:
    patterns = [
        rf'<meta[^>]+property=["\']{re.escape(name)}["\'][^>]+content=["\']([^"\']+)["\']',
        rf'<meta[^>]+name=["\']{re.escape(name)}["\'][^>]+content=["\']([^"\']+)["\']',
        rf'<meta[^>]+content=["\']([^"\']+)["\'][^>]+property=["\']{re.escape(name)}["\']',
        rf'<meta[^>]+content=["\']([^"\']+)["\'][^>]+name=["\']{re.escape(name)}["\']',
    ]
    for pattern in patterns:
        match = re.search(pattern, body, flags=re.IGNORECASE | re.DOTALL)
        if match:
            return normalize_space(match.group(1))
    return ""


def extract_title(body: str) -> str:
    og_title = extract_meta(body, "og:title")
    if og_title:
        return og_title
    match = re.search(r"<title[^>]*>(.*?)</title>", body, flags=re.IGNORECASE | re.DOTALL)
    return strip_html(match.group(1)) if match else ""


def iso_now() -> str:
    return datetime.now(timezone.utc).isoformat()


def make_payload(
    *,
    key: str,
    blog: str,
    base_url: str,
    requested_url: str,
    crawler: str,
    requested_size: int,
    posts: list[Post],
) -> dict[str, object]:
    return {
        "key": key,
        "blog": blog,
        "baseUrl": base_url,
        "requestedUrl": requested_url,
        "crawler": crawler,
        "crawledAt": iso_now(),
        "requestedSize": requested_size,
        "postCount": len(posts),
        "posts": [asdict(post) for post in posts],
    }


def unique_posts(posts: Iterable[Post], limit: int) -> list[Post]:
    result: list[Post] = []
    seen: set[str] = set()
    for post in posts:
        if not post.url or post.url in seen:
            continue
        seen.add(post.url)
        result.append(post)
        if len(result) >= limit:
            break
    return result


def html_link_posts(
    *,
    key: str,
    blog: str,
    base_url: str,
    list_urls: list[str],
    include: str,
    request: Any,
    crawler: str = "html.urllib",
    exclude: str | None = None,
) -> dict[str, object]:
    include_re = re.compile(include)
    exclude_re = re.compile(exclude) if exclude else None
    posts: list[Post] = []
    first_url = list_urls[0]

    for list_url in list_urls:
        body = fetch_text(list_url)
        for link in extract_links(body, list_url):
            url = url_without_query(link.url)
            if not include_re.search(url):
                continue
            if exclude_re and exclude_re.search(url):
                continue
            title = link.text or title_from_url(url)
            if not title:
                continue
            posts.append(
                Post(
                    key=key_from_url(url),
                    title=title,
                    description="",
                    tags=[],
                    thumbnail="",
                    publishedAt="",
                    url=url,
                    source="html",
                )
            )
            posts = unique_posts(posts, request.size)
            if len(posts) >= request.size:
                break
        if len(posts) >= request.size:
            break

    posts = unique_posts(posts, request.size)
    if not posts:
        raise RuntimeError(f"{key} crawl finished but no post links were extracted from {first_url}")

    return make_payload(
        key=key,
        blog=blog,
        base_url=base_url,
        requested_url=first_url,
        crawler=crawler,
        requested_size=request.size,
        posts=posts,
    )


def medium_browser_posts(
    *,
    key: str,
    blog: str,
    base_url: str,
    request: Any,
    config: Any,
    post_path_prefix: str | None = None,
    publication_host: str | None = None,
) -> dict[str, object]:
    from scrapling.fetchers import StealthyFetcher

    parsed_base = urllib.parse.urlsplit(base_url)
    allowed_hosts = {parsed_base.netloc}
    if publication_host:
        allowed_hosts.add(publication_host)
    list_url = base_url.rstrip("/") + "/all"

    def fetch_page(url: str) -> Any:
        return StealthyFetcher.fetch(
            url,
            headless=config.headless,
            network_idle=True,
            wait=config.wait,
            page_action=_scroll_until_stable(config.scroll_wait),
        )

    posts: list[Post] = []
    page = fetch_page(list_url)
    posts.extend(_medium_posts_from_page(page, base_url, allowed_hosts, post_path_prefix))
    posts = unique_posts(posts, len(posts))
    if not posts:
        raise RuntimeError(f"{key} Medium browser crawl finished but no post links were extracted from {list_url}")

    return make_payload(
        key=key,
        blog=blog,
        base_url=base_url,
        requested_url=list_url,
        crawler="scrapling.StealthyFetcher",
        requested_size=len(posts),
        posts=posts,
    )


def _scroll_page(scrolls: int, scroll_wait: int):
    def action(page: Any) -> None:
        for _ in range(scrolls):
            page.mouse.wheel(0, 2400)
            page.wait_for_timeout(scroll_wait)

    return action


def _scroll_until_stable(scroll_wait: int):
    def action(page: Any) -> None:
        stable_count = 0
        previous_height = 0
        for _ in range(200):
            current_height = page.evaluate("document.body.scrollHeight")
            if current_height == previous_height:
                stable_count += 1
            else:
                stable_count = 0
                previous_height = current_height
            if stable_count >= 3:
                break
            page.mouse.wheel(0, 2400)
            page.wait_for_timeout(scroll_wait)

    return action


def _medium_posts_from_page(page: Any, base_url: str, allowed_hosts: set[str], post_path_prefix: str | None) -> list[Post]:
    posts: list[Post] = []
    for anchor in page.css("a[href]"):
        href = _selector_attr(anchor, "href")
        if not href:
            continue
        url = url_without_query(normalize_url(base_url, href))
        if not _is_medium_post_url(url, allowed_hosts, post_path_prefix):
            continue
        title = _selector_text(anchor) or _selector_attr(anchor, "aria-label") or _selector_attr(anchor, "title") or title_from_url(url)
        title = re.sub(r"-[0-9a-f]{12,}$", "", title).replace("-", " ")
        title = normalize_space(title)
        if title:
            posts.append(Post(key_from_url(url), title, "", [], "", "", url, "browser"))
    return posts


def _is_medium_post_url(url: str, allowed_hosts: set[str], post_path_prefix: str | None) -> bool:
    parsed = urllib.parse.urlsplit(url)
    path = urllib.parse.unquote(parsed.path)
    if parsed.netloc not in allowed_hosts:
        return False
    if post_path_prefix and not path.startswith(post_path_prefix):
        return False
    excluded = ("/about", "/archive", "/followers", "/latest", "/lists", "/membership", "/search", "/subpage", "/tagged")
    path_after_prefix = path.removeprefix(post_path_prefix or "")
    if path_after_prefix.startswith(excluded):
        return False
    return re.search(r"-[0-9a-f]{12}/?$", path) is not None


def _selector_attr(selector: Any, name: str) -> str:
    if hasattr(selector, "attrib"):
        value = selector.attrib.get(name)
        if value:
            return str(value)
    if hasattr(selector, "get"):
        try:
            value = selector.get(name)
            if value:
                return str(value)
        except TypeError:
            return ""
    return ""


def _selector_text(selector: Any) -> str:
    value = getattr(selector, "text", "")
    if callable(value):
        value = value()
    return normalize_space(str(value)) if value else ""
