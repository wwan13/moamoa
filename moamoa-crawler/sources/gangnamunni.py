from __future__ import annotations

import urllib.parse

from concurrent.futures import ThreadPoolExecutor

from _common import (
    Post,
    extract_json_ld_objects,
    extract_meta,
    fetch_text,
    key_from_url,
    make_payload,
    normalize_published_at,
    normalize_space,
    normalize_tag,
    normalize_url,
    parse_html,
    unique_posts,
)


KEY = "gangnamunni"
BLOG = "강남언니 공식 블로그"
BASE_URL = "https://blog.gangnamunni.com"
LIST_URL = f"{BASE_URL}/blog"


def crawl(request, config) -> dict[str, object]:
    from scrapling.fetchers import StealthyFetcher

    page = StealthyFetcher.fetch(
        LIST_URL,
        headless=config.headless,
        network_idle=True,
        wait=config.wait,
        page_action=_scroll_until_stable(config.scroll_wait),
    )
    source = _page_source(page)
    posts = _parse_list_source(source)
    posts = unique_posts(posts, request.size)
    if not posts:
        raise RuntimeError(f"{KEY} browser crawl finished but no post links were extracted from {LIST_URL}")

    posts = _enrich_posts(posts)
    posts = [post for post in posts if post.title]
    posts = unique_posts(posts, request.size)
    if not posts:
        raise RuntimeError(f"{KEY} browser crawl finished but detail parsing returned no posts from {LIST_URL}")

    return make_payload(
        key=KEY,
        blog=BLOG,
        base_url=BASE_URL,
        requested_url=LIST_URL,
        crawler="scrapling.StealthyFetcher",
        requested_size=request.size,
        posts=posts,
    )


def _page_source(page) -> str:
    source = getattr(page, "html_content", "") or getattr(page, "body", "") or str(page)
    if not isinstance(source, str):
        source = str(source)
    return source


def _parse_list_source(source: str) -> list[Post]:
    doc = parse_html(source, LIST_URL)
    posts: list[Post] = []
    for anchor in doc.select('a[href*="/post/"]'):
        href = anchor.abs_url("href")
        if not _is_post_url(href):
            continue
        url = _normalize_post_url(href)
        posts.append(
            Post(
                key=key_from_url(url),
                title="",
                description="",
                tags=[],
                thumbnail="",
                publishedAt="",
                url=url,
                source="browser",
            )
        )
    return posts


def _enrich_posts(posts: list[Post]) -> list[Post]:
    with ThreadPoolExecutor(max_workers=8) as executor:
        return list(executor.map(_enrich_post, posts))


def _enrich_post(post: Post) -> Post:
    body = fetch_text(post.url)
    article = _blog_posting(extract_json_ld_objects(body))
    title = _first_text(article, "headline", "name") or extract_meta(body, "og:title")
    description = _first_text(article, "description") or extract_meta(body, "og:description") or extract_meta(body, "description")
    thumbnail = _image(article) or extract_meta(body, "og:image")
    published_at = (
        _first_text(article, "datePublished", "dateCreated")
        or extract_meta(body, "article:published_time")
        or extract_meta(body, "publishdate")
        or extract_meta(body, "date")
    )

    tags: list[str] = []
    article_section = normalize_tag(_first_text(article, "articleSection"))
    if article_section:
        tags.append(article_section)
    for tag in _keywords(article):
        if tag and tag not in tags:
            tags.append(tag)

    return Post(
        key=post.key,
        title=normalize_space(title),
        description=normalize_space(description),
        tags=tags,
        thumbnail=normalize_url(post.url, thumbnail) if thumbnail else "",
        publishedAt=normalize_published_at(published_at),
        url=post.url,
        source=post.source,
    )


def _blog_posting(objects: list[dict[str, object]]) -> dict[str, object]:
    for obj in objects:
        type_name = normalize_space(str(obj.get("@type", ""))).lower()
        if type_name in {"article", "blogposting", "newsarticle", "techarticle"}:
            return obj
    return {}


def _first_text(obj: dict[str, object], *keys: str) -> str:
    for key in keys:
        value = obj.get(key)
        if isinstance(value, str) and normalize_space(value):
            return normalize_space(value)
    return ""


def _image(obj: dict[str, object]) -> str:
    value = obj.get("image")
    if isinstance(value, str):
        return normalize_space(value)
    if isinstance(value, dict):
        url = value.get("url")
        if isinstance(url, str):
            return normalize_space(url)
    if isinstance(value, list):
        for item in value:
            if isinstance(item, str) and normalize_space(item):
                return normalize_space(item)
            if isinstance(item, dict):
                url = item.get("url")
                if isinstance(url, str) and normalize_space(url):
                    return normalize_space(url)
    return ""


def _keywords(obj: dict[str, object]) -> list[str]:
    value = obj.get("keywords")
    if isinstance(value, str):
        return [normalize_tag(part) for part in value.split(",") if normalize_tag(part)]
    if isinstance(value, list):
        return [normalize_tag(str(part)) for part in value if normalize_tag(str(part))]
    return []


def _is_post_url(url: str) -> bool:
    parsed = urllib.parse.urlsplit(url)
    if parsed.netloc != urllib.parse.urlsplit(BASE_URL).netloc:
        return False
    path = urllib.parse.unquote(parsed.path)
    return path.startswith("/post/") and path not in {"/post", "/post/"}


def _normalize_post_url(url: str) -> str:
    parsed = urllib.parse.urlsplit(url)
    return urllib.parse.urlunsplit((parsed.scheme, parsed.netloc, parsed.path.rstrip("/"), "", ""))


def _scroll_until_stable(scroll_wait: int):
    def action(page) -> None:
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
