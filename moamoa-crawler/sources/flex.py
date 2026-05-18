from __future__ import annotations

import re

from datetime import datetime
from urllib.error import HTTPError
from urllib.parse import urlsplit

from _common import HtmlNode, Post, fetch_html, make_payload_raw as make_payload, normalize_url


KEY = "flex"
BLOG = "flex team"
BASE_URL = "https://flex.team/blog/category/flexteam/"
DATE_RE = re.compile(r"\d{4}\.\s*\d{1,2}\.\s*\d{1,2}")


def crawl(request, config) -> dict[str, object]:
    del config
    doc = _fetch_document(BASE_URL)
    post_links = []
    for link_el in doc.select('a[href^="/blog/"]'):
        href = link_el.attr("href")
        if (
            not href
            or href == "/blog/"
            or href.startswith("/blog/category/")
            or href.startswith("/blog/tag/")
            or href.startswith("/blog/search")
            or not link_el.text()
        ):
            continue
        post_links.append(link_el)

    if not post_links:
        raise RuntimeError(f"{KEY} crawl finished but no post links were extracted from {BASE_URL}")

    seen_keys: set[str] = set()
    posts: list[Post] = []
    for link_el in post_links:
        url = link_el.abs_url("href")
        key = _extract_key(url)
        if not url or not key or key in seen_keys:
            continue

        card = _closest_li(link_el) or _parent(link_el)
        if card is None:
            continue

        tags = []
        for tag_el in card.select('a[href^="/blog/category/"]'):
            tag = tag_el.text()
            if tag and tag not in tags:
                tags.append(tag)

        thumbnail = _extract_thumbnail(card)
        if not thumbnail:
            continue

        posts.append(
            Post(
                key=key,
                title=link_el.text(),
                description=_extract_description(card, link_el.text()),
                tags=tags,
                thumbnail=thumbnail,
                publishedAt=_extract_published_at(card.text()),
                url=url,
                source="html",
            )
        )
        seen_keys.add(key)
        if request.size and len(posts) >= request.size:
            break

    return make_payload(
        key=KEY,
        blog=BLOG,
        base_url=BASE_URL,
        requested_url=BASE_URL,
        crawler="html.urllib",
        requested_size=request.size,
        posts=posts,
    )


def _closest_li(node: HtmlNode) -> HtmlNode | None:
    current = node.element
    while current is not None:
        if current.tag.lower() == "li":
            return HtmlNode(current, node.base_url)
        current = current.getparent()
    return None


def _parent(node: HtmlNode) -> HtmlNode | None:
    parent = node.element.getparent()
    return HtmlNode(parent, node.base_url) if parent is not None else None


def _extract_thumbnail(card: HtmlNode) -> str:
    img = card.select_first("img[src]")
    if img is None:
        return ""
    return img.abs_url("src") or img.attr("src")


def _extract_description(card: HtmlNode, title: str) -> str:
    candidates = []
    for el in card.select("p, div"):
        text = el.text()
        if not text or text == title or DATE_RE.search(text) or len(text) <= 6:
            continue
        candidates.append(text)
    return candidates[0] if candidates else ""


def _extract_published_at(card_text: str) -> str:
    matched = DATE_RE.search(card_text)
    if not matched:
        return ""
    try:
        return datetime.strptime(re.sub(r"\s+", " ", matched.group(0)).strip(), "%Y. %m. %d").strftime("%Y-%m-%dT00:00:00")
    except ValueError:
        return ""


def _extract_key(url: str) -> str:
    return urlsplit(url).path.rstrip("/")


def _fetch_document(url: str):
    current_url = url
    for _ in range(5):
        try:
            return fetch_html(current_url)
        except HTTPError as error:
            if error.code != 308:
                raise
            location = error.headers.get("Location", "").strip()
            if not location:
                raise
            current_url = normalize_url(current_url, location)
    raise RuntimeError(f"{KEY} crawl finished but too many redirects for {url}")
