from __future__ import annotations

import re
import urllib.parse
from datetime import datetime
from zoneinfo import ZoneInfo

from _common import Post, make_payload_raw as make_payload, normalize_space, normalize_url, unique_posts


KEY = "kurly"
BLOG = "Kurly Tech Blog"
BASE_URL = "https://helloworld.kurly.com"
KOREAN_DATE_RE = re.compile(r"(\d{4})년\s*(\d{1,2})월\s*(\d{1,2})일")
SEOUL = ZoneInfo("Asia/Seoul")


def crawl(request, config) -> dict[str, object]:
    from scrapling.fetchers import StealthyFetcher

    page = 1
    first_page_signature: str | None = None
    posts: list[Post] = []

    while request.size is None or len(posts) < request.size:
        page_url = _build_list_url(page)
        browser_page = StealthyFetcher.fetch(
            page_url,
            headless=config.headless,
            network_idle=True,
            wait=config.wait,
            page_action=_scroll_page(config.scrolls, config.scroll_wait),
        )
        parsed = _parse_posts(browser_page)
        if not parsed:
            break

        signature = "|".join(post.url.strip() for post in parsed if post.url.strip())
        if not signature:
            break
        if page == 1:
            first_page_signature = signature
        elif first_page_signature == signature:
            break

        posts.extend(parsed)
        posts = unique_posts(posts, request.size)
        if request.size is not None and len(posts) >= request.size:
            break
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
        crawler="scrapling.StealthyFetcher",
        requested_size=request.size,
        posts=posts,
    )


def _parse_posts(page) -> list[Post]:
    current_cards = [card for card in page.css("article a[href]") if _normalized_blog_url(card)]
    legacy_cards = list(page.css("ul.post-list li.post-card"))
    if current_cards:
        return unique_posts([post for card in current_cards if (post := _parse_current_card(card)) is not None], 10_000)
    return unique_posts([post for card in legacy_cards if (post := _parse_legacy_card(card)) is not None], 10_000)


def _parse_current_card(card) -> Post | None:
    url = _normalized_blog_url(card)
    if not url:
        return None
    title = _text(_first(card.css("h2")))
    if not title:
        return None
    time_el = _first(card.css("time"))
    published_at = _parse_published_at(
        raw=_text(time_el),
        datetime_raw=_attr(time_el, "datetime").strip(),
    )
    if not published_at:
        return None
    category = _text(_first(card.css("span")))
    description = _text(_first(card.css("p")))
    thumbnail = _image_url(_first(card.css("img[src]")))
    return Post(
        key=url.rstrip("/").split("/")[-1],
        title=title,
        description=description,
        tags=[category] if category else [],
        thumbnail=thumbnail,
        publishedAt=published_at,
        url=url,
        source="browser",
    )


def _parse_legacy_card(card) -> Post | None:
    link = _first(card.css("a.post-link[href]"))
    title = _text(_first(card.css("h3.post-title")))
    raw_date = _text(_first(card.css("span.post-date")))
    if link is None or not title or not raw_date:
        return None
    url = _absolute_url(_attr(link, "href"))
    published_at = _parse_published_at(raw=raw_date.rstrip("."), datetime_raw="")
    if not url or not published_at:
        return None
    return Post(
        key=url.rstrip("/").split("/")[-1],
        title=title,
        description=_text(_first(card.css("p.title-summary"))),
        tags=[],
        thumbnail="",
        publishedAt=published_at,
        url=url,
        source="browser",
    )


def _build_list_url(page: int) -> str:
    return f"{BASE_URL}/" if page == 1 else f"{BASE_URL}/page/{page}/"


def _normalized_blog_url(card) -> str:
    url = _absolute_url(_attr(card, "href")).strip()
    parsed = urllib.parse.urlsplit(url)
    path = parsed.path.lstrip("/")
    return url if path.startswith("blog/") else ""


def _absolute_url(value: str) -> str:
    if not value:
        return ""
    return normalize_url(BASE_URL, value)


def _image_url(node) -> str:
    src = _attr(node, "src")
    return _absolute_url(src) if src else ""


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
            return normalize_space(str(value))
    return ""


def _scroll_page(scrolls: int, scroll_wait: int):
    def action(page) -> None:
        for _ in range(scrolls):
            page.mouse.wheel(0, 2400)
            page.wait_for_timeout(scroll_wait)

    return action


def _parse_published_at(*, raw: str, datetime_raw: str) -> str:
    matched = KOREAN_DATE_RE.search(raw)
    if matched:
        year, month, day = matched.groups()
        return f"{int(year):04d}-{int(month):02d}-{int(day):02d}T00:00:00"

    try:
        return datetime.strptime(raw.rstrip("."), "%Y.%m.%d").strftime("%Y-%m-%dT00:00:00")
    except ValueError:
        pass

    if datetime_raw:
        try:
            dt = datetime.fromisoformat(datetime_raw.replace("Z", "+00:00"))
            return dt.astimezone(SEOUL).date().isoformat() + "T00:00:00"
        except ValueError:
            return ""
    return ""
