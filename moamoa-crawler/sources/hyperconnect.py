from __future__ import annotations

import re
import urllib.error
import urllib.parse

from datetime import datetime

from _common import HtmlNode, Post, fetch_html, make_payload_raw as make_payload, normalize_tag


KEY = "hyperconnect"
BLOG = "Hyperconnect Tech Blog"
BASE_URL = "https://hyperconnect.github.io"
DATE_RE = re.compile(r"\d{4}[./-]\d{1,2}[./-]\d{1,2}|[A-Za-z]{3,}\s+\d{1,2},\s+\d{4}")


def crawl(request, config) -> dict[str, object]:
    del config
    page = 1
    seen_keys: set[str] = set()
    posts: list[Post] = []

    while request.size is None or len(posts) < request.size:
        try:
            doc = fetch_html(_build_list_url(page))
        except urllib.error.HTTPError as exc:
            if exc.code in {403, 404}:
                break
            raise
        link_els = doc.select("article a[href], .post-list a[href], .posts a[href], main a[href]")
        if not link_els:
            break

        new_posts = []
        for link_el in link_els:
            url = link_el.abs_url("href").strip()
            if not url or not url.startswith(BASE_URL) or _is_non_post_url(url):
                continue
            key = _extract_key_from_url(url)
            if not key or key in seen_keys:
                continue
            title = _require_field(url, "title", _extract_title(link_el))

            new_posts.append(
                Post(
                    key=key,
                    title=title,
                    description=_extract_description(link_el),
                    tags=_extract_tags(link_el),
                    thumbnail="",
                    publishedAt=_extract_published_at(link_el),
                    url=url,
                    source="html",
                )
            )
            seen_keys.add(key)

        if not new_posts:
            break
        posts.extend(new_posts)
        page += 1

    if request.size is not None:
        posts = posts[: request.size]
    if not posts:
        raise RuntimeError(f"{KEY} crawl finished but no post links were extracted from {_build_list_url(1)}")

    return make_payload(
        key=KEY,
        blog=BLOG,
        base_url=BASE_URL,
        requested_url=_build_list_url(1),
        crawler="html.urllib",
        requested_size=request.size,
        posts=posts,
    )


def _build_list_url(page: int) -> str:
    return BASE_URL if page == 1 else f"{BASE_URL}/page/{page}/"


def _extract_key_from_url(url: str) -> str:
    path = urllib.parse.urlsplit(url).path.strip()
    path = path.strip("/")
    if (
        not path
        or path.startswith("page/")
        or path.startswith("tag/")
        or path.startswith("tags/")
        or path.startswith("category/")
        or path.startswith("categories/")
        or path in {"about", "archive"}
        or path.endswith(".xml")
    ):
        return ""
    return path


def _is_non_post_url(url: str) -> bool:
    return not _extract_key_from_url(url)


def _container(link_el: HtmlNode) -> HtmlNode:
    current = link_el.element
    while current is not None:
        if current.tag.lower() in {"article", "li", "div", "section"}:
            return HtmlNode(current, link_el.base_url)
        current = current.getparent()
    return link_el


def _extract_title(link_el: HtmlNode) -> str:
    heading = link_el.select_first("h1, h2, h3")
    return (heading.text() if heading else "") or link_el.text()


def _extract_description(link_el: HtmlNode) -> str:
    container = _container(link_el)
    for candidate in container.select("p"):
        text = candidate.text()
        if candidate.select_first("a") is None and text:
            return text
    return ""


def _extract_tags(link_el: HtmlNode) -> list[str]:
    container = _container(link_el)
    tags = []
    for tag_el in container.select('a[rel="tag"], .tag, .tags a, span'):
        tag = normalize_tag(tag_el.text())
        if tag and tag not in tags:
            tags.append(tag)
    return tags


def _extract_published_at(link_el: HtmlNode) -> str:
    container = _container(link_el)
    time_el = container.select_first("time[datetime]")
    if time_el and time_el.attr("datetime"):
        return _parse_published_at(time_el.attr("datetime"))
    for candidate in container.select("time, .post-meta, .meta, span, div"):
        text = candidate.text()
        matched = DATE_RE.search(text)
        if matched:
            return _parse_published_at(matched.group(0))
    return ""


def _parse_published_at(raw: str) -> str:
    text = raw.strip()
    if not text:
        return ""
    for pattern in ("%Y-%m-%d", "%Y.%m.%d", "%Y/%m/%d", "%b %d, %Y"):
        try:
            return datetime.strptime(text, pattern).strftime("%Y-%m-%dT00:00:00")
        except ValueError:
            continue
    return ""


def _require_field(url: str, field: str, value: str) -> str:
    text = value.strip()
    if not text:
        raise RuntimeError(f"blogKey={KEY} url={url} field={field}")
    return text
