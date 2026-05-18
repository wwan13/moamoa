from __future__ import annotations

from datetime import datetime
from urllib.error import HTTPError
from urllib.parse import urlsplit

from _common import HtmlNode, Post, fetch_html, make_payload_raw as make_payload


KEY = "kakaostyle"
BLOG = "KakaoStyle DevBlog"
BASE_URL = "https://devblog.kakaostyle.com/ko"


def crawl(request, config) -> dict[str, object]:
    del config
    page = 1
    first_page_signature: str | None = None
    seen_keys: set[str] = set()
    posts: list[Post] = []

    while request.size is None or len(posts) < request.size:
        try:
            doc = fetch_html(_build_list_url(page))
        except HTTPError as error:
            if error.code in {403, 404} and page > 1:
                break
            raise
        heads = doc.select("div.posts-head")
        if not heads:
            break

        signature = "|".join(
            head.select_first('a[href^="/ko/"]').abs_url("href").strip()
            for head in heads
            if head.select_first('a[href^="/ko/"]')
        )
        if not signature:
            break
        if page == 1:
            first_page_signature = signature
        elif first_page_signature == signature:
            break

        new_posts = []
        for head in heads:
            title_link = head.select_first('a[href^="/ko/"]')
            if title_link is None:
                continue
            url = title_link.abs_url("href").strip()
            key = _extract_key(url)
            title = title_link.text().strip()
            if not url or not key or not title or key in seen_keys:
                continue

            parent = head.element.getparent()
            root = HtmlNode(parent, BASE_URL) if parent is not None else head
            body_link = root.select_first("div.card-body a.posts-content")
            description = body_link.text().strip() if body_link else ""
            date_el = head.select_first("span.posts-date")
            new_posts.append(
                Post(
                    key=key,
                    title=title,
                    description=description,
                    tags=[],
                    thumbnail="",
                    publishedAt=_parse_published_at(date_el.text() if date_el else ""),
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
        raise RuntimeError(f"{KEY} crawl finished but no posts were extracted from {_build_list_url(1)}")

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
    return f"{BASE_URL}/" if page == 1 else f"{BASE_URL}/page/{page}/"


def _extract_key(url: str) -> str:
    return urlsplit(url).path.rstrip("/").removeprefix("/ko/").strip()


def _parse_published_at(raw: str) -> str:
    text = raw.replace("|", "").strip()
    if not text:
        return ""
    for pattern in ("%d %b %Y", "%d %B %Y"):
        try:
            return datetime.strptime(text, pattern).strftime("%Y-%m-%dT00:00:00")
        except ValueError:
            continue
    return ""
