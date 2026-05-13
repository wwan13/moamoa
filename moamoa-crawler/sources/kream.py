from __future__ import annotations

import re
import urllib.parse

from _common import card_browser_posts


KEY = "kream"
BASE_URL = "https://medium.com/kream-%EA%B8%B0%EC%88%A0-%EB%B8%94%EB%A1%9C%EA%B7%B8"


def is_post_url(url: str) -> bool:
    parsed = urllib.parse.urlsplit(url)
    path = urllib.parse.unquote(parsed.path)
    if parsed.netloc != "medium.com":
        return False
    if path in {"", "/", "/kream-기술-블로그", "/kream-기술-블로그/", "/kream-기술-블로그/all"}:
        return False
    if not path.startswith("/kream-기술-블로그/"):
        return False
    if path.startswith(("/kream-기술-블로그/tagged/", "/m/", "/me/", "/search")):
        return False
    return re.search(r"-[0-9a-f]{12,}/?$", path) is not None


def crawl(request, config) -> dict[str, object]:
    return card_browser_posts(
        key=KEY,
        blog="KREAM Tech Blog",
        base_url=BASE_URL,
        request=request,
        config=config,
        is_post_url=is_post_url,
        no_posts_message="kream browser crawl finished but no post links were extracted from /all",
    )
