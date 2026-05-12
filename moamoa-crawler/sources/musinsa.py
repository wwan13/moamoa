from __future__ import annotations

import logging
import re
import urllib.parse
from dataclasses import asdict
from datetime import datetime, timezone
from pathlib import Path
from typing import Any

from scrapling.fetchers import StealthyFetcher

from crawler import BlogPost
from jobs import CrawlJobConfig, CrawlJobRequest


KEY = "musinsa"
BASE_URL = "https://techblog.musinsa.com"
ALL_URL = f"{BASE_URL}/all"


def normalize_url(url: str) -> str:
    absolute = urllib.parse.urljoin(BASE_URL, url)
    parsed = urllib.parse.urlsplit(absolute)
    return urllib.parse.urlunsplit((parsed.scheme, parsed.netloc, parsed.path, "", ""))


def is_post_url(url: str) -> bool:
    parsed = urllib.parse.urlsplit(url)
    if parsed.netloc != "techblog.musinsa.com":
        return False
    if parsed.path in {"", "/", "/all"}:
        return False
    if parsed.path.startswith(("/tagged/", "/m/", "/me/", "/search")):
        return False
    return re.search(r"-[0-9a-f]{12,}$", urllib.parse.unquote(parsed.path)) is not None


def text_of(selector: Any) -> str:
    value = getattr(selector, "text", "")
    if callable(value):
        value = value()
    if value:
        return re.sub(r"\s+", " ", str(value)).strip()
    return ""


def attr_of(selector: Any, name: str) -> str | None:
    if hasattr(selector, "attrib"):
        value = selector.attrib.get(name)
        if value:
            return str(value)
    if hasattr(selector, "get"):
        try:
            value = selector.get(name)
            if value:
                return str(value)
        except TypeError:
            return None
    return None


def extract_posts(page: Any) -> list[BlogPost]:
    posts: dict[str, BlogPost] = {}

    for anchor in page.css("a[href]"):
        href = attr_of(anchor, "href")
        if not href:
            continue

        url = normalize_url(href)
        if not is_post_url(url):
            continue

        title = text_of(anchor)
        if not title:
            title = urllib.parse.unquote(Path(urllib.parse.urlsplit(url).path).name)
            title = re.sub(r"-[0-9a-f]{12,}$", "", title).replace("-", " ").strip()

        if title:
            posts[url] = BlogPost(title=title, url=url, source="browser")

    return list(posts.values())


def scroll_page(scrolls: int, scroll_wait: int):
    def action(page: Any) -> None:
        for _ in range(scrolls):
            page.mouse.wheel(0, 2400)
            page.wait_for_timeout(scroll_wait)

    return action


def crawl(request: CrawlJobRequest, config: CrawlJobConfig) -> dict[str, object]:
    logging.info("fetching %s with Scrapling StealthyFetcher", ALL_URL)
    page = StealthyFetcher.fetch(
        ALL_URL,
        headless=config.headless,
        network_idle=True,
        wait=config.wait,
        page_action=scroll_page(scrolls=config.scrolls, scroll_wait=config.scroll_wait),
    )
    posts = extract_posts(page)
    if not posts:
        raise RuntimeError("browser crawl finished but no post links were extracted")

    posts = posts[: request.size]

    return {
        "key": KEY,
        "blog": "MUSINSA techblog",
        "baseUrl": BASE_URL,
        "requestedUrl": ALL_URL,
        "crawler": "scrapling.StealthyFetcher",
        "crawledAt": datetime.now(timezone.utc).isoformat(),
        "requestedSize": request.size,
        "scrolls": config.scrolls,
        "postCount": len(posts),
        "posts": [asdict(post) for post in posts],
    }
