from __future__ import annotations

import re

from datetime import datetime
from urllib.parse import urlsplit

from _common import HtmlNode, Post, fetch_html, make_payload_raw as make_payload, normalize_tag


KEY = "rapportlabs"
BLOG = "Rapport Labs Blog"
BASE_URL = "https://blog.rapportlabs.kr"
DATE_RE = re.compile(r"(Jan|Feb|Mar|Apr|May|Jun|Jul|Aug|Sep|Oct|Nov|Dec)\s+\d{1,2},\s+\d{4}")


def crawl(request, config) -> dict[str, object]:
    del config
    seen_keys: set[str] = set()
    posts: list[Post] = []
    page = 1

    while request.size is None or len(posts) < request.size:
        doc = fetch_html(_build_list_url(page))
        links = doc.select("main a[href]")
        if not links:
            break

        new_posts = []
        for link_el in links:
            url = link_el.abs_url("href").strip()
            key = _extract_key(url)
            title_el = link_el.select_first("h1, h2, h3")
            title = title_el.text().strip() if title_el else ""
            if not url.startswith(BASE_URL) or not key or not title or key in seen_keys:
                continue

            root = _container(link_el)
            date_text = _extract_date_text(root)
            thumbnail = _extract_thumbnail(root)
            if not thumbnail:
                continue
            new_posts.append(
                Post(
                    key=key,
                    title=title,
                    description=_extract_description(root),
                    tags=_extract_tags(root, date_text),
                    thumbnail=thumbnail,
                    publishedAt=_parse_published_at(date_text),
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
        raise RuntimeError(f"{KEY} crawl finished but no links were extracted from {_build_list_url(1)}")

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
    return BASE_URL if page == 1 else f"{BASE_URL}?page={page}"


def _extract_key(url: str) -> str:
    path = urlsplit(url).path.strip("/")
    if not path or path.startswith("category/") or path.startswith("tag/"):
        return ""
    return path


def _container(link_el) -> HtmlNode:
    current = link_el.element
    while current is not None:
        if current.tag.lower() in {"article", "li", "div", "section"}:
            return HtmlNode(current, link_el.base_url)
        current = current.getparent()
    return link_el


def _extract_description(root: HtmlNode) -> str:
    for candidate in root.select('div[class*="line-clamp"], p'):
        if candidate.select_first("span") is None and candidate.text():
            return candidate.text().strip()
    return ""


def _extract_date_text(root: HtmlNode) -> str:
    time_el = root.select_first("time[datetime]")
    if time_el and time_el.attr("datetime"):
        return time_el.attr("datetime")
    for candidate in root.select("span, div"):
        matched = DATE_RE.search(candidate.text())
        if matched:
            return matched.group(0)
    return ""


def _extract_tags(root: HtmlNode, date_text: str) -> list[str]:
    candidates = [node.text() for node in root.select('span[class*="rounded-full"]')]
    if not candidates:
        candidates = [node.text() for node in root.select("span")]
    tags = []
    for candidate in candidates:
        text = candidate.strip()
        if not text or text == date_text or DATE_RE.match(text) or (len(text) <= 2 and text.isalpha() and text.islower()):
            continue
        tag = normalize_tag(text)
        if tag and tag not in tags:
            tags.append(tag)
    return tags


def _extract_thumbnail(root: HtmlNode) -> str:
    img = root.select_first("img[src], img[srcset]")
    if img is None:
        return ""
    if img.attr("src"):
        return _resolve_image_url(img.attr("src").strip())
    if img.attr("srcset"):
        first_src = img.attr("srcset").split(",", 1)[0].strip().split(" ", 1)[0]
        return _resolve_image_url(first_src)
    return ""


def _resolve_image_url(raw: str) -> str:
    if raw.startswith("http"):
        return raw
    if raw.startswith("//"):
        return "https:" + raw
    if raw.startswith("/"):
        return BASE_URL + raw
    return raw


def _parse_published_at(raw: str) -> str:
    text = raw.strip().replace("Z", "+00:00")
    if not text:
        return ""
    try:
        return datetime.fromisoformat(text).replace(microsecond=0).isoformat(timespec="seconds")
    except ValueError:
        pass
    for pattern in ("%b %d, %Y", "%b %d, %Y"):
        try:
            return datetime.strptime(text, pattern).strftime("%Y-%m-%dT00:00:00")
        except ValueError:
            continue
    return ""
