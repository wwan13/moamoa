from __future__ import annotations

import re
import urllib.parse

from _common import card_browser_posts


KEY = "class101"
BASE_URL = "https://medium.com/class101"


def is_post_url(url: str) -> bool:
    parsed = urllib.parse.urlsplit(url)
    if parsed.netloc != "medium.com":
        return False
    if parsed.path in {"", "/", "/class101", "/class101/", "/class101/all"}:
        return False
    if not parsed.path.startswith("/class101/"):
        return False
    if parsed.path.startswith(("/class101/tagged/", "/m/", "/me/", "/search")):
        return False
    return re.search(r"-[0-9a-f]{12,}/?$", urllib.parse.unquote(parsed.path)) is not None


def crawl(request, config) -> dict[str, object]:
    return card_browser_posts(
        key=KEY,
        blog="CLASS101",
        base_url=BASE_URL,
        request=request,
        config=config,
        is_post_url=is_post_url,
        no_posts_message="class101 browser crawl finished but no post links were extracted from /all",
    )
