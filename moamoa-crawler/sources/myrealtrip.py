from __future__ import annotations

import re
import urllib.parse
from typing import Any

from _common import (
    Post,
    fetch_text,
    key_from_url,
    make_payload,
    normalize_space,
    normalize_tag,
    strip_html,
    title_from_url,
    unique_posts,
    url_without_query,
)


KEY = "myrealtrip"
BLOG = "How we build Myrealtrip"
BASE_URL = "https://medium.com/myrealtrip-product"
RSS_URL = "https://medium.com/feed/myrealtrip-product"
TAG_RSS_URLS = [
    "https://medium.com/feed/myrealtrip-product/tagged/ai",
    "https://medium.com/feed/myrealtrip-product/tagged/%EA%B0%9C%EB%B0%9C%EB%AC%B8%ED%99%94",
    "https://medium.com/feed/myrealtrip-product/tagged/%EB%8D%B0%EC%9D%B4%ED%84%B0",
    "https://medium.com/feed/myrealtrip-product/tagged/%EB%94%94%EC%9E%90%EC%9D%B8",
    "https://medium.com/feed/myrealtrip-product/tagged/%ED%94%84%EB%A1%9C%EA%B7%B8%EB%9E%98%EB%B0%8D",
]


def crawl(request, config) -> dict[str, object]:
    posts: list[Post] = []

    for feed_url in [RSS_URL, *TAG_RSS_URLS]:
        posts.extend(_rss_posts(feed_url))
        posts = unique_posts(posts, max(request.size, len(posts)))

    browser_posts = _browser_posts(config)
    posts.extend(browser_posts)
    posts = unique_posts(posts, request.size)

    if not posts:
        raise RuntimeError("myrealtrip crawl finished but no post links were extracted from RSS or Medium subpages")

    return make_payload(
        key=KEY,
        blog=BLOG,
        base_url=BASE_URL,
        requested_url=BASE_URL,
        crawler="rss.urllib+scrapling.StealthyFetcher",
        requested_size=request.size,
        posts=posts,
    )


def _rss_posts(feed_url: str) -> list[Post]:
    body = fetch_text(feed_url)
    posts: list[Post] = []

    for item in re.findall(r"<item\b.*?</item>", body, flags=re.IGNORECASE | re.DOTALL):
        link = url_without_query(normalize_space(_tag(item, "link")))
        title = normalize_space(_tag(item, "title"))
        if not _is_post_url(link) or not title:
            continue

        guid = normalize_space(_tag(item, "guid"))
        key = key_from_url(guid if guid else link)
        tags = [
            normalize_tag(value)
            for value in re.findall(r"<category[^>]*>(?:<!\[CDATA\[)?(.*?)(?:\]\]>)?</category>", item, flags=re.IGNORECASE | re.DOTALL)
            if normalize_tag(value)
        ]
        content = _tag(item, "content:encoded")
        thumbnail_match = re.search(r'<img[^>]+src=["\']([^"\']+)["\']', content, flags=re.IGNORECASE)
        description = strip_html(_first_paragraph(content))
        posts.append(
            Post(
                key=key,
                title=title,
                description=description,
                tags=tags,
                thumbnail=thumbnail_match.group(1) if thumbnail_match else "",
                publishedAt=normalize_space(_tag(item, "pubDate")),
                url=link,
                source="rss",
            )
        )

    return posts


def _browser_posts(config: Any) -> list[Post]:
    try:
        from scrapling.fetchers import StealthyFetcher
    except ImportError:
        return []

    page = StealthyFetcher.fetch(
        BASE_URL,
        headless=config.headless,
        network_idle=True,
        wait=config.wait,
        page_action=_scroll_page(config.scrolls, config.scroll_wait),
    )
    subpage_urls = _subpage_urls(page)
    posts = _posts_from_page(page)

    for subpage_url in subpage_urls:
        subpage = StealthyFetcher.fetch(
            subpage_url,
            headless=config.headless,
            network_idle=True,
            wait=config.wait,
            page_action=_scroll_page(config.scrolls, config.scroll_wait),
        )
        posts.extend(_posts_from_page(subpage))

    return unique_posts(posts, len(posts))


def _subpage_urls(page: Any) -> list[str]:
    urls: list[str] = []
    for anchor in page.css("a[href]"):
        href = _attr(anchor, "href")
        if not href:
            continue
        url = url_without_query(urllib.parse.urljoin(BASE_URL, href))
        if re.match(r"^https://medium\.com/myrealtrip-product/subpage/[0-9a-f]+$", url):
            urls.append(url)
    return sorted(set(urls))


def _posts_from_page(page: Any) -> list[Post]:
    posts: list[Post] = []
    for anchor in page.css("a[href]"):
        href = _attr(anchor, "href")
        if not href:
            continue
        url = url_without_query(urllib.parse.urljoin(BASE_URL, href))
        if not _is_post_url(url):
            continue
        title = _text(anchor) or _attr(anchor, "aria-label") or _attr(anchor, "title") or title_from_url(url)
        title = re.sub(r"-[0-9a-f]{12,}$", "", title).replace("-", " ")
        title = normalize_space(title)
        if not title:
            continue
        posts.append(Post(key_from_url(url), title, "", [], "", "", url, "browser"))
    return posts


def _is_post_url(url: str) -> bool:
    parsed = urllib.parse.urlsplit(url)
    if parsed.netloc != "medium.com":
        return False
    if not parsed.path.startswith("/myrealtrip-product/"):
        return False
    if parsed.path.startswith(
        (
            "/myrealtrip-product/about",
            "/myrealtrip-product/archive",
            "/myrealtrip-product/followers",
            "/myrealtrip-product/subpage",
            "/myrealtrip-product/tagged",
        )
    ):
        return False
    return re.search(r"-[0-9a-f]{12}$", urllib.parse.unquote(parsed.path)) is not None


def _scroll_page(scrolls: int, scroll_wait: int):
    def action(page: Any) -> None:
        for _ in range(scrolls):
            page.mouse.wheel(0, 2400)
            page.wait_for_timeout(scroll_wait)

    return action


def _attr(selector: Any, name: str) -> str:
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


def _text(selector: Any) -> str:
    value = getattr(selector, "text", "")
    if callable(value):
        value = value()
    return normalize_space(str(value)) if value else ""


def _tag(body: str, tag: str) -> str:
    match = re.search(rf"<{tag}[^>]*>(?:<!\[CDATA\[)?(.*?)(?:\]\]>)?</{tag}>", body, flags=re.IGNORECASE | re.DOTALL)
    return match.group(1) if match else ""


def _first_paragraph(body: str) -> str:
    match = re.search(r"<p\b[^>]*>(.*?)</p>", body, flags=re.IGNORECASE | re.DOTALL)
    return match.group(1) if match else ""
