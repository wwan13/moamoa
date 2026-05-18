from __future__ import annotations

import re
import subprocess
from datetime import datetime
from zoneinfo import ZoneInfo

from lxml import html as lxml_html

from _common import HtmlDocument, Post, make_payload_raw as make_payload


KEY = "kurly"
BLOG = "Kurly Tech Blog"
BASE_URL = "https://helloworld.kurly.com"
KOREAN_DATE_RE = re.compile(r"(\d{4})년\s*(\d{1,2})월\s*(\d{1,2})일")
SEOUL = ZoneInfo("Asia/Seoul")


def crawl(request, config) -> dict[str, object]:
    del config
    page = 1
    first_page_signature: str | None = None
    posts: list[Post] = []

    while request.size is None or len(posts) < request.size:
        doc = _fetch_html(_build_list_url(page))
        parsed = _parse_posts(doc)
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


def _parse_posts(doc) -> list[Post]:
    current_cards = [card for card in doc.select("article a[href]") if _normalized_blog_url(card)]
    legacy_cards = doc.select("ul.post-list li.post-card")
    if current_cards:
        return [post for card in current_cards if (post := _parse_current_card(card)) is not None]
    return [post for card in legacy_cards if (post := _parse_legacy_card(card)) is not None]


def _parse_current_card(card) -> Post | None:
    url = _normalized_blog_url(card)
    if not url:
        return None
    title_el = card.select_first("h2")
    if title_el is None or not title_el.text():
        return None
    time_el = card.select_first("time")
    published_at = _parse_published_at(
        raw=time_el.text().strip() if time_el else "",
        datetime_raw=time_el.attr("datetime").strip() if time_el else "",
    )
    if not published_at:
        return None
    category_el = card.select_first("span")
    img_el = card.select_first("img[src]")
    return Post(
        key=url.rstrip("/").split("/")[-1],
        title=title_el.text(),
        description=card.select_first("p").text() if card.select_first("p") else "",
        tags=[category_el.text()] if category_el and category_el.text() else [],
        thumbnail=img_el.abs_url("src") if img_el else "",
        publishedAt=published_at,
        url=url,
        source="html",
    )


def _parse_legacy_card(card) -> Post | None:
    link = card.select_first("a.post-link[href]")
    title_el = card.select_first("h3.post-title")
    date_el = card.select_first("span.post-date")
    if link is None or title_el is None or date_el is None:
        return None
    url = link.abs_url("href")
    published_at = _parse_published_at(raw=date_el.text().strip().rstrip("."), datetime_raw="")
    if not url or not title_el.text() or not published_at:
        return None
    return Post(
        key=url.rstrip("/").split("/")[-1],
        title=title_el.text(),
        description=card.select_first("p.title-summary").text() if card.select_first("p.title-summary") else "",
        tags=[],
        thumbnail="",
        publishedAt=published_at,
        url=url,
        source="html",
    )


def _build_list_url(page: int) -> str:
    return f"{BASE_URL}/" if page == 1 else f"{BASE_URL}/page/{page}/"


def _fetch_html(url: str):
    response = subprocess.run(
        ["curl", "-L", "-A", "Mozilla/5.0", url],
        check=True,
        capture_output=True,
    )
    return HtmlDocument(lxml_html.fromstring(response.stdout), url)


def _normalized_blog_url(card) -> str:
    url = card.abs_url("href").strip()
    path = url.split("://", 1)[-1].split("/", 1)[-1] if "://" in url else ""
    return url if path.startswith("blog/") else ""


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
