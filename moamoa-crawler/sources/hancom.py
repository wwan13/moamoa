from __future__ import annotations

import re

from _common import Post, fetch_html, key_from_url, make_payload, normalize_published_at, unique_posts


KEY = "hancom"
BLOG = "한컴테크"
BASE_URL = "https://tech.hancom.com"
LIST_URL = f"{BASE_URL}/blog/"


def crawl(request, config) -> dict[str, object]:
    del config

    posts: list[Post] = []
    page = 1

    while True:
        list_url = _page_url(page)
        document = fetch_html(list_url)
        page_posts = _parse_list_page(document)
        if not page_posts:
            break

        posts.extend(page_posts)
        posts = unique_posts(posts, request.size)
        if request.size is not None and len(posts) >= request.size:
            break

        page += 1

    posts = unique_posts(posts, request.size)
    if not posts:
        raise RuntimeError(f"{KEY} crawl finished but no post links were extracted from {LIST_URL}")

    return make_payload(
        key=KEY,
        blog=BLOG,
        base_url=BASE_URL,
        requested_url=LIST_URL,
        crawler="html.urllib+lxml",
        requested_size=request.size,
        posts=posts,
    )


def _page_url(page: int) -> str:
    return LIST_URL if page <= 1 else f"{BASE_URL}/blog/{page}/"


def _parse_list_page(document) -> list[Post]:
    posts: list[Post] = []
    for card in document.select(".uc_post_list_box"):
        title_link = card.select_first(".uc_post_list_title a[href]")
        if title_link is None:
            continue

        url = title_link.abs_url("href")
        title = title_link.text()
        if not url or not title:
            continue

        posts.append(
            Post(
                key=key_from_url(url),
                title=title,
                description=_description(card),
                tags=[],
                thumbnail=_thumbnail(card),
                publishedAt=_published_at(card),
                url=url,
                source="html",
            )
        )

    return posts


def _description(card) -> str:
    content = card.select_first(".uc_post_content")
    if content is None:
        return ""
    return content.text()


def _thumbnail(card) -> str:
    image = card.select_first(".uc_post_list_image img")
    if image is None:
        return ""
    return image.abs_url("src")


def _published_at(card) -> str:
    metadata = [node.text() for node in card.select(".ue-meta-data .ue-grid-item-meta-data")]
    for value in metadata:
        published_at = normalize_published_at(value)
        if published_at:
            return published_at

    card_text = card.text()
    match = re.search(r"\d{4}년\s*\d{2}월\s*\d{2}일", card_text)
    if match:
        return normalize_published_at(match.group(0))
    return ""
