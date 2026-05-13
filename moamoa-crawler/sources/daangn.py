from __future__ import annotations

import re
import urllib.parse

from _common import card_browser_posts


KEY = "daangn"
BASE_URL = "https://medium.com/daangn"


def is_post_url(url: str) -> bool:
    parsed = urllib.parse.urlsplit(url)
    if parsed.netloc != "medium.com":
        return False
    if parsed.path in {"", "/", "/daangn", "/daangn/", "/daangn/all"}:
        return False
    if not parsed.path.startswith("/daangn/"):
        return False
    if parsed.path.startswith(("/daangn/tagged/", "/m/", "/me/", "/search")):
        return False
    return re.search(r"-[0-9a-f]{12,}/?$", urllib.parse.unquote(parsed.path)) is not None


def crawl(request, config) -> dict[str, object]:
    return card_browser_posts(
        key=KEY,
        blog="Daangn Tech Blog",
        base_url=BASE_URL,
        request=request,
        config=config,
        is_post_url=is_post_url,
        no_posts_message="daangn browser crawl finished but no post links were extracted from /all",
    )
