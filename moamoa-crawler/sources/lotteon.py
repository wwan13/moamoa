from __future__ import annotations

import re
import urllib.parse

from _common import card_browser_posts


KEY = "lotteon"
BASE_URL = "https://techblog.lotteon.com"


def is_post_url(url: str) -> bool:
    parsed = urllib.parse.urlsplit(url)
    if parsed.netloc != "techblog.lotteon.com":
        return False
    if parsed.path in {"", "/", "/all"}:
        return False
    if parsed.path.startswith(("/tagged/", "/m/", "/me/", "/search")):
        return False
    return re.search(r"-[0-9a-f]{12,}/?$", urllib.parse.unquote(parsed.path)) is not None


def crawl(request, config) -> dict[str, object]:
    return card_browser_posts(
        key=KEY,
        blog="LotteON Tech Blog",
        base_url=BASE_URL,
        request=request,
        config=config,
        is_post_url=is_post_url,
        no_posts_message="lotteon browser crawl finished but no post links were extracted from /all",
    )
