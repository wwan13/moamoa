from __future__ import annotations

import re
import urllib.parse

from _common import Post, fetch_json, key_from_url, make_payload, normalize_published_at, normalize_space, normalize_url, unique_posts


KEY = "line"
BLOG = "LY Corporation Tech Blog"
BASE_URL = "https://techblog.lycorp.co.jp"
REQUESTED_URL = f"{BASE_URL}/ko/page/1"


def crawl(request, config) -> dict[str, object]:
    from scrapling.fetchers import StealthyFetcher

    posts: list[Post] = []
    seen_urls: set[str] = set()

    for page_number in range(1, 101):
        page_url = _page_url(page_number)
        published_at_by_url = _published_at_by_url(page_number)
        page = StealthyFetcher.fetch(
            page_url,
            headless=config.headless,
            network_idle=True,
            wait=config.wait,
        )
        page_posts = _extract_posts_from_page(page, published_at_by_url)
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
        blog=BLOG,
        base_url=BASE_URL,
        requested_url=REQUESTED_URL,
        crawler="scrapling.StealthyFetcher",
        requested_size=request.size,
        posts=posts,
    )


def _extract_posts_from_page(page, published_at_by_url: dict[str, str]) -> list[Post]:
    posts: list[Post] = []
    for anchor in page.css("a.link.list_item[href]"):
        post = _extract_post(anchor, published_at_by_url)
        if post is None:
            continue
        posts.append(post)
    return unique_posts(posts, 10_000)


def _extract_post(anchor, published_at_by_url: dict[str, str]) -> Post | None:
    href = _attr(anchor, "href")
    if not href:
        return None

    url = normalize_url(BASE_URL, href)
    if not is_post_url(url):
        return None

    title = _text(_first(anchor.css("h2.title"))) or _attr(anchor, "title")
    title = normalize_space(title)
    if not title:
        return None

    published_at = published_at_by_url.get(url) or _published_at(anchor)
    thumbnail = _image_url(_first(anchor.css("img")))

    return Post(
        key=key_from_url(url),
        title=title,
        description="",
        tags=[],
        thumbnail=thumbnail,
        publishedAt=published_at,
        url=url,
        source="browser",
    )


def is_post_url(url: str) -> bool:
    parsed = urllib.parse.urlsplit(url)
    if parsed.netloc != "techblog.lycorp.co.jp":
        return False

    path = urllib.parse.unquote(parsed.path)
    if not path.startswith("/ko/"):
        return False
    if path.startswith(("/ko/page/", "/ko/tag/", "/ko/feed/")):
        return False
    return path not in {"/ko", "/ko/"}


def _page_url(page_number: int) -> str:
    return f"{BASE_URL}/ko/page/{page_number}"


def _page_data_url(page_number: int) -> str:
    return f"{BASE_URL}/page-data/ko/page/{page_number}/page-data.json"


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


def _image_url(node) -> str:
    if node is None:
        return ""
    for name in ("src", "data-src", "srcset", "data-srcset"):
        value = _attr(node, name)
        if not value:
            continue
        candidate = value.split(",")[0].strip().split(" ")[0]
        if candidate:
            return normalize_url(BASE_URL, candidate)
    return ""


def _published_at(anchor) -> str:
    text = _text(_first(anchor.css("p.update")))
    match = re.search(r"\d{4}\.\d{2}\.\d{2}", text)
    if not match:
        return normalize_published_at(text)
    return normalize_published_at(match.group(0))


def _published_at_by_url(page_number: int) -> dict[str, str]:
    try:
        data = fetch_json(_page_data_url(page_number))
    except Exception:
        return {}
    edges = (
        data.get("result", {})
        .get("data", {})
        .get("BlogsQuery", {})
        .get("edges", [])
    )

    published_at_by_url: dict[str, str] = {}
    for edge in edges:
        node = edge.get("node", {})
        slug = normalize_space(node.get("slug"))
        if not slug:
            continue

        published_at = normalize_published_at(node.get("pubdate"))
        if not published_at:
            continue

        url = normalize_url(BASE_URL, f"/ko/{slug}")
        published_at_by_url[url] = published_at

    return published_at_by_url
