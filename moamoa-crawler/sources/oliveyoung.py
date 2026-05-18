from __future__ import annotations

import re
from urllib.error import HTTPError

from _common import HtmlNode, Post, fetch_html, make_payload_raw as make_payload


KEY = "oliveyoung"
BLOG = "Oliveyoung Tech"
BASE_URL = "https://oliveyoung.tech"
HREF_RE = re.compile(r"^/\d{4}-\d{2}-\d{2}/.+/?$")
DATE_RE = re.compile(r"^\d{4}-\d{2}-\d{2}$")


def crawl(request, config) -> dict[str, object]:
    del config
    page = 1
    seen_keys: set[str] = set()
    posts: list[Post] = []

    while request.size is None or len(posts) < request.size:
        try:
            doc = fetch_html(_build_list_url(page))
        except HTTPError as error:
            if error.code == 404 and page > 1:
                break
            raise
        link_els = [link for link in doc.select("a[href]") if HREF_RE.match(link.attr("href"))]
        if not link_els:
            break

        new_posts: list[Post] = []
        for link_el in link_els:
            href = link_el.attr("href").strip()
            key = href.split("?", 1)[0].split("#", 1)[0].strip("/")
            url = link_el.abs_url("href").strip()
            if not key or not url or key in seen_keys:
                continue

            root = _root_node(link_el)
            title_el = root.select_first("h1")
            title = title_el.text().strip() if title_el else ""
            if not title:
                continue

            spans = [span.text() for span in root.select("span") if span.text()]
            date_text = next((text for text in spans if DATE_RE.match(text)), "")
            tag = next((text for text in spans if text and not DATE_RE.match(text)), "")
            extra_tags = [text for text in spans if text and text != tag and text != date_text]
            categories = []
            for value in [tag, *extra_tags]:
                if value and value not in categories:
                    categories.append(value)

            thumbnail = _extract_thumbnail(root)
            if not thumbnail:
                continue

            description_el = root.select_first("p")
            description = description_el.text().strip() if description_el else ""
            published_at = f"{date_text}T00:00:00" if date_text else ""
            new_posts.append(
                Post(
                    key=key,
                    title=title,
                    description=description,
                    tags=categories,
                    thumbnail=thumbnail,
                    publishedAt=published_at,
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


def _root_node(link_el) -> HtmlNode:
    current = link_el.element
    while current is not None:
        if current.tag.lower() == "li":
            return HtmlNode(current, link_el.base_url)
        current = current.getparent()
    return link_el


def _extract_thumbnail(root: HtmlNode) -> str:
    main_img = root.select_first("img[data-main-image]")
    if main_img and main_img.abs_url("src"):
        return main_img.abs_url("src").strip()
    srcset_img = root.select_first("img[srcset]")
    if srcset_img and srcset_img.attr("srcset"):
        first_src = srcset_img.attr("srcset").split(",", 1)[0].strip().split(" ", 1)[0]
        return BASE_URL + first_src if first_src.startswith("/") else first_src
    return ""
