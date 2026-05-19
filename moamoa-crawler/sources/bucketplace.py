from __future__ import annotations

import re
from urllib.error import HTTPError

from _common import Post, fetch_html, key_from_url, make_payload, normalize_tag


KEY = "bucketplace"
BLOG = "오늘의집 이야기"
BASE_URL = "https://www.bucketplace.com"
LIST_URL = f"{BASE_URL}/culture/"
POST_PATH_PREFIX = "/post/"
DATE_PREFIX_RE = re.compile(r"^(\d{4})-(\d{2})-(\d{2})-")


def crawl(request, config) -> dict[str, object]:
    del config

    page = 1
    seen_urls: set[str] = set()
    posts: list[Post] = []

    while request.size is None or len(posts) < request.size:
        list_url = _build_list_url(page)
        try:
            doc = fetch_html(list_url)
        except HTTPError as error:
            if error.code == 404 and page > 1:
                break
            raise

        page_posts = _extract_page_posts(doc, seen_urls)
        if not page_posts:
            break

        posts.extend(page_posts)
        if request.size is not None:
            posts = posts[: request.size]
        page += 1

    if not posts:
        raise RuntimeError(f"{KEY} crawl finished but no post links were extracted from {LIST_URL}")

    return make_payload(
        key=KEY,
        blog=BLOG,
        base_url=BASE_URL,
        requested_url=LIST_URL,
        crawler="html.urllib",
        requested_size=request.size,
        posts=posts,
    )


def _build_list_url(page: int) -> str:
    return LIST_URL if page == 1 else f"{LIST_URL}{page}/"


def _extract_page_posts(doc, seen_urls: set[str]) -> list[Post]:
    posts: list[Post] = []
    for item in doc.select("a.blog-page__post-list__item[href]"):
        href = item.attr("href").strip()
        url = item.abs_url("href").strip()
        if not _is_post_href(href) or not url or url in seen_urls:
            continue

        title_el = item.select_first(".blog-page__post-list__item__description__title")
        title = title_el.text().strip() if title_el else ""
        if not title:
            continue

        description_el = item.select_first(".blog-page__post-list__item__description__description")
        image_el = item.select_first("img.blog-page__post-list__item__image")
        thumbnail = image_el.abs_url("src").strip() if image_el else ""
        tags = _extract_tags(item)

        seen_urls.add(url)
        posts.append(
            Post(
                key=key_from_url(url),
                title=title,
                description=description_el.text().strip() if description_el else "",
                tags=tags,
                thumbnail=thumbnail,
                publishedAt=_published_at_from_href(href),
                url=url,
                source="html",
            )
        )

    return posts


def _is_post_href(href: str) -> bool:
    return href.startswith(POST_PATH_PREFIX)


def _extract_tags(item) -> list[str]:
    tags: list[str] = []
    for tag_el in item.select(".blog-page__post-list__item__tags__item a"):
        tag = normalize_tag(tag_el.text())
        if tag and tag not in tags:
            tags.append(tag)
    return tags


def _published_at_from_href(href: str) -> str:
    slug = href.removeprefix(POST_PATH_PREFIX).strip("/")
    match = DATE_PREFIX_RE.match(slug)
    if not match:
        return ""
    return f"{match.group(1)}-{match.group(2)}-{match.group(3)}T00:00:00"
