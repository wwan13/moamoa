from __future__ import annotations

from _common import medium_browser_posts


def crawl(request, config) -> dict[str, object]:
    return medium_browser_posts(
        key="lotteon",
        blog="LotteON Tech Blog",
        base_url="https://techblog.lotteon.com",
        request=request,
        config=config,
    )
