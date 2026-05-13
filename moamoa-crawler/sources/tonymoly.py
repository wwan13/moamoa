from __future__ import annotations

import re
import urllib.parse

from _common import Post, enrich_post_details, key_from_url, make_payload, title_from_url, unique_posts


KEY = "tonymoly"
BLOG = "토니모리 테크블로그 : TonyTech"
BASE_URL = "https://tonymoly-tech.medium.com"
REQUESTED_URL = BASE_URL


def is_post_url(url: str) -> bool:
    parsed = urllib.parse.urlsplit(url)
    if parsed.netloc != "tonymoly-tech.medium.com":
        return False
    if parsed.path in {"", "/", "/all"}:
        return False
    if parsed.path.startswith(("/tagged/", "/m/", "/me/", "/search")):
        return False
    return re.search(r"-[0-9a-f]{12,}/?$", urllib.parse.unquote(parsed.path)) is not None


def crawl(request, config) -> dict[str, object]:
    from scrapling.fetchers import StealthyFetcher

    page = StealthyFetcher.fetch(
        REQUESTED_URL,
        headless=config.headless,
        network_idle=True,
        wait=config.wait,
        page_action=_scroll_page(config.scrolls, config.scroll_wait),
    )
    posts = _extract_posts_from_page(page)
    posts = enrich_post_details(posts, limit=request.size)
    posts = unique_posts(posts, request.size)
    if not posts:
        raise RuntimeError(f"{KEY} browser crawl finished but no post links were extracted from {REQUESTED_URL}")

    return make_payload(
        key=KEY,
        blog=BLOG,
        base_url=BASE_URL,
        requested_url=REQUESTED_URL,
        crawler="scrapling.StealthyFetcher",
        requested_size=request.size,
        posts=posts,
    )


def _extract_posts_from_page(page) -> list[Post]:
    posts: list[Post] = []
    for anchor in page.css("a[href]"):
        href = _attr(anchor, "href")
        if not href:
            continue
        url = urllib.parse.urljoin(BASE_URL, href)
        url = urllib.parse.urlunsplit(urllib.parse.urlsplit(url)._replace(query="", fragment=""))
        if not is_post_url(url):
            continue
        posts.append(
            Post(
                key=key_from_url(url),
                title=title_from_url(url),
                description="",
                tags=[],
                thumbnail="",
                publishedAt="",
                url=url,
                source="browser",
            )
        )
    return unique_posts(posts, 10_000)


def _attr(node, name: str) -> str:
    if hasattr(node, "attrib"):
        value = node.attrib.get(name)
        if value:
            return str(value)
    return ""


def _scroll_page(scrolls: int, scroll_wait: int):
    def action(page) -> None:
        for _ in range(scrolls):
            page.mouse.wheel(0, 2400)
            page.wait_for_timeout(scroll_wait)

    return action
