from __future__ import annotations

import re
import urllib.parse

from _common import card_browser_posts


KEY = "babitalk"
BASE_URL = "https://medium.com/babitalk-blog"


def is_post_url(url: str) -> bool:
    parsed = urllib.parse.urlsplit(url)
    if parsed.netloc != "medium.com":
        return False
    if parsed.path in {"", "/", "/babitalk-blog", "/babitalk-blog/", "/babitalk-blog/all"}:
        return False
    if not parsed.path.startswith("/babitalk-blog/"):
        return False
    if parsed.path.startswith(("/babitalk-blog/tagged/", "/m/", "/me/", "/search")):
        return False
    return re.search(r"-[0-9a-f]{12,}/?$", urllib.parse.unquote(parsed.path)) is not None


def crawl(request, config) -> dict[str, object]:
    return card_browser_posts(
        key=KEY,
        blog="Babitalk",
        base_url=BASE_URL,
        request=request,
        config=config,
        is_post_url=is_post_url,
        no_posts_message="babitalk browser crawl finished but no post links were extracted from /all",
    )
