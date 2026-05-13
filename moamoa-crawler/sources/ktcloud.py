from __future__ import annotations

import re
import urllib.parse

from _common import Post, key_from_url, make_payload, normalize_published_at, normalize_space, normalize_url, unique_posts


KEY = "ktcloud"
BASE_URL = "https://tech.ktcloud.com"
REQUESTED_URL = "https://tech.ktcloud.com/category/Tech%20Story"
THUMBNAIL_URL_RE = re.compile(r'url\(["\']?(.*?)["\']?\)')


def crawl(request, config) -> dict[str, object]:
    from scrapling.fetchers import StealthyFetcher

    posts: list[Post] = []
    seen_urls: set[str] = set()

    for page_number in range(1, 101):
        page_url = _category_page_url(page_number)
        page = StealthyFetcher.fetch(
            page_url,
            headless=config.headless,
            network_idle=True,
            wait=config.wait,
            page_action=_scroll_page(config.scrolls, config.scroll_wait),
        )
        page_posts = _extract_posts_from_page(page)
        if not page_posts:
            break

        new_count = 0
        for post in page_posts:
            if post.url in seen_urls:
                continue
            seen_urls.add(post.url)
            posts.append(post)
            new_count += 1

        posts = unique_posts(posts, request.size)
        if request.size is not None and len(posts) >= request.size:
            break
        if new_count == 0:
            break

    if not posts:
        raise RuntimeError(f"{KEY} browser crawl finished but no post links were extracted from {REQUESTED_URL}")

    return make_payload(
        key=KEY,
        blog="kt cloud Tech Blog",
        base_url=BASE_URL,
        requested_url=REQUESTED_URL,
        crawler="scrapling.StealthyFetcher",
        requested_size=request.size,
        posts=posts,
    )


def _extract_posts_from_page(page) -> list[Post]:
    posts: list[Post] = []
    for article in page.css("article.article-type-common[role='article']"):
        post = _extract_post(article)
        if post is None:
            continue
        posts.append(post)
    return unique_posts(posts, 10_000)


def _extract_post(article) -> Post | None:
    link = _first(article.css("a.link-article[href]"))
    if link is None:
        return None

    href = _attr(link, "href")
    if not href:
        return None

    url = normalize_url(BASE_URL, href)
    if not is_post_url(url):
        return None

    title = _text(_first(article.css("strong.title"))) or _attr(link, "title")
    title = normalize_space(title)
    if not title:
        return None

    description = _text(_first(article.css("p.summary")))
    category = _text(_first(article.css(".box-meta .link-category")))
    published_at = normalize_published_at(_text(_first(article.css(".box-meta .date"))))
    thumbnail = _thumbnail_url(_first(article.css("p.thumbnail")))

    return Post(
        key=key_from_url(url),
        title=title,
        description=description,
        tags=[category] if category else [],
        thumbnail=thumbnail,
        publishedAt=published_at,
        url=url,
        source="browser",
    )


def is_post_url(url: str) -> bool:
    parsed = urllib.parse.urlsplit(url)
    if parsed.netloc != "tech.ktcloud.com":
        return False
    return urllib.parse.unquote(parsed.path).startswith("/entry/")


def _thumbnail_url(node) -> str:
    if node is None:
        return ""
    style = _attr(node, "style")
    if not style:
        return ""
    match = THUMBNAIL_URL_RE.search(style)
    if not match:
        return ""
    return normalize_url(BASE_URL, match.group(1))


def _category_page_url(page_number: int) -> str:
    if page_number <= 1:
        return REQUESTED_URL
    return f"{REQUESTED_URL}?page={page_number}"


def _scroll_page(scrolls: int, scroll_wait: int):
    def action(page) -> None:
        for _ in range(scrolls):
            page.mouse.wheel(0, 2400)
            page.wait_for_timeout(scroll_wait)

    return action


def _first(nodes):
    for node in nodes:
        return node
    return None


def _text(node) -> str:
    if node is None:
        return ""
    value = getattr(node, "text", "")
    if callable(value):
        value = value()
    return normalize_space(str(value)) if value else ""


def _attr(node, name: str) -> str:
    if node is None:
        return ""
    if hasattr(node, "attrib"):
        value = node.attrib.get(name)
        if value:
            return str(value)
    if hasattr(node, "get"):
        try:
            value = node.get(name)
        except TypeError:
            value = None
        if value:
            return str(value)
    return ""
